# ADR-0001: Cache Key Taxonomy & Invalidation Conventions

**Status:** Accepted
**Date:** 2026-06-16
**Deciders:** Valentin Vergilov
**Tickets:** [Epic 1 — Caching Architecture](https://app.clickup.com/t/869dpdevg) · [PERF-1.7](https://app.clickup.com/t/869dpdeza)

## Context

BabaAI caches in two layers:

- **L1 — Caffeine (in-process) in `babaai-core`** for hot read paths (recipe catalog, parsed JSON config, public config).
- **L3 — React Query in `babaai-frontend`** for stale-while-revalidate on the client.

(L2 — a shared Redis tier — is **deferred**; see [Cross-instance invalidation](https://app.clickup.com/t/869dq83x8). It is only needed once `core` runs as multiple replicas.)

Without a written convention, multiple cache domains (global catalog, semi-static config, per-user, per-recipe) risk key collisions, missed invalidations, and stale-data debugging. This ADR records the naming conventions, TTLs, and the invalidation matrix **as implemented**, and marks what is reserved/future so the two layers stay consistent as the system grows.

A foundational constraint shapes everything below: **all recipe & ingredient writes go through `babaai-core`'s service methods** (crawler → admin approval → save). Nothing writes to Postgres out-of-band. This is what makes in-process, single-instance eviction sufficient today.

## Decision

### 1. Backend (Caffeine) — `(cacheName, key)` tuples, not flat strings

Spring Cache addresses entries by a **cache name + a key**, so we do **not** use a flat `{domain}:{scope}:{id}` string. The *cache name* is the domain; the *key* is the method arguments. Cache names are centralised in `com.babaai.core.config.CacheNames` and referenced from `@Cacheable` — never inline string literals.

| Cache name (`CacheNames`) | Key | Holds | TTL / size | Status |
|---|---|---|---|---|
| `recipeCatalog` | `SimpleKey.EMPTY` (no-arg `snapshot()`) | One immutable full-catalog `List<Recipe>` snapshot | 10m / size 8* | **Implemented** (PERF-1.3) |
| `appConfig` | resource location `String` (JSON) **and** `SimpleKey.EMPTY` (`publicConfig()`) | Parsed JSON config maps + `PublicConfigResponse` | 30m / size 50 | **Implemented** (PERF-1.4) |
| `recipeCategories` | — | Recipe category list | 1h / size 200 | **Reserved** — name defined, not yet wired (`listCategories()` uncached) |
| *(default spec)* | per-method args | any unlisted cache name | 10m / size 1000 | Fallback in `CacheConfig` |

\* `recipeCatalog` is a **single full-catalog entry**; `maximum-size` is headroom only — heap scales with catalog size, not the entry count. Kill switch: `babaai.cache.enabled=false` → `NoOpCacheManager`.

**Conventions:**
- New cache → add a constant to `CacheNames`, a spec under `babaai.cache.specs.*`, and reference the constant from `@Cacheable`.
- Cached values must be **immutable / per-request-safe**. Never cache an entity that downstream code mutates per request (the catalog removed `Recipe`'s transient `favoriteCount`/`favorite`; favorite data is applied at DTO-mapping time instead).
- The cached method must be a **separate bean** from its caller (Spring AOP does not advise self-invocation) — see `RecipeCatalogCache`, `JsonConfigCache`.

### 2. Frontend (React Query) — hierarchical array keys

Keys are arrays built by the `queryKeys` factory (`src/lib/queryKeys.ts`), shaped `[domain, scope, ...params]` so independent components dedupe and so prefixes can be invalidated. Client defaults: `staleTime 60s`, `gcTime 5m`, `refetchOnWindowFocus`, `retry 1`.

| Key | TTL (staleTime) | Notes |
|---|---|---|
| `['config','public']` | 30m | semi-static |
| `['config','inferCategory', name]` | 5m | debounced lookup, cached per name |
| `['recipes','categories']` | 5m | **shared** by HomePage + RecipesPage (dedupe) |
| `['recipes','favorites']` | 60s (default) | `enabled` only when authenticated |
| `['recipes','list', params]` | 60s | `keepPreviousData` for smooth pagination |
| `['recipes','featured', scope]` | 60s | `scope` = `auth`/`guest` |
| `['recipes','daily', limit]` | 60s | `enabled` on token |
| `['recipes','history', params]` | 60s | |
| `['ingredients','list', params]` | 60s | |

### 3. Invalidation: evict-after-commit, not versioned keys

We invalidate by **eviction**, keyed off the mutating write, deferred to **after the DB transaction commits** (so a concurrent read can't repopulate the cache with pre-commit data). We did **not** adopt versioned keys (`catalog:v{N}`) — eviction is simpler for the single-instance case and versioned keys buy little until cross-instance is in play.

**Invalidation matrix (entity mutation → what to evict):**

| Mutation | Layer | Action | Status |
|---|---|---|---|
| Recipe created (`ingest()`) | backend | evict `recipeCatalog` (after commit) | **Implemented** |
| Recipe approve / update / delete / verify | backend | evict `recipeCatalog` (after commit) | **Future** (admin endpoints not built) |
| Recipe **view** (`recordView`) | backend | **no evict** — view-count staleness absorbed by 10m TTL (views too frequent to evict on) | **Implemented** |
| Category created / edited | backend | evict `appConfig` (+ `recipeCategories` once wired) | **Future** (no category CRUD yet) |
| JSON config files | backend | none — static resources; app restart / TTL only | **Implemented** |
| Favorite toggle | frontend | invalidate `['recipes','favorites']`; `featured`/`daily`/`list` patched optimistically in place, corrected on next refetch | **Implemented** |
| Ingredient create / update / delete | frontend | refetch `['ingredients','list', …]` via mutation `onSuccess` | **Implemented** |
| Suggestion generate | frontend | none (mutation, not cached) | **Implemented** |

## Options Considered

### Option A: `(cacheName, key)` tuples + eviction (chosen)
| Dimension | Assessment |
|-----------|------------|
| Complexity | Low — idiomatic Spring Cache + React Query |
| Cost | Zero infra (in-process) |
| Scalability | Single-instance only; needs L2/broadcast to scale horizontally |
| Team familiarity | High — standard framework patterns |

**Pros:** minimal code, type-safe constants/factory, no key-string parsing, immediate single-instance correctness via after-commit eviction.
**Cons:** local-only eviction (multi-replica replicas stay stale until TTL); no global "flush a domain by version".

### Option B: Flat versioned string keys (`{domain}:{scope}:{id}:v{N}`)
| Dimension | Assessment |
|-----------|------------|
| Complexity | Medium — bespoke key format + version bumping |
| Cost | Zero infra now, but pairs naturally with Redis later |
| Scalability | Cross-instance friendly (bump a version, all readers miss) |
| Team familiarity | Lower — custom convention, review gate needed |

**Pros:** invalidate a whole domain by bumping one version; maps cleanly onto a future shared store.
**Cons:** over-engineered for current single-instance scale; doesn't fit Spring's `(name, key)` model without a custom KeyGenerator; risk of stale version counters.

## Trade-off Analysis

At today's scale (single `core` instance, API-mediated writes) Option A is correct and cheapest: after-commit eviction gives immediate consistency with zero new infrastructure. Option B's main advantage — cheap cross-instance invalidation — only matters once we run multiple replicas, at which point a **broadcast** (Redis pub/sub, or Postgres `LISTEN/NOTIFY`) layered on Option A achieves the same result without rewriting keys. We therefore keep Option A and treat versioning as a possibility to revisit alongside L2.

## Consequences

**Easier:** consistent, type-safe cache references; clear "what evicts what"; new caches follow a recipe (constant + spec + separate bean); frontend dedupe/SWR for free.

**Harder / to watch:**
- **Multi-replica correctness** — local Caffeine eviction does not propagate. Before scaling `core`, implement the broadcast in [869dq83x8](https://app.clickup.com/t/869dq83x8).
- **New admin write paths must evict** — every catalog-mutating endpoint (approve/update/delete/verify) must call `recipeCatalogCache.evictAfterCommit()`. It is the *approval* that makes a recipe visible, so that is the write that must evict.
- **Out-of-band writes** would bypass eviction. This is safe only while the all-writes-through-core constraint holds; revisit if a crawler/job ever writes Postgres directly (then DB-trigger `NOTIFY` is required).

**Superseded:** PERF-1.5 (cache the JWT user lookup, `user:{id}`) is **not** implemented — the auth overhaul ([869dq4a12](https://app.clickup.com/t/869dq4a12)) makes the per-request user load unnecessary by carrying roles/permissions in the token, so there is nothing to cache.

## Action Items
1. [x] Centralise backend cache names in `CacheNames`; frontend keys in `queryKeys`.
2. [ ] When admin recipe endpoints land, add `evictAfterCommit()` to approve/update/delete/verify.
3. [ ] Wire `recipeCategories` (cache `listCategories()`) or drop the reserved constant.
4. [ ] Add a cache-key compliance check to the PR review checklist (use `CacheNames`/`queryKeys`, never inline keys; cached values immutable; cross-bean `@Cacheable`).
5. [ ] Cross-instance broadcast (Redis pub/sub or `LISTEN/NOTIFY`) before scaling `core` to >1 replica — [869dq83x8](https://app.clickup.com/t/869dq83x8).

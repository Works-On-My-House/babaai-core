# BabaAI Security

How authentication and authorization work across the BabaAI stack. **babaai-core is the security
authority** — it issues tokens and owns the user/permission data; every other service trusts what
core signs.

> Status legend: ✅ implemented · 🟡 scaffolded but inert · ⛔ planned / not built.

---

## 1. Trust boundaries

```
browser ──┐
          ▼
        gateway ──► core   (auth authority: issues + validates tokens, owns users/permissions)
          │
          └──────► ai      (intelligence only)
```

- **core** — single source of truth for identity, roles, permissions; the only JWT **issuer**.
- **gateway** — public entry; today a reverse proxy (no token validation of its own ⛔).
- **ai** — internal compute. Trusts core via a shared service token; for direct user calls it
  validates the JWT statelessly (planned, see §8).

**The RSA private signing key lives only in core** (`JWT_KEY_PATH`). Everyone else verifies with the
public key published at `/.well-known/jwks.json`. Never copy the private key anywhere.

---

## 2. Tokens

### Access token ✅
- **RS256 JWT**, short-lived (**`JWT_EXPIRE_MINUTES`, default 15**), stateless.
- Claims: `sub` (user id), `permissions` (effective permission keys — see §4), `pv`
  (permissions_version — drives instant revocation, see §8).
- Sent as `Authorization: Bearer <token>`. Verified by signature; no DB lookup needed by consumers
  that only need the claims.

### Refresh token ✅
- **Opaque** 256-bit random value. The DB stores **only its SHA-256 hash** (`refresh_tokens`), never
  the raw token.
- Lifetime **`REFRESH_EXPIRE_DAYS`, default 30**; delivered as an **httpOnly cookie** (Secure /
  SameSite / path configurable — see §9).
- **Rotating + one-time-use:** every `/auth/refresh` issues a new token in the same *family* and
  marks the old one used.
- **Reuse detection:** presenting an already-used/revoked token (token theft signal) **revokes the
  whole family** — all of that login's tokens die, forcing re-login.

### Endpoints
| Endpoint | Auth | Notes |
|---|---|---|
| `POST /api/v1/auth/register` | public | creates a user (no role yet — see §4) |
| `POST /api/v1/auth/login` | public | returns access token (body) + refresh cookie |
| `POST /api/v1/auth/refresh` | refresh **cookie** | rotates; returns a new access token + cookie |
| `POST /api/v1/auth/logout` | refresh **cookie** | revokes the family + clears the cookie |
| `GET  /api/v1/auth/me` | access token | current user |

`refresh`/`logout` are public to the access-token filter — they authenticate via the cookie.

### Login / refresh flow
```
login:    browser → core: username/password
          core → browser: { access_token }  +  Set-Cookie: refresh_token (httpOnly)
use:      browser → gateway → core/ai:  Authorization: Bearer <access>
expiry:   access token 401 → browser → core POST /auth/refresh (cookie)
          core → browser: new access_token + rotated cookie → retry original request
```
The frontend does the refresh-on-401 + retry transparently (single-flight axios interceptor).

---

## 3. Authentication enforcement (core)

`JwtAuthenticationFilter` validates the access token's signature, loads the user (with roles +
permissions), and sets the security context with the user's **effective permission authorities**.

URL rules (`SecurityConfig`): a single `PUBLIC_ENDPOINTS` list is `permitAll`; everything else is
`.anyRequest().authenticated()`. `/api/v1/internal/**` requires the service authority (§7).

### Status codes ✅
| Situation | Status | Body |
|---|---|---|
| No / invalid / expired access token on a protected endpoint | **401** | `{"detail":"Authentication required"}` |
| Authenticated but lacks the required permission | **403** | `{"detail":"You do not have permission…"}` |
| Authenticated, endpoint has no `@HasPermission` | 200 | any authenticated user |

(A custom `authenticationEntryPoint` is required for the 401 — Spring otherwise defaults to 403.)

---

## 4. Authorization — permissions, not roles

**We authorize on permissions, never roles.** Roles are just *bundles* of permissions. An endpoint
checks for a permission (e.g. `RECIPE_VERIFY`), never `hasRole('ADMIN')`. A user's authorities are
**permission keys only** — the role name is never an authority, so `hasRole(...)` matches nothing.

### Method security ✅
```java
@HasPermission(AppPermission.RECIPE_VERIFY)   // meta-annotation over @PreAuthorize("hasAuthority('{value}')")
public void verifyRecipe(...) { ... }
```
Enabled by `@EnableMethodSecurity` + an `AnnotationTemplateExpressionDefaults` bean. Use the
`AppPermission` enum constants so a typo is a compile error. **An endpoint without `@HasPermission`
is reachable by any authenticated user** — admin/sensitive endpoints MUST carry one.

### The model
- `AppPermission` / `AppRole` **enums are the single source of truth** (code-owned). `PermissionSyncService`
  reconciles the `permissions` / `roles` / `role_permissions` tables to match the enums on startup
  (hard-prune — DB mirrors code). A `grantsAll` role (e.g. SUPER_ADMIN) gets every permission
  dynamically.
- **Effective permissions** (`PermissionResolver`): `⋃ role permissions ∪ per-user GRANT overrides
  \ per-user DENY overrides`. **DENY wins.**
- **Per-user overrides** (`user_permission_overrides`) let an admin add/strip a single permission for
  one user without touching the role. The startup reconciler owns *role definitions* only — it never
  touches `user_roles` or `user_permission_overrides`, so per-user grants/demotions survive restarts.

### 🟡 Current state
`AppPermission` and `AppRole` are intentionally **empty** right now — the machinery is in place but no
concrete permissions/roles are defined, so no endpoint is permission-gated yet. Define them when the
product's roles/permissions are decided; the reconciler + `@HasPermission` then "light up".

---

## 5. Account security ✅
- **Failed-login lockout:** 5 consecutive wrong passwords → account locked **15 min** (a correct
  password is still rejected while locked); counter resets on success. (`login()` uses
  `noRollbackFor=AppException` so the attempt/lock state commits even though the failure throws.)
- **Presence:** `last_seen_at` is stamped on authenticated requests, throttled to ≤1 write/60s.
  Derive "online" from it; it is never a stuck boolean.

Passwords are hashed with **BCrypt**.

---

## 6. User account flags (`users`)
`enabled` (login/verification gate), `account_non_locked` + `locked_until` (lockout),
`failed_login_attempts`, `last_seen_at` (presence), `permissions_version` (token freshness),
`two_factor_enabled` / `two_factor_secret` (⛔ 2FA, columns only).

---

## 7. Service-to-service (core ↔ ai) ✅
Both directions use the shared-secret header **`X-Service-Token`** (derived from `AI_SERVICE_TOKEN`):
core→ai (recipe proposals, reindex) and ai→core (recipe ingest, `/api/v1/internal/**`, where it maps
to the `ROLE_SERVICE` authority via `ServiceTokenFilter`). A static secret keeps these internal hops
free of any JWKS/clock dependency.

The **verifier stores only a SHA-256 hash** of the secret, not the plaintext, and checks the incoming
header by hashing it + constant-time compare (same approach as the refresh-token store §2) — so a
leak of ai's config doesn't expose a usable token. ai holds `AI_SERVICE_TOKEN_HASH`; the sender still
holds the raw secret (it must, to send it), and TLS protects it in transit. Keep the secret strong and
rotated; ai must not be publicly reachable except via the gateway.

> The inbound ai→core verifier (`ServiceTokenFilter`) still compares the raw value — it can be moved
> to the same hash-at-rest scheme for symmetry.

> Note: the **gateway** revocation push (§8) uses a *signed* service token instead, because the
> gateway already loads core's JWKS for edge token checks (so verification is free there) — whereas
> on the core↔ai hops a shared secret is simpler and adds no runtime dependency.

---

## 8. Instant permission revocation (gateway) ✅ (ticket 869dqmbfp)
An issued access token carries the `permissions` baked in at issuance, so a revoke/role change
would normally only take effect on the next token (≤ `JWT_EXPIRE_MINUTES`). To make it **near-instant
without Redis or a broker**:

- **`pv` (permissions_version)** on `users` is minted into every access token. Bumping it
  invalidates all of a user's outstanding tokens; the *refreshed* token carries the new `pv`, so it
  is the discriminator the gateway uses (reject `pv < N`, admit `pv = N`).
- **Trigger (core):** `PermissionVersionService.bumpAndNotify(userId)` increments `pv` in-tx and,
  **after commit** (mirrors `RecipeCatalogCache.evictAfterCommit`), fire-and-forget POSTs
  `{user_id, pv}` to the gateway. Exposed at `POST /api/v1/internal/auth/users/{id}/revoke` — the
  seam the future admin UI (869dqjtpc) calls.
- **Service auth (core→gateway):** no shared secret on the wire. Core signs a **short-lived (60s)
  RS256 service token** (`JwtService.createServiceToken`, `svc` claim) with its private key and sends
  it in `X-Service-Auth`; the gateway verifies it via core's **JWKS** (signature + expiry + `svc`).
  A captured token is signature-bound and expires in 60s. (The static `X-Service-Token` shared-secret
  path in §7 is unchanged for core↔ai.)
- **Enforce (gateway):** a Caffeine denylist `userId → minAcceptablePv` with
  `expireAfterWrite = ACCESS_TOKEN_TTL`. A reactive `GlobalFilter` verifies the JWT signature (core
  JWKS) and rejects stale tokens with **`401` + header `X-Auth-Error: token_stale`**. Entries
  self-expire once no token with the old `pv` can exist; absent/undecodable tokens pass through
  (downstream still authorizes).
- **Client (FE):** the axios 401 interceptor silently `/auth/refresh`es and retries (§2 flow) — the
  retry carries the new claims, or `403`s if the permission is genuinely gone.
- **Resilience:** if the push fails or the gateway restarts, the system falls back to the
  `ACCESS_TOKEN_TTL` bound — never worse than TTL-only. **Caveat:** the in-memory denylist is
  per-instance; with multiple gateway replicas, core must fan out (or revisit a shared channel).

---

## 9. ai service authorization ⛔ (planned — ticket 869dqh96c)
For direct, streaming user calls (`browser → gateway → ai`, no core hop), **ai validates the JWT
statelessly** (signature via JWKS + read the `permissions` claim) — core already baked the
entitlement into the token, so ai does a crypto verify + a claim read, no DB, no core call.
- Any authenticated user may call AI; the **claim selects the tier** (premium model vs free Ollama).
- **Quota** (e.g. tokens/day) enforced at ai via an in-process daily counter (stub; a shared store
  can replace it if/when premium quotas need to span instances).
- ai keeps the static `X-Service-Token` path for its core-internal calls (§7).

---

## 10. Configuration (env)
| Var | Meaning |
|---|---|
| `JWT_KEY_PATH` | RSA keypair dir (private key — core only) |
| `JWT_EXPIRE_MINUTES` | access token lifetime (default 15) |
| `REFRESH_EXPIRE_DAYS` | refresh token lifetime (default 30) |
| `REFRESH_COOKIE_SECURE` | `false` for local http dev (a Secure cookie isn't sent over http) |
| `REFRESH_COOKIE_SAME_SITE` / `_NAME` / `_PATH` | refresh cookie attributes |
| `AI_SERVICE_TOKEN` | shared core↔ai secret (must match in both) |
| `GATEWAY_INTERNAL_URL` | gateway base URL core pushes revocation events to (§8; auth is a signed service token, no shared secret) |
| `CORS_ORIGINS` | allowed browser origins (credentials are allowed) |

The frontend must send `withCredentials` so the httpOnly refresh cookie flows cross-origin.
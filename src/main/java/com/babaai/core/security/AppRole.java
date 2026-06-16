package com.babaai.core.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * Single source of truth for roles and their permission bundles. On startup
 * {@code PermissionSyncService} reconciles the {@code roles} + {@code role_permissions} tables to
 * match this enum.
 *
 * <p>No roles are defined yet. Add them like:
 * <pre>{@code
 *   SUPER_ADMIN("Full access", true,  EnumSet.noneOf(AppPermission.class)), // grantsAll -> dynamic
 *   USER       ("End user",    false, EnumSet.of(AppPermission.RECIPE_VIEW));
 * }</pre>
 * A {@code grantsAll} role's bundle is computed dynamically from {@link AppPermission#values()}.
 */
public enum AppRole {

    // Define roles here.
    ;

    private final String description;
    private final boolean grantsAll;
    private final Set<AppPermission> explicit;

    AppRole(String description, boolean grantsAll, Set<AppPermission> explicit) {
        this.description = description;
        this.grantsAll = grantsAll;
        this.explicit = explicit;
    }

    public String description() {
        return description;
    }

    /** Effective bundle: all permissions for {@code grantsAll} roles (dynamic), else the explicit set. */
    public Set<AppPermission> permissions() {
        return grantsAll ? EnumSet.allOf(AppPermission.class) : EnumSet.copyOf(explicit);
    }
}

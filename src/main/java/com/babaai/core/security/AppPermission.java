package com.babaai.core.security;

/**
 * THE single source of truth for permissions. Add or remove a constant here ONLY — on startup
 * {@code PermissionSyncService} reconciles the {@code permissions} table to match this enum.
 * The enum {@link #name()} is the authority/permission key; reference these from {@link HasPermission}.
 *
 * <p>No permissions are defined yet. Add them like:
 * <pre>{@code  RECIPE_VIEW("Browse recipes and recipe details"); }</pre>
 */
public enum AppPermission {

    // Define permissions here.
    ;

    private final String description;

    AppPermission(String description) {
        this.description = description;
    }

    public String key() {
        return name();
    }

    public String description() {
        return description;
    }
}

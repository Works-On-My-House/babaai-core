package com.babaai.core.domain;

/**
 * Per-user override of a single permission, layered on top of role-derived permissions.
 * DENY wins over GRANT and over role-granted permissions (see PermissionResolver).
 */
public enum PermissionEffect {
    GRANT,
    DENY
}

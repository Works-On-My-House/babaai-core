package com.babaai.core.security;

import com.babaai.core.domain.Permission;
import com.babaai.core.domain.PermissionEffect;
import com.babaai.core.domain.Role;
import com.babaai.core.domain.User;
import com.babaai.core.domain.UserPermissionOverride;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Computes a user's effective permission keys:
 * <pre>
 *   effective = (union of role permissions) ∪ GRANT overrides \ DENY overrides
 * </pre>
 * DENY always wins. Operates on an already-loaded graph (roles, role.permissions, overrides,
 * override.permission) — callers must fetch those eagerly (see
 * {@code UserRepository.findWithRolesAndPermissionsById}).
 */
@Component
public class PermissionResolver {

    public Set<String> effectivePermissions(User user) {
        Set<String> granted = new HashSet<>();
        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                granted.add(permission.getKey());
            }
        }
        Set<String> denied = new HashSet<>();
        for (UserPermissionOverride override : user.getPermissionOverrides()) {
            String key = override.getPermission().getKey();
            if (override.getEffect() == PermissionEffect.GRANT) {
                granted.add(key);
            } else if (override.getEffect() == PermissionEffect.DENY) {
                denied.add(key);
            }
        }
        granted.removeAll(denied);
        return granted;
    }
}

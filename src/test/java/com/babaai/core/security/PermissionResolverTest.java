package com.babaai.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.babaai.core.domain.Permission;
import com.babaai.core.domain.PermissionEffect;
import com.babaai.core.domain.Role;
import com.babaai.core.domain.User;
import com.babaai.core.domain.UserPermissionOverride;
import org.junit.jupiter.api.Test;

class PermissionResolverTest {

    private final PermissionResolver resolver = new PermissionResolver();

    @Test
    void unionsPermissionsAcrossRoles() {
        Role user = role("RECIPE_VIEW", "FAVORITE_MANAGE");
        Role pro = role("AI_PRO_SUGGESTIONS");
        User u = new User();
        u.getRoles().add(user);
        u.getRoles().add(pro);

        assertThat(resolver.effectivePermissions(u))
                .containsExactlyInAnyOrder("RECIPE_VIEW", "FAVORITE_MANAGE", "AI_PRO_SUGGESTIONS");
    }

    @Test
    void grantOverrideAddsAPermission() {
        User u = new User();
        u.getPermissionOverrides().add(override(u, "AI_PRO_SUGGESTIONS", PermissionEffect.GRANT));

        assertThat(resolver.effectivePermissions(u)).containsExactly("AI_PRO_SUGGESTIONS");
    }

    @Test
    void denyOverrideStripsARolePermissionButKeepsTheRest() {
        User u = new User();
        u.getRoles().add(role("RECIPE_VIEW", "FAVORITE_MANAGE"));
        u.getPermissionOverrides().add(override(u, "FAVORITE_MANAGE", PermissionEffect.DENY));

        assertThat(resolver.effectivePermissions(u))
                .containsExactly("RECIPE_VIEW")
                .doesNotContain("FAVORITE_MANAGE");
    }

    @Test
    void denyWinsOverGrant() {
        User u = new User();
        u.getPermissionOverrides().add(override(u, "AI_PRO_SUGGESTIONS", PermissionEffect.GRANT));
        u.getPermissionOverrides().add(override(u, "AI_PRO_SUGGESTIONS", PermissionEffect.DENY));

        assertThat(resolver.effectivePermissions(u)).isEmpty();
    }

    private static Role role(String... permissionKeys) {
        Role role = new Role();
        for (String key : permissionKeys) {
            role.getPermissions().add(permission(key));
        }
        return role;
    }

    private static Permission permission(String key) {
        Permission permission = new Permission();
        permission.setKey(key);
        return permission;
    }

    private static UserPermissionOverride override(User user, String key, PermissionEffect effect) {
        UserPermissionOverride override = new UserPermissionOverride();
        override.setUser(user);
        override.setPermission(permission(key));
        override.setEffect(effect);
        return override;
    }
}

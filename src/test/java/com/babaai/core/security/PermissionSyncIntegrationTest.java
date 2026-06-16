package com.babaai.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.domain.Permission;
import com.babaai.core.repository.PermissionRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies the startup reconciler keeps the {@code permissions} table in sync with the
 * {@link AppPermission} enum. With no permissions defined yet, the table reconciles to empty.
 */
class PermissionSyncIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void permissionsTableMirrorsTheCodeEnum() {
        Set<String> dbKeys = permissionRepository.findAll().stream()
                .map(Permission::getKey)
                .collect(Collectors.toSet());
        Set<String> codeKeys = Arrays.stream(AppPermission.values())
                .map(AppPermission::key)
                .collect(Collectors.toSet());

        assertThat(dbKeys).containsExactlyInAnyOrderElementsOf(codeKeys);
    }
}

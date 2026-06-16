package com.babaai.core.bootstrap;

import com.babaai.core.service.PermissionSyncService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * On startup, reconciles permissions/roles/bundles in the DB to match the code definitions
 * ({@link com.babaai.core.security.AppPermission} / {@link com.babaai.core.security.AppRole}).
 * Runs before {@link DataSeedRunner} so the role/permission reference data exists first.
 */
@Component
@Order(1)
public class PermissionSyncRunner implements ApplicationRunner {

    private final PermissionSyncService permissionSyncService;

    public PermissionSyncRunner(PermissionSyncService permissionSyncService) {
        this.permissionSyncService = permissionSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        permissionSyncService.syncPermissions();
        permissionSyncService.syncRoles();
    }
}

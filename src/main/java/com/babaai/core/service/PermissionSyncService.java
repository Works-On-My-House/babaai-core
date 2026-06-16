package com.babaai.core.service;

import com.babaai.core.domain.Permission;
import com.babaai.core.domain.Role;
import com.babaai.core.repository.PermissionRepository;
import com.babaai.core.repository.RoleRepository;
import com.babaai.core.security.AppPermission;
import com.babaai.core.security.AppRole;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciles the {@code permissions}/{@code roles}/{@code role_permissions} tables to match the
 * {@link AppPermission} and {@link AppRole} code definitions on startup (hard-prune: the DB exactly
 * mirrors the code each boot). Run via {@code PermissionSyncRunner}.
 *
 * <p>Permissions and roles are synced in separate transactions so that a permission pruned in
 * {@link #syncPermissions()} (and its cascaded role_permissions rows) is already gone before
 * {@link #syncRoles()} rebuilds the bundles from code.
 */
@Service
public class PermissionSyncService {

    private static final Logger log = LoggerFactory.getLogger(PermissionSyncService.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionSyncService(PermissionRepository permissionRepository, RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void syncPermissions() {
        Map<String, Permission> existing = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getKey, Function.identity()));
        Set<String> codeKeys = new HashSet<>();

        for (AppPermission code : AppPermission.values()) {
            codeKeys.add(code.key());
            Permission entity = existing.get(code.key());
            if (entity == null) {
                Permission created = new Permission();
                created.setKey(code.key());
                created.setDescription(code.description());
                permissionRepository.save(created);
                log.info("Permission added: {}", code.key());
            } else if (!Objects.equals(entity.getDescription(), code.description())) {
                entity.setDescription(code.description());
                permissionRepository.save(entity);
            }
        }

        existing.values().stream()
                .filter(permission -> !codeKeys.contains(permission.getKey()))
                .forEach(permission -> {
                    // Hard-prune: DB cascade removes role_permissions + user_permission_overrides.
                    // CAVEAT: deploying an OLDER app version will delete permissions missing from it.
                    permissionRepository.delete(permission);
                    log.warn("Permission removed from code -> pruned from DB (cascades overrides): {}",
                            permission.getKey());
                });
    }

    @Transactional
    public void syncRoles() {
        Map<String, Permission> permissionsByKey = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getKey, Function.identity()));
        Map<String, Role> existing = roleRepository.findAll().stream()
                .collect(Collectors.toMap(Role::getName, Function.identity()));
        Set<String> codeNames = new HashSet<>();

        for (AppRole code : AppRole.values()) {
            codeNames.add(code.name());
            Role role = existing.getOrDefault(code.name(), new Role());
            role.setName(code.name());
            role.setDescription(code.description());
            role.setSystem(true);

            Set<Permission> desired = code.permissions().stream()
                    .map(permission -> permissionsByKey.get(permission.key()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            // Exact reconcile -> SUPER_ADMIN (grantsAll) picks up new permissions; shrinking works too.
            role.getPermissions().clear();
            role.getPermissions().addAll(desired);
            roleRepository.save(role);
        }

        existing.values().stream()
                .filter(role -> !codeNames.contains(role.getName()))
                .forEach(role -> {
                    // Hard-prune: DB cascade removes user_roles links.
                    roleRepository.delete(role);
                    log.warn("Role removed from code -> pruned from DB (cascades user_roles): {}", role.getName());
                });
    }
}

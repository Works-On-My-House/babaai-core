package com.babaai.core.api;

import com.babaai.core.service.PermissionVersionService;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal (service-token guarded — see {@code ServiceTokenFilter}) auth operations.
 *
 * <p>{@code revoke} bumps the user's permissions_version and pushes it to the gateway denylist,
 * forcing their in-flight access tokens to refresh. It is the seam the future admin
 * role/permission-management UI (ClickUp 869dqjtpc) calls, and the trigger the revocation e2e
 * tests exercise (ClickUp 869dqmbfp).
 */
@RestController
@RequestMapping("/api/v1/internal/auth")
public class InternalAuthController {

    private final PermissionVersionService permissionVersionService;

    public InternalAuthController(PermissionVersionService permissionVersionService) {
        this.permissionVersionService = permissionVersionService;
    }

    public record RevokeResponse(UUID userId, int permissionsVersion) {
    }

    @PostMapping("/users/{userId}/revoke")
    public RevokeResponse revoke(@PathVariable UUID userId) {
        int pv = permissionVersionService.bumpAndNotify(userId);
        return new RevokeResponse(userId, pv);
    }
}

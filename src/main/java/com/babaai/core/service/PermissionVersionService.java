package com.babaai.core.service;

import com.babaai.core.domain.User;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Bumps a user's {@code permissions_version} (pv) — which invalidates every access token already
 * issued to them — and pushes the new pv to the gateway so those tokens are rejected near-instantly
 * instead of lingering until their ≤15-min TTL. Call from any path that changes a user's effective
 * authorities (role assign/revoke, permission overrides, admin suspend). See ClickUp 869dqmbfp.
 */
@Service
public class PermissionVersionService {

    private final UserRepository userRepository;
    private final GatewayRevocationClient gatewayRevocationClient;

    public PermissionVersionService(
            UserRepository userRepository, GatewayRevocationClient gatewayRevocationClient) {
        this.userRepository = userRepository;
        this.gatewayRevocationClient = gatewayRevocationClient;
    }

    /**
     * Increments the user's pv and, once the surrounding transaction commits, notifies the gateway.
     * Returns the new pv. Deferring the push to after-commit (mirrors
     * {@link RecipeCatalogCache#evictAfterCommit()}) avoids advertising a revocation that then rolls back.
     */
    @Transactional
    public int bumpAndNotify(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        int newVersion = user.getPermissionsVersion() + 1;
        user.setPermissionsVersion(newVersion);
        userRepository.save(user);
        notifyAfterCommit(userId, newVersion);
        return newVersion;
    }

    private void notifyAfterCommit(UUID userId, int pv) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gatewayRevocationClient.revoke(userId, pv);
                }
            });
        } else {
            gatewayRevocationClient.revoke(userId, pv);
        }
    }
}

package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A per-user GRANT or DENY for one permission, layered over role-derived permissions.
 * Lets an admin add or strip a single permission for a user without touching the role.
 */
@Entity
@Table(
        name = "user_permission_overrides",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_permission_override",
                columnNames = {"user_id", "permission_id"}))
public class UserPermissionOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PermissionEffect effect;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public PermissionEffect getEffect() {
        return effect;
    }

    public void setEffect(PermissionEffect effect) {
        this.effect = effect;
    }
}

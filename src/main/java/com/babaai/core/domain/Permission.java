package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A single, atomic capability (e.g. {@code RECIPE_VERIFY}). Roles bundle permissions; endpoints
 * authorize on the permission key, never on the role.
 */
@Entity
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(name = "key", nullable = false, unique = true, length = 100)
    private String key;

    @Column
    private String description;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Records that a user cooked a recipe and what it consumed from their pantry (869dtvyct). The basis
 * for cook-rate and £/kg-saved metrics. The consumed snapshot is stored as JSONB so the analytics
 * don't depend on the (mutated) pantry rows.
 */
@Entity
@Table(name = "cooked_events")
public class CookedEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Column(name = "cooked_at", nullable = false)
    private Instant cookedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ConsumedLine> consumed = new ArrayList<>();

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(UUID recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public Instant getCookedAt() {
        return cookedAt;
    }

    public void setCookedAt(Instant cookedAt) {
        this.cookedAt = cookedAt;
    }

    public List<ConsumedLine> getConsumed() {
        return consumed;
    }

    public void setConsumed(List<ConsumedLine> consumed) {
        this.consumed = consumed != null ? consumed : new ArrayList<>();
    }

    /** One pantry line consumed by the cook, in the recipe ingredient's unit. */
    public static class ConsumedLine {
        private String productName;
        private Double quantity;
        private String unit;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public Double getQuantity() {
            return quantity;
        }

        public void setQuantity(Double quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}

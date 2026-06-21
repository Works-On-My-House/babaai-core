package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A user's taste profile (869dr0a4d): preferred/disliked ingredients and categories (soft signals)
 * plus dietary tags and allergens (hard safety filters). One row per user. Stored as JSONB lists to
 * avoid join-table overhead for what is a small, read-mostly per-user document.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_ingredients", nullable = false, columnDefinition = "jsonb")
    private List<String> preferredIngredients = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disliked_ingredients", nullable = false, columnDefinition = "jsonb")
    private List<String> dislikedIngredients = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_categories", nullable = false, columnDefinition = "jsonb")
    private List<String> preferredCategories = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_tags", nullable = false, columnDefinition = "jsonb")
    private List<String> dietaryTags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergens", nullable = false, columnDefinition = "jsonb")
    private List<String> allergens = new ArrayList<>();

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<String> getPreferredIngredients() {
        return preferredIngredients;
    }

    public void setPreferredIngredients(List<String> preferredIngredients) {
        this.preferredIngredients = preferredIngredients != null ? preferredIngredients : new ArrayList<>();
    }

    public List<String> getDislikedIngredients() {
        return dislikedIngredients;
    }

    public void setDislikedIngredients(List<String> dislikedIngredients) {
        this.dislikedIngredients = dislikedIngredients != null ? dislikedIngredients : new ArrayList<>();
    }

    public List<String> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(List<String> preferredCategories) {
        this.preferredCategories = preferredCategories != null ? preferredCategories : new ArrayList<>();
    }

    public List<String> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(List<String> dietaryTags) {
        this.dietaryTags = dietaryTags != null ? dietaryTags : new ArrayList<>();
    }

    public List<String> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<String> allergens) {
        this.allergens = allergens != null ? allergens : new ArrayList<>();
    }
}

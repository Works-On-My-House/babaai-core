package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A persisted "today for you" pick for one user on one day (869dr0a4d). Personalized sets are not
 * globally cacheable, so they are computed once per user/day and stored, then served all day.
 */
@Entity
@Table(name = "daily_suggestions")
public class DailySuggestion extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "generated_on", nullable = false)
    private LocalDate generatedOn;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Column(nullable = false)
    private int rank;

    @Column(nullable = false)
    private double score;

    @Column(name = "match_percent", nullable = false)
    private double matchPercent;

    @Column(name = "can_prepare", nullable = false)
    private boolean canPrepare;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> reasons = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_ingredients", nullable = false, columnDefinition = "jsonb")
    private List<String> missingIngredients = new ArrayList<>();

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getGeneratedOn() {
        return generatedOn;
    }

    public void setGeneratedOn(LocalDate generatedOn) {
        this.generatedOn = generatedOn;
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getMatchPercent() {
        return matchPercent;
    }

    public void setMatchPercent(double matchPercent) {
        this.matchPercent = matchPercent;
    }

    public boolean isCanPrepare() {
        return canPrepare;
    }

    public void setCanPrepare(boolean canPrepare) {
        this.canPrepare = canPrepare;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons != null ? reasons : new ArrayList<>();
    }

    public List<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients != null ? missingIngredients : new ArrayList<>();
    }
}

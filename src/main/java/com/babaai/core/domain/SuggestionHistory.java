package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "suggestion_history")
public class SuggestionHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "recipe_id")
    private UUID recipeId;

    @Column(name = "recipe_name", nullable = false)
    private String recipeName;

    @Column(name = "match_percent")
    private Double matchPercent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "used_ingredients", nullable = false, columnDefinition = "jsonb")
    private List<String> usedIngredients = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_ingredients", nullable = false, columnDefinition = "jsonb")
    private List<String> missingIngredients = new ArrayList<>();

    @Column(nullable = false, length = 20)
    private String source = "catalog";

    @Column(name = "agent_label", length = 100)
    private String agentLabel;

    @Column(columnDefinition = "text")
    private String preparation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<HistoryIngredientEntry> ingredients;

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

    public Double getMatchPercent() {
        return matchPercent;
    }

    public void setMatchPercent(Double matchPercent) {
        this.matchPercent = matchPercent;
    }

    public List<String> getUsedIngredients() {
        return usedIngredients;
    }

    public void setUsedIngredients(List<String> usedIngredients) {
        this.usedIngredients = usedIngredients != null ? usedIngredients : new ArrayList<>();
    }

    public List<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients != null ? missingIngredients : new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAgentLabel() {
        return agentLabel;
    }

    public void setAgentLabel(String agentLabel) {
        this.agentLabel = agentLabel;
    }

    public String getPreparation() {
        return preparation;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public List<HistoryIngredientEntry> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<HistoryIngredientEntry> ingredients) {
        this.ingredients = ingredients;
    }

    public static class HistoryIngredientEntry {
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

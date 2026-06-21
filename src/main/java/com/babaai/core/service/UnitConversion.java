package com.babaai.core.service;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UnitConversion {

    private final JsonConfigService jsonConfigService;

    public UnitConversion(JsonConfigService jsonConfigService) {
        this.jsonConfigService = jsonConfigService;
    }

    public String canonicalUnit(String unit) {
        String normalized = unit.strip().toLowerCase();
        return jsonConfigService.unitAliases().getOrDefault(normalized, normalized);
    }

    public record BaseQuantity(String family, double amount) {
    }

    public BaseQuantity toBaseQuantity(double quantity, String unit) {
        String canonical = canonicalUnit(unit);
        Map<String, Double> mass = jsonConfigService.massToGrams();
        if (mass.containsKey(canonical)) {
            return new BaseQuantity("mass", quantity * mass.get(canonical));
        }
        Map<String, Double> volume = jsonConfigService.volumeToMl();
        if (volume.containsKey(canonical)) {
            return new BaseQuantity("volume", quantity * volume.get(canonical));
        }
        Set<String> countUnits = jsonConfigService.countUnits();
        if (countUnits.contains(canonical)) {
            return new BaseQuantity("count", quantity);
        }
        return null;
    }

    /**
     * Converts a base-family amount (grams / ml / count) back into the given unit, or null if the
     * unit is unknown. Inverse of {@link #toBaseQuantity}; used to write a decremented pantry quantity
     * back in its original unit (869dtvyct).
     */
    public Double fromBaseAmount(double baseAmount, String unit) {
        BaseQuantity perUnit = toBaseQuantity(1, unit);
        if (perUnit == null || perUnit.amount() == 0) {
            return null;
        }
        return baseAmount / perUnit.amount();
    }

    public Boolean isSufficient(double requiredQuantity, String requiredUnit, double availableQuantity, String availableUnit) {
        BaseQuantity required = toBaseQuantity(requiredQuantity, requiredUnit);
        BaseQuantity available = toBaseQuantity(availableQuantity, availableUnit);
        if (required == null || available == null || !required.family().equals(available.family())) {
            return null;
        }
        return available.amount() >= required.amount();
    }
}

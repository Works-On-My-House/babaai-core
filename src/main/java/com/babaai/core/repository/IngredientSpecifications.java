package com.babaai.core.repository;

import com.babaai.core.domain.Ingredient;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class IngredientSpecifications {

    private IngredientSpecifications() {
    }

    public static Specification<Ingredient> forUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Ingredient> filter(UUID userId, String search, String category) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.strip().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), pattern));
            }
            if (category != null && !category.isBlank()) {
                var categoryJoin = root.join("categoryRef", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(categoryJoin.get("name")), category.strip().toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

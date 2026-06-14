package com.babaai.core.repository;

import com.babaai.core.domain.Dish;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, UUID> {

    Optional<Dish> findBySlug(String slug);
}

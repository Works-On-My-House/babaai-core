package com.babaai.core.service;

import com.babaai.core.domain.UserPreferences;
import com.babaai.core.dto.PreferenceDtos;
import com.babaai.core.repository.UserPreferencesRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD for the per-user taste profile (869dr0a4d). */
@Service
public class PreferenceService {

    private final UserPreferencesRepository repository;

    public PreferenceService(UserPreferencesRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PreferenceDtos.PreferencesResponse get(UUID userId) {
        return toResponse(repository.findByUserId(userId).orElseGet(UserPreferences::new));
    }

    /** The entity (or a transient empty one) used by the suggestion engine. */
    @Transactional(readOnly = true)
    public UserPreferences entityOrEmpty(UUID userId) {
        return repository.findByUserId(userId).orElseGet(UserPreferences::new);
    }

    @Transactional
    public PreferenceDtos.PreferencesResponse update(UUID userId, PreferenceDtos.PreferencesUpdateRequest request) {
        UserPreferences prefs = repository.findByUserId(userId).orElseGet(() -> {
            UserPreferences created = new UserPreferences();
            created.setUserId(userId);
            return created;
        });
        prefs.setPreferredIngredients(clean(request.preferredIngredients()));
        prefs.setDislikedIngredients(clean(request.dislikedIngredients()));
        prefs.setPreferredCategories(clean(request.preferredCategories()));
        // Tags/allergens are matched against lowercase config keys, so normalize them on the way in.
        prefs.setDietaryTags(cleanLower(request.dietaryTags()));
        prefs.setAllergens(cleanLower(request.allergens()));
        return toResponse(repository.save(prefs));
    }

    private PreferenceDtos.PreferencesResponse toResponse(UserPreferences prefs) {
        return new PreferenceDtos.PreferencesResponse(
                prefs.getPreferredIngredients(),
                prefs.getDislikedIngredients(),
                prefs.getPreferredCategories(),
                prefs.getDietaryTags(),
                prefs.getAllergens());
    }

    private List<String> clean(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                seen.add(value.strip());
            }
        }
        return new ArrayList<>(seen);
    }

    private List<String> cleanLower(List<String> values) {
        return clean(values).stream().map(v -> v.toLowerCase(Locale.ROOT)).distinct().toList();
    }
}

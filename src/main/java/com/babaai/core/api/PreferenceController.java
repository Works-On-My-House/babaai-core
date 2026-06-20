package com.babaai.core.api;

import com.babaai.core.dto.PreferenceDtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Per-user taste profile (869dr0a4d). Both endpoints require authentication. */
@RestController
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PreferenceDtos.PreferencesResponse get() {
        return preferenceService.get(SecurityUtils.requireUser().getId());
    }

    @PutMapping
    public PreferenceDtos.PreferencesResponse update(@Valid @RequestBody PreferenceDtos.PreferencesUpdateRequest request) {
        return preferenceService.update(SecurityUtils.requireUser().getId(), request);
    }
}

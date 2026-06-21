package com.babaai.core.api;

import com.babaai.core.dto.RecipeImportDtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.RecipeImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * User recipe-import submissions (ClickUp 869dtx7uq). Available to all authenticated users; the
 * upload is staged as a pending import for admin moderation. Both endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/recipe-imports")
public class RecipeImportController {

    private final RecipeImportService recipeImportService;

    public RecipeImportController(RecipeImportService recipeImportService) {
        this.recipeImportService = recipeImportService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeImportDtos.RecipeImportResponse submit(@RequestParam("file") MultipartFile file) {
        return recipeImportService.submit(SecurityUtils.requireUser().getId(), file);
    }

    @GetMapping("/mine")
    public RecipeImportDtos.RecipeImportListResponse mine() {
        return recipeImportService.listMine(SecurityUtils.requireUser().getId());
    }
}

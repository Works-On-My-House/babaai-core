package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.RecipeImport;
import com.babaai.core.dto.RecipeImportDtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.RecipeImportRepository;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Accepts user-submitted recipe files and stages them for moderation (ClickUp 869dtx7uq). Any
 * authenticated user may submit; the file is stored via {@link ImportFileStore} and a PENDING
 * {@link RecipeImport} record is created. Parsing + admin approve/reject are later slices.
 */
@Service
public class RecipeImportService {

    private final RecipeImportRepository recipeImportRepository;
    private final ImportFileStore importFileStore;
    private final AppProperties appProperties;

    public RecipeImportService(
            RecipeImportRepository recipeImportRepository,
            ImportFileStore importFileStore,
            AppProperties appProperties
    ) {
        this.recipeImportRepository = recipeImportRepository;
        this.importFileStore = importFileStore;
        this.appProperties = appProperties;
    }

    @Transactional
    public RecipeImportDtos.RecipeImportResponse submit(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("Please choose a non-empty file to upload.");
        }
        AppProperties.Imports config = appProperties.getImports();
        if (file.getSize() > config.getMaxFileSizeBytes()) {
            long maxMb = config.getMaxFileSizeBytes() / (1024 * 1024);
            throw new AppException("That file is too large. The maximum upload size is " + maxMb + " MB.");
        }
        String contentType = file.getContentType();
        List<String> allowed = config.getAllowedContentTypes();
        if (!allowed.isEmpty()
                && (contentType == null || allowed.stream().noneMatch(type -> type.equalsIgnoreCase(contentType)))) {
            throw new AppException("Unsupported file type: " + (contentType != null ? contentType : "unknown") + ".");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new AppException("We couldn't read that file. Please try again.");
        }

        RecipeImport recipeImport = new RecipeImport();
        recipeImport.setSubmittedBy(userId);
        recipeImport.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        recipeImport.setContentType(contentType);
        recipeImport.setSizeBytes(bytes.length);
        recipeImport.setStatus(RecipeImport.STATUS_PENDING);
        recipeImport = recipeImportRepository.save(recipeImport);

        importFileStore.store(recipeImport.getId(), bytes);
        return DtoMapper.toRecipeImportResponse(recipeImport);
    }

    @Transactional(readOnly = true)
    public RecipeImportDtos.RecipeImportListResponse listMine(UUID userId) {
        List<RecipeImport> mine = recipeImportRepository.findBySubmittedByOrderByCreatedAtDesc(userId);
        return new RecipeImportDtos.RecipeImportListResponse(
                mine.stream().map(DtoMapper::toRecipeImportResponse).toList(),
                mine.size());
    }

    /** Strip any path components a browser might include and bound the length. */
    private String safeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        String name = Paths.get(original).getFileName().toString().strip();
        if (name.isEmpty()) {
            return "upload";
        }
        return name.length() > 512 ? name.substring(0, 512) : name;
    }
}

package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.RecipeImport;
import com.babaai.core.dto.RecipeImportDtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.RecipeImportRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

/** Unit-level coverage of the submission rules (size / type / sanitisation / staging). */
class RecipeImportServiceTest {

    private final UUID userId = UUID.randomUUID();
    private RecipeImportRepository repository;
    private ImportFileStore fileStore;
    private AppProperties appProperties;
    private RecipeImportService service;

    @BeforeEach
    void setUp() {
        repository = mock(RecipeImportRepository.class);
        fileStore = mock(ImportFileStore.class);
        appProperties = new AppProperties();
        service = new RecipeImportService(repository, fileStore, appProperties);
        when(repository.save(any(RecipeImport.class))).thenAnswer(invocation -> {
            RecipeImport saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
    }

    private MockMultipartFile file(String name, String contentType, byte[] body) {
        return new MockMultipartFile("file", name, contentType, body);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> service.submit(userId, file("x.json", "application/json", new byte[0])))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rejectsOversizedFile() {
        appProperties.getImports().setMaxFileSizeBytes(4);
        assertThatThrownBy(() ->
                service.submit(userId, file("big.json", "application/json", "123456".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rejectsDisallowedContentTypeWhenAllowListConfigured() {
        appProperties.getImports().setAllowedContentTypes(List.of("text/csv"));
        assertThatThrownBy(() ->
                service.submit(userId, file("r.json", "application/json", "{}".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(AppException.class);
    }

    @Test
    void storesBytesCreatesPendingRecordAndSanitisesFilename() {
        byte[] body = "name,category\nTest,Pasta".getBytes(StandardCharsets.UTF_8);

        // A filename carrying path components must be reduced to its basename.
        RecipeImportDtos.RecipeImportResponse response =
                service.submit(userId, file("../../etc/recipes.csv", "text/csv", body));

        ArgumentCaptor<RecipeImport> captor = ArgumentCaptor.forClass(RecipeImport.class);
        verify(repository).save(captor.capture());
        RecipeImport saved = captor.getValue();
        assertThat(saved.getSubmittedBy()).isEqualTo(userId);
        assertThat(saved.getStatus()).isEqualTo(RecipeImport.STATUS_PENDING);
        assertThat(saved.getOriginalFilename()).isEqualTo("recipes.csv");
        assertThat(saved.getContentType()).isEqualTo("text/csv");
        assertThat(saved.getSizeBytes()).isEqualTo(body.length);

        verify(fileStore).store(eq(saved.getId()), eq(body));
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.originalFilename()).isEqualTo("recipes.csv");
        assertThat(response.sizeBytes()).isEqualTo(body.length);
    }

    @Test
    void listMineMapsAndCounts() {
        RecipeImport entry = new RecipeImport();
        entry.setId(UUID.randomUUID());
        entry.setSubmittedBy(userId);
        entry.setOriginalFilename("a.json");
        entry.setStatus(RecipeImport.STATUS_PENDING);
        when(repository.findBySubmittedByOrderByCreatedAtDesc(userId)).thenReturn(List.of(entry));

        RecipeImportDtos.RecipeImportListResponse result = service.listMine(userId);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).originalFilename()).isEqualTo("a.json");
    }
}

package com.babaai.core.repository;

import com.babaai.core.domain.SuggestionHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SuggestionHistoryRepository
        extends JpaRepository<SuggestionHistory, UUID>, JpaSpecificationExecutor<SuggestionHistory> {
}

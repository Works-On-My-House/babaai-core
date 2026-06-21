package com.babaai.core.repository;

import com.babaai.core.domain.CookedEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookedEventRepository extends JpaRepository<CookedEvent, UUID> {

    List<CookedEvent> findByUserIdOrderByCookedAtDesc(UUID userId);
}

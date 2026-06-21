package com.babaai.core.bootstrap;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.User;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.service.RescueService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly "expiry digest" job (Rescue Mode, 869dtvycn): for each user, write one notification listing
 * pantry items about to expire and recipes that use them. Per-user failures are isolated; the digest
 * is idempotent per day (see {@link RescueService#generateDigest}).
 */
@Component
public class RescueScheduler {

    private static final Logger log = LoggerFactory.getLogger(RescueScheduler.class);

    private final UserRepository userRepository;
    private final RescueService rescueService;
    private final AppProperties appProperties;

    public RescueScheduler(
            UserRepository userRepository,
            RescueService rescueService,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.rescueService = rescueService;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "${babaai.rescue.cron}")
    public void sendExpiryDigests() {
        if (!appProperties.getRescue().isSchedulerEnabled()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int sent = 0;
        int failed = 0;
        for (User user : userRepository.findAll()) {
            try {
                if (rescueService.generateDigest(user.getId(), today)) {
                    sent++;
                }
            } catch (Exception ex) {
                failed++;
                log.warn("Expiry digest failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("Expiry digests sent to {} users ({} failed)", sent, failed);
    }
}

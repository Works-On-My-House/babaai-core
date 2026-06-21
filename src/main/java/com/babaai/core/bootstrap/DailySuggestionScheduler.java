package com.babaai.core.bootstrap;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.User;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.service.DailySuggestionService;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates each user's personalized "today for you" set every morning and writes a re-engagement
 * notification (869dr0a4d). Single-instance is fine (core has no other scheduler). Per-user failures
 * are isolated so one bad user never aborts the run; each {@code generate} call is its own transaction.
 */
@Component
public class DailySuggestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySuggestionScheduler.class);

    private final UserRepository userRepository;
    private final DailySuggestionService dailySuggestionService;
    private final AppProperties appProperties;

    public DailySuggestionScheduler(
            UserRepository userRepository,
            DailySuggestionService dailySuggestionService,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.dailySuggestionService = dailySuggestionService;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "${babaai.suggestions.cron}")
    public void generateDailySuggestions() {
        AppProperties.Suggestions config = appProperties.getSuggestions();
        if (!config.isSchedulerEnabled()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int limit = config.getDailyLimit();
        int generated = 0;
        int failed = 0;
        for (User user : userRepository.findAll()) {
            try {
                dailySuggestionService.generate(user.getId(), today, limit, true);
                generated++;
            } catch (Exception ex) {
                failed++;
                log.warn("Daily suggestion generation failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("Daily suggestions generated for {} users ({} failed)", generated, failed);
    }
}

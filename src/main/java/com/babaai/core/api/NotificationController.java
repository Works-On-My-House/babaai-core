package com.babaai.core.api;

import com.babaai.core.dto.NotificationDtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.NotificationService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationDtos.NotificationListResponse list() {
        return notificationService.list(SecurityUtils.requireUser().getId());
    }

    @PatchMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(SecurityUtils.requireUser().getId());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationDtos.MarkReadResponse markRead(@PathVariable UUID notificationId) {
        return notificationService.markRead(SecurityUtils.requireUser().getId(), notificationId);
    }
}

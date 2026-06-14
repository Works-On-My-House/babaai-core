package com.babaai.core.service;

import com.babaai.core.domain.Notification;
import com.babaai.core.dto.NotificationDtos;
import com.babaai.core.exception.NotFoundException;
import com.babaai.core.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationDtos.NotificationListResponse list(UUID userId) {
        List<Notification> items = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long unread = notificationRepository.countByUserIdAndReadFalse(userId);
        return new NotificationDtos.NotificationListResponse(
                items.stream().map(DtoMapper::toNotificationResponse).toList(),
                items.size(),
                unread
        );
    }

    @Transactional
    public NotificationDtos.MarkReadResponse markRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return new NotificationDtos.MarkReadResponse(notification.getId(), notification.isRead(), notification.getUpdatedAt());
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId);
    }

    @Transactional
    public Notification create(UUID userId, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type != null ? type : "info");
        notification.setRead(false);
        return notificationRepository.save(notification);
    }
}

package com.findit.services;

import com.findit.models.Notification;
import com.findit.repositories.NotificationRepository;
import java.util.*;

public class NotificationService {
    private final NotificationRepository notifRepo = new NotificationRepository();

    public List<Notification> getUserNotifications(Long userId) {
        if (userId == null) return Collections.emptyList();
        return notifRepo.findByUserId(userId);
    }

    public boolean markAsRead(Long notificationId) {
        return notifRepo.markAsRead(notificationId);
    }

    public int markAllAsRead(Long userId) {
        return notifRepo.markAllAsRead(userId);
    }
}


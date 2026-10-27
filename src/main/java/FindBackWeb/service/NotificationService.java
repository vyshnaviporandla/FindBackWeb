package FindBackWeb.service;

import FindBackWeb.entity.AppNotification;
import FindBackWeb.repository.AppNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;

    public NotificationService(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<AppNotification> getNotifications(Long userId) {
        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        return notificationRepository
                .findByIdAndRecipientUserId(notificationId, userId)
                .map(notification -> {
                    notification.setRead(true);
                    notificationRepository.save(notification);
                    return true;
                })
                .orElse(false);
    }

    public AppNotification createNotification(
            Long recipientUserId,
            String message,
            Long relatedItemId) {

        AppNotification notification = new AppNotification();
        notification.setRecipientUserId(recipientUserId);
        notification.setMessage(message);
        notification.setRelatedItemId(relatedItemId);

        return notificationRepository.save(notification);
    }
}

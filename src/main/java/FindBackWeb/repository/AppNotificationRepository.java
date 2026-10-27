package FindBackWeb.repository;

import FindBackWeb.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);

    Optional<AppNotification> findByIdAndRecipientUserId(Long id, Long recipientUserId);
}

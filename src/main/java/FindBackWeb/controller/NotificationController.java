package FindBackWeb.controller;

import FindBackWeb.entity.AppNotification;
import FindBackWeb.entity.AppUser;
import FindBackWeb.repository.AppUserRepository;
import FindBackWeb.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    public NotificationController(
            NotificationService notificationService,
            AppUserRepository appUserRepository) {
        this.notificationService = notificationService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public List<AppNotification> getNotifications(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", notificationService.getUnreadCount(userId));
        return response;
    }

    @PatchMapping("/{id}/read")
    public Map<String, String> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        if (!notificationService.markAsRead(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Notification not found");
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return response;
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please sign in first");
        }

        AppUser user = appUserRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Account not found"));

        return user.getId();
    }
}

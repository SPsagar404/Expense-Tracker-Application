package com.expensemanager.notification.service;

import com.expensemanager.notification.entity.Notification;
import com.expensemanager.notification.entity.UserDeviceToken;
import com.expensemanager.notification.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final UserDeviceTokenRepository deviceTokenRepository;

    /**
     * Future-ready hook for Firebase Cloud Messaging (FCM) or similar.
     * Currently logs the intent to send a push notification.
     */
    public void sendPushNotification(Notification notification) {
        Long userId = notification.getUser().getId();
        List<UserDeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens registered for user {} – skipping push notification", userId);
            return;
        }

        // TODO: Integrate with FCM / APNs.
        log.info("Would send push notification '{}' to {} device(s) for user {}",
                notification.getTitle(), tokens.size(), userId);
    }
}


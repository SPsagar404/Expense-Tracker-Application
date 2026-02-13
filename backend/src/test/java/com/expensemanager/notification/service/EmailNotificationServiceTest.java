package com.expensemanager.notification.service;

import com.expensemanager.entity.User;
import com.expensemanager.notification.entity.Notification;
import com.expensemanager.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Test
    void sendNotificationEmail_SendsMailThroughJavaMailSender() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        Notification notification = Notification.builder()
                .id(10L)
                .type(NotificationType.BUDGET_ALERT)
                .title("Test Alert")
                .message("This is a test notification.")
                .user(user)
                .build();

        emailNotificationService.sendNotificationEmail(user, notification);

        verify(mailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}


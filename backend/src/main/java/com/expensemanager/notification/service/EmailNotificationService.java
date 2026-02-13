package com.expensemanager.notification.service;

import com.expensemanager.entity.User;
import com.expensemanager.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@expensemanager.local}")
    private String fromAddress;

    @Value("${app.notifications.dashboard-url:http://localhost:5173/}")
    private String dashboardUrl;

    public void sendNotificationEmail(User user, Notification notification) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping email notification for user {} - missing email", user.getId());
            return;
        }

        String subject = "[Expense Manager Alert] " + notification.getTitle();
        String body = buildBody(notification.getMessage());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Sent notification email to user {} ({})", user.getId(), user.getEmail());
    }

    private String buildBody(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n\n");
        sb.append("View details in your Expense Manager dashboard:\n");
        sb.append(dashboardUrl);
        sb.append("\n\n");
        sb.append("This is an automated message. Please do not reply.");
        return sb.toString();
    }
}


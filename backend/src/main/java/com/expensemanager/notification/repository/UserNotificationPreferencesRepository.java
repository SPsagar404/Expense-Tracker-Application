package com.expensemanager.notification.repository;

import com.expensemanager.notification.entity.UserNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationPreferencesRepository extends JpaRepository<UserNotificationPreferences, Long> {

    Optional<UserNotificationPreferences> findByUserId(Long userId);
}


package com.expensemanager.notification.repository;

import com.expensemanager.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    List<UserDeviceToken> findByUserId(Long userId);

    void deleteByDeviceToken(String deviceToken);
}


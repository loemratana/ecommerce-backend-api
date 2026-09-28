package com.example.ecommerce_api.user.repository;

import com.example.ecommerce_api.user.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);

    List<UserDevice> findByUserIdOrderByLastSeenAtDesc(Long userId);
}

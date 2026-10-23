package com.example.ecommerce_api.settings.repository;

import com.example.ecommerce_api.settings.entity.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {
}

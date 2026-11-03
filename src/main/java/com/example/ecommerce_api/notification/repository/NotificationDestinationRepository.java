package com.example.ecommerce_api.notification.repository;

import com.example.ecommerce_api.Enum.NotificationEvent;
import com.example.ecommerce_api.Enum.NotificationType;
import com.example.ecommerce_api.notification.entity.NotificationDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationDestinationRepository extends JpaRepository<NotificationDestination, Long> {

    /** Enabled destinations subscribed to the event. */
    @Query("select distinct d from NotificationDestination d join d.events e "
            + "where e = :event and d.enabled = true order by d.id")
    List<NotificationDestination> findEnabledFor(@Param("event") NotificationEvent event);

    List<NotificationDestination> findAllByOrderByIdAsc();

    boolean existsByChannelAndTargetIgnoreCase(NotificationType channel, String target);

    boolean existsByChannelAndTargetIgnoreCaseAndIdNot(NotificationType channel, String target, Long id);
}

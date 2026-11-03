package com.rishav.billing.reminder.repository;

import com.rishav.billing.reminder.entity.BusinessReminderSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessReminderSettingsRepository extends JpaRepository<BusinessReminderSettings, UUID> {

    Optional<BusinessReminderSettings> findByBusinessId(UUID businessId);

    List<BusinessReminderSettings> findByEnabledTrue();
}

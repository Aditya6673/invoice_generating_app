package com.rishav.billing.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business_reminder_settings")
public class BusinessReminderSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false, unique = true)
    private UUID businessId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "before_due_days", nullable = false)
    private int beforeDueDays = 3;

    @Column(name = "overdue_intervals", nullable = false, columnDefinition = "TEXT")
    private String overdueIntervals = "1,7,15";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public BusinessReminderSettings() {
    }

    public BusinessReminderSettings(UUID id, UUID businessId, boolean enabled, int beforeDueDays,
                                    String overdueIntervals, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.businessId = businessId;
        this.enabled = enabled;
        this.beforeDueDays = beforeDueDays;
        this.overdueIntervals = overdueIntervals;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.overdueIntervals == null) {
            this.overdueIntervals = "1,7,15";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBeforeDueDays() {
        return beforeDueDays;
    }

    public void setBeforeDueDays(int beforeDueDays) {
        this.beforeDueDays = beforeDueDays;
    }

    public String getOverdueIntervals() {
        return overdueIntervals;
    }

    public void setOverdueIntervals(String overdueIntervals) {
        this.overdueIntervals = overdueIntervals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

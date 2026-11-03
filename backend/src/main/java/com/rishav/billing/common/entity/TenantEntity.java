package com.rishav.billing.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

@MappedSuperclass
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "businessId", type = UUID.class),
    defaultCondition = "business_id = :businessId"
)
@Filter(name = "tenantFilter")
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "business_id", nullable = false, updatable = false)
    private UUID businessId;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.businessId == null) {
            this.businessId = com.rishav.billing.common.security.SecurityContextHelper.getCurrentBusinessId();
        }
        if (getCreatedBy() == null) {
            UUID userId = com.rishav.billing.common.security.SecurityContextHelper.getCurrentUserIdOrNull();
            if (userId != null) {
                setCreatedBy(userId);
                setUpdatedBy(userId);
            }
        }
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }
}

package com.rishav.billing.common.audit;

import com.rishav.billing.common.security.SecurityContextHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action, String details) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setDetails(details);

        UUID userId = SecurityContextHelper.getCurrentUserIdOrNull();
        if (userId != null) {
            entry.setPerformedBy(userId);
        }

        UUID businessId = SecurityContextHelper.getCurrentBusinessIdOrNull();
        if (businessId != null) {
            entry.setBusinessId(businessId);
        }

        auditLogRepository.save(entry);
    }
}

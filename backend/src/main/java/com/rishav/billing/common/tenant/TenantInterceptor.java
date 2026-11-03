package com.rishav.billing.common.tenant;

import com.rishav.billing.common.security.SecurityContextHelper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepts every authenticated request and enables the Hibernate tenant filter
 * so all queries on TenantEntity subclasses are automatically scoped to the
 * current user's business.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    public TenantInterceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessIdOrNull();
        if (businessId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                   .setParameter("businessId", businessId);
        }
        return true;
    }
}

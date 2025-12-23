package com.msedcl.billing.admin.audit.service;

import com.msedcl.billing.shared.entity.AuditLog;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(User user, String action, String entityType, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }

    public void record(String systemUser, String action, String entityType, Long entityId, String details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        if (systemUser != null) {
            auditLog.setDetails("[system=" + systemUser + "] " + (details == null ? "" : details));
        } else {
            auditLog.setDetails(details);
        }
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }
}

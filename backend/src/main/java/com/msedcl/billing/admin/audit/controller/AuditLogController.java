package com.msedcl.billing.admin.audit.controller;

import com.msedcl.billing.shared.entity.AuditLog;
import com.msedcl.billing.admin.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    // GET all audit logs, sorted by most recent
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByTimestampDesc());
    }

    // GET audit logs for a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogRepository.findByUser_UserIdOrderByTimestampDesc(userId));
    }

    // GET audit logs related to a specific entity (e.g., a specific bill)
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByEntity(@PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
    }
}

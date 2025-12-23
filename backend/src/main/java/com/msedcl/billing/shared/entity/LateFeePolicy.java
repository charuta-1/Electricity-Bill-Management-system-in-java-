package com.msedcl.billing.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "late_fee_policies")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LateFeePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    private Account.ConnectionType connectionType;

    @Column(name = "standard_due_days", nullable = false)
    private Integer standardDueDays = 15;

    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 3;

    @Column(name = "daily_rate_percentage", precision = 10, scale = 4)
    private BigDecimal dailyRatePercentage = BigDecimal.ZERO;

    @Column(name = "flat_fee", precision = 10, scale = 2)
    private BigDecimal flatFee = BigDecimal.ZERO;

    @Column(name = "max_late_fee", precision = 10, scale = 2)
    private BigDecimal maxLateFee;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

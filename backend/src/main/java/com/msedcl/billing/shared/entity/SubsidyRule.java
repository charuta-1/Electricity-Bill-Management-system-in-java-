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
@Table(name = "subsidy_rules")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubsidyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "tariff_code", nullable = false, length = 20)
    private String tariffCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    private Account.ConnectionType connectionType;

    @Column(name = "max_units")
    private Integer maxUnits;

    @Column(name = "per_unit_subsidy", precision = 10, scale = 4)
    private BigDecimal perUnitSubsidy = BigDecimal.ZERO;

    @Column(name = "percentage_subsidy", precision = 10, scale = 4)
    private BigDecimal percentageSubsidy = BigDecimal.ZERO;

    @Column(name = "fixed_subsidy", precision = 10, scale = 2)
    private BigDecimal fixedSubsidy = BigDecimal.ZERO;

    @Column(name = "max_benefit", precision = 10, scale = 2)
    private BigDecimal maxBenefit = BigDecimal.ZERO;

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

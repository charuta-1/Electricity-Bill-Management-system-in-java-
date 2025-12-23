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
import java.util.List;

@Entity
@Table(name = "tariff_master")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tariff_id")
    private Long tariffId;

    @Column(name = "tariff_code", unique = true, nullable = false, length = 20)
    private String tariffCode;

    @Column(name = "tariff_name", nullable = false, length = 100)
    private String tariffName;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    private Account.ConnectionType connectionType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "fixed_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedCharge = BigDecimal.ZERO;

    @Column(name = "meter_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal meterRent = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "tariffMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TariffSlab> tariffSlabs;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

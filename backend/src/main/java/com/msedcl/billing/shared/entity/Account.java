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
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "meter_number", unique = true, nullable = false, length = 50)
    private String meterNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    private ConnectionType connectionType;

    @Column(name = "sanctioned_load", nullable = false, precision = 10, scale = 2)
    private BigDecimal sanctionedLoad;

    @Column(name = "connection_date", nullable = false)
    private LocalDate connectionDate;

    @Column(name = "installation_address", nullable = false, columnDefinition = "TEXT")
    private String installationAddress;

    @Column(name = "tariff_category", nullable = false, length = 20)
    private String tariffCategory;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MeterReading> meterReadings;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bill> bills;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ConnectionType {
        RESIDENTIAL, COMMERCIAL, INDUSTRIAL, AGRICULTURAL
    }
}

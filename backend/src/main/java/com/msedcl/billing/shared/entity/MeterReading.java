package com.msedcl.billing.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_readings")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Long readingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "previous_reading", nullable = false)
    private Integer previousReading = 0;

    @Column(name = "current_reading", nullable = false)
    private Integer currentReading;

    @Column(name = "units_consumed", insertable = false, updatable = false)
    private Integer unitsConsumed;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false)
    private ReadingType readingType = ReadingType.ACTUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ReadingType {
        ACTUAL, ESTIMATED
    }
}

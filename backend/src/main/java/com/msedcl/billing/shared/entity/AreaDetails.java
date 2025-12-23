package com.msedcl.billing.shared.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "area_details")
public class AreaDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String areaName;

    @Column(nullable = false)
    private String transformerNo;

    @Column(nullable = false)
    private String feederNo;

    @Column(nullable = false)
    private String poleNo;
}
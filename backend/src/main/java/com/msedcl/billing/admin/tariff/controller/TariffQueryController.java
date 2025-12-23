package com.msedcl.billing.admin.tariff.controller;

import com.msedcl.billing.admin.tariff.dto.tariff.TariffResponse;
import com.msedcl.billing.admin.tariff.service.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
public class TariffQueryController {

    private final TariffService tariffService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TariffResponse>> listTariffs() {
        return ResponseEntity.ok(tariffService.getAllTariffsForAdmin());
    }
}

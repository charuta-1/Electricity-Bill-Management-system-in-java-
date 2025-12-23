package com.msedcl.billing.admin.tariff.controller;

import com.msedcl.billing.admin.tariff.dto.tariff.TariffResponse;
import com.msedcl.billing.shared.entity.TariffMaster;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.tariff.service.TariffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/tariffs")
@RequiredArgsConstructor
public class TariffController {

    private final TariffService tariffService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<TariffResponse>> getAllTariffs() {
        return ResponseEntity.ok(tariffService.getAllTariffsForAdmin());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TariffResponse> getTariffById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(tariffService.getTariffResponseById(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(@RequestBody TariffMaster tariffMaster,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        TariffMaster savedTariff = tariffService.createTariff(tariffMaster, currentUser, request.getRemoteAddr());
        return ResponseEntity.ok(tariffService.getTariffResponseById(savedTariff.getTariffId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TariffResponse> updateTariff(@PathVariable Long id,
                                                     @RequestBody TariffMaster tariffDetails,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        TariffMaster updatedTariff = tariffService.updateTariff(id, tariffDetails, currentUser, request.getRemoteAddr());
        return ResponseEntity.ok(tariffService.getTariffResponseById(updatedTariff.getTariffId()));
    }
}

package com.msedcl.billing.shared.controller;

import com.msedcl.billing.shared.entity.AreaDetails;
import com.msedcl.billing.shared.service.AreaDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/areas")
public class AreaDetailsController {
    @Autowired
    private AreaDetailsService areaDetailsService;

    @GetMapping
    public ResponseEntity<List<AreaDetails>> getAllAreas() {
        return ResponseEntity.ok(areaDetailsService.getAllAreas());
    }
}
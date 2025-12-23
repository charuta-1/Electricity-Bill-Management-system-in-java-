package com.msedcl.billing.shared.service;

import com.msedcl.billing.shared.entity.AreaDetails;
import com.msedcl.billing.shared.repository.AreaDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaDetailsService {
    @Autowired
    private AreaDetailsRepository areaDetailsRepository;

    public List<AreaDetails> getAllAreas() {
        return areaDetailsRepository.findAll();
    }

    public AreaDetails getAreaByName(String areaName) {
        return areaDetailsRepository.findByAreaName(areaName);
    }
}
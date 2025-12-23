package com.msedcl.billing.user.profile.controller;

import com.msedcl.billing.admin.customer.dto.customer.BillDetailResponse;
import com.msedcl.billing.admin.customer.dto.customer.ComplaintListItemDto;
import com.msedcl.billing.admin.customer.dto.customer.ConsumptionPointDto;
import com.msedcl.billing.admin.customer.dto.customer.CustomerBillListItem;
import com.msedcl.billing.admin.customer.dto.customer.CustomerQuickSummary;
import com.msedcl.billing.admin.customer.dto.customer.CustomerSelfSummaryResponse;
import com.msedcl.billing.user.dashboard.service.CustomerPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.util.StringUtils;

import java.util.List;

@RestController
@RequestMapping("/customers/self")
@RequiredArgsConstructor
public class CustomerSelfController {

    private final CustomerPortalService customerPortalService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<CustomerSelfSummaryResponse> getSummary(Authentication authentication) {
        CustomerQuickSummary summary = customerPortalService.getQuickSummary(authentication.getName());
        CustomerSelfSummaryResponse response = new CustomerSelfSummaryResponse(
            summary.outstandingAmount(),
            summary.lastBillAmount(),
            summary.averageConsumption(),
            summary.nextDueDate()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bills")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<CustomerBillListItem>> getBills(Authentication authentication) {
        return ResponseEntity.ok(customerPortalService.getAllBills(authentication.getName()));
    }

    @GetMapping("/bills/{billId}/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<Resource> downloadBillPdf(Authentication authentication, @PathVariable Long billId) {
        BillDetailResponse billDetail = customerPortalService.getBillDetail(authentication.getName(), billId);

        if (!StringUtils.hasText(billDetail.getPdfPath())) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(billDetail.getPdfPath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + resource.getFilename())
            .body(resource);
    }

    @GetMapping("/bills/{billId}/qr")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<Resource> getBillQr(Authentication authentication, @PathVariable Long billId) {
        BillDetailResponse billDetail = customerPortalService.getBillDetail(authentication.getName(), billId);

        if (!StringUtils.hasText(billDetail.getQrCodePath())) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(billDetail.getQrCodePath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + resource.getFilename())
            .body(resource);
    }

    @GetMapping("/bills/pending")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<CustomerBillListItem>> getPendingBills(Authentication authentication) {
        return ResponseEntity.ok(customerPortalService.getPendingBills(authentication.getName()));
    }

    @GetMapping("/consumption")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<ConsumptionPointDto>> getConsumptionTrend(
        Authentication authentication,
        @RequestParam(defaultValue = "6") int months
    ) {
        return ResponseEntity.ok(customerPortalService.getConsumptionTrend(authentication.getName(), months));
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<List<ComplaintListItemDto>> getComplaints(Authentication authentication) {
        return ResponseEntity.ok(customerPortalService.getCustomerComplaintFeed(authentication.getName()));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        return ResponseEntity.ok(customerPortalService.getCustomerProfile(authentication.getName()));
    }
}

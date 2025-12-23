package com.msedcl.billing.admin.billing.controller;

import com.msedcl.billing.shared.entity.Bill;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillingService billingService;
    private final BillRepository billRepository;
    private final UserRepository userRepository;

    @PostMapping("/generate/{readingId}")
    public ResponseEntity<?> generateBill(@PathVariable Long readingId, Authentication authentication) {
        try {
            User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            Bill bill = billingService.generateBill(readingId, currentUser);

            return ResponseEntity.ok(bill);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Bill>> getBillsByAccount(@PathVariable Long accountId) {
    List<Bill> bills = billRepository.findByAccountAccountIdOrderByBillDateDesc(accountId);
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBill(@PathVariable Long id) {
        return billRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<?> getBillByInvoice(@PathVariable String invoiceNumber) {
        return billRepository.findByInvoiceNumber(invoiceNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Bill>> getAllBills() {
        return ResponseEntity.ok(billRepository.findAll());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadBillPdf(@PathVariable Long id) {
        Bill bill = billRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Bill not found"));

        if (!StringUtils.hasText(bill.getPdfPath())) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(bill.getPdfPath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + resource.getFilename())
            .body(resource);
    }
}

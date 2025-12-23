package com.msedcl.billing.user.dashboard.controller;

import com.msedcl.billing.admin.customer.dto.customer.CustomerDashboardResponse;
import com.msedcl.billing.admin.customer.dto.customer.AccountSummaryDto;
import com.msedcl.billing.admin.customer.dto.customer.AccountDetailsResponse;
import com.msedcl.billing.admin.customer.dto.customer.BillSummaryDto;
import com.msedcl.billing.admin.customer.dto.customer.BillDetailResponse;
import com.msedcl.billing.admin.customer.dto.customer.PaymentSummaryDto;
import com.msedcl.billing.admin.customer.dto.customer.ComplaintSummaryDto;
import com.msedcl.billing.admin.customer.dto.customer.MeterReadingSummaryDto;
import com.msedcl.billing.user.dashboard.service.CustomerPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/customer/portal")
@RequiredArgsConstructor
public class CustomerPortalController {

    private final CustomerPortalService customerPortalService;

    @GetMapping("/summary")
    public ResponseEntity<CustomerDashboardResponse> getSummary(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getDashboard(username));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountSummaryDto>> getAccounts(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getCustomerAccounts(username));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(Authentication authentication, @PathVariable Long accountId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getAccountDetails(username, accountId));
    }

    @GetMapping("/accounts/{accountId}/bills")
    public ResponseEntity<List<BillSummaryDto>> getAccountBills(Authentication authentication, @PathVariable Long accountId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getAccountBills(username, accountId));
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<BillDetailResponse> getBillDetail(Authentication authentication, @PathVariable Long billId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getBillDetail(username, billId));
    }

    @GetMapping("/accounts/{accountId}/payments")
    public ResponseEntity<List<PaymentSummaryDto>> getAccountPayments(Authentication authentication, @PathVariable Long accountId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getAccountPayments(username, accountId));
    }

    @GetMapping("/accounts/{accountId}/complaints")
    public ResponseEntity<List<ComplaintSummaryDto>> getAccountComplaints(Authentication authentication, @PathVariable Long accountId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getAccountComplaints(username, accountId));
    }

    @GetMapping("/accounts/{accountId}/readings")
    public ResponseEntity<List<MeterReadingSummaryDto>> getAccountReadings(Authentication authentication, @PathVariable Long accountId) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getAccountReadings(username, accountId));
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<ComplaintSummaryDto>> getCustomerComplaints(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(customerPortalService.getCustomerComplaints(username));
    }
}

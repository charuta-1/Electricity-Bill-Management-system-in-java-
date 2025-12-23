package com.msedcl.billing.admin.account.controller;

import com.msedcl.billing.admin.account.dto.account.AccountResponse;
import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    // GET all accounts
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccountsForAdmin());
    }

    // GET a single account by ID
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(accountService.getAccountResponse(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // GET all accounts for a specific customer
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountResponsesByCustomer(customerId));
    }

    @GetMapping("/next-meter")
    public ResponseEntity<String> getNextMeterNumber() {
        return ResponseEntity.ok(accountService.previewNextMeterNumber());
    }

    // POST (Create) a new account
    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Account account,
                                                 Authentication authentication,
                                                 HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Account savedAccount = accountService.createAccount(account, currentUser, request.getRemoteAddr());
            return ResponseEntity.ok(accountService.getAccountResponse(savedAccount.getAccountId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Long id,
                                                 @RequestBody Account accountDetails,
                                                 Authentication authentication,
                                                 HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Account updatedAccount = accountService.updateAccount(id, accountDetails, currentUser, request.getRemoteAddr());
            return ResponseEntity.ok(accountService.getAccountResponse(updatedAccount.getAccountId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        accountService.deleteAccount(id, currentUser, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

package com.msedcl.billing.admin.account.service;

import com.msedcl.billing.admin.account.dto.account.AccountResponse;
import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.account.repository.AccountRepository;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Set<String> ALLOWED_TARIFF_CATEGORIES = Set.of("LT-I", "LT-II", "LT-III", "LT-IV", "LT-V");
    private static final String METER_PREFIX = "MTR-";
    private static final int METER_SEQUENCE_WIDTH = 4;
    private static final Map<Account.ConnectionType, Set<String>> CONNECTION_TARIFF_RULES = new EnumMap<>(Account.ConnectionType.class);

    static {
        CONNECTION_TARIFF_RULES.put(Account.ConnectionType.RESIDENTIAL, Set.of("LT-I"));
        CONNECTION_TARIFF_RULES.put(Account.ConnectionType.COMMERCIAL, Set.of("LT-II"));
        CONNECTION_TARIFF_RULES.put(Account.ConnectionType.INDUSTRIAL, Set.of("LT-III"));
        CONNECTION_TARIFF_RULES.put(Account.ConnectionType.AGRICULTURAL, Set.of("LT-IV", "LT-V"));
    }

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public List<AccountResponse> getAllAccountsForAdmin() {
        return accountRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findByAccountId(id)
            .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
    }

    public AccountResponse getAccountResponse(Long id) {
        return AccountResponse.from(getAccountById(id));
    }

    public List<Account> getAccountsByCustomer(Long customerId) {
        return accountRepository.findByCustomerCustomerId(customerId);
    }

    public List<AccountResponse> getAccountResponsesByCustomer(Long customerId) {
        return accountRepository.findByCustomerCustomerId(customerId)
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    @Transactional
    public Account createAccount(Account account, User actor, String ipAddress) {
        Long customerId = account.getCustomer() != null ? account.getCustomer().getCustomerId() : null;
        if (customerId == null) {
            throw new RuntimeException("Customer information is required to create an account");
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found for this account"));

        account.setCustomer(customer);
        account.setAccountNumber(generateAccountNumber());

        String meterNumber = account.getMeterNumber();
        if (meterNumber == null || meterNumber.isBlank()) {
            meterNumber = generateMeterNumber();
        } else {
            meterNumber = normalizeMeterNumber(meterNumber);
            if (accountRepository.existsByMeterNumber(meterNumber)) {
                throw new IllegalArgumentException("Meter number already exists: " + meterNumber);
            }
        }
        account.setMeterNumber(meterNumber);
        if (account.getConnectionDate() == null) {
            account.setConnectionDate(LocalDate.now());
        }

        account.setTariffCategory(normalizeTariffCategory(account.getConnectionType(), account.getTariffCategory()));

        Account savedAccount = accountRepository.save(account);

        auditLogService.record(actor,
            "CREATE_ACCOUNT",
            "Account",
            savedAccount.getAccountId(),
            String.format("Created account %s for customer %s", savedAccount.getAccountNumber(), customer.getFullName()),
            ipAddress);

        return savedAccount;
    }

    @Transactional
    public Account updateAccount(Long id, Account accountDetails, User actor, String ipAddress) {
        Account account = getAccountById(id);

        String meterNumber = normalizeMeterNumber(accountDetails.getMeterNumber());
        if (!meterNumber.equals(account.getMeterNumber()) && accountRepository.existsByMeterNumber(meterNumber)) {
            throw new IllegalArgumentException("Meter number already exists: " + meterNumber);
        }

    account.setMeterNumber(meterNumber);
    account.setConnectionType(accountDetails.getConnectionType());
        account.setSanctionedLoad(accountDetails.getSanctionedLoad());
        account.setConnectionDate(accountDetails.getConnectionDate());
        account.setInstallationAddress(accountDetails.getInstallationAddress());
    account.setTariffCategory(normalizeTariffCategory(account.getConnectionType(), accountDetails.getTariffCategory()));
        account.setIsActive(accountDetails.getIsActive());

        Account updatedAccount = accountRepository.save(account);

        auditLogService.record(actor,
            "UPDATE_ACCOUNT",
            "Account",
            updatedAccount.getAccountId(),
            String.format("Updated account %s", updatedAccount.getAccountNumber()),
            ipAddress);

        return updatedAccount;
    }

    @Transactional
    public void deleteAccount(Long id, User actor, String ipAddress) {
        Account account = getAccountById(id);
        accountRepository.delete(account);

        auditLogService.record(actor,
            "DELETE_ACCOUNT",
            "Account",
            id,
            String.format("Deleted account %s", account.getAccountNumber()),
            ipAddress);
    }

    private String generateAccountNumber() {
        String datePart = LocalDate.now().format(NUMBER_DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ACC-" + datePart + "-" + randomPart;
    }

    public synchronized String previewNextMeterNumber() {
        return nextAvailableMeterNumber();
    }

    private synchronized String generateMeterNumber() {
        return nextAvailableMeterNumber();
    }

    private long resolveNextMeterSequence() {
        return accountRepository.findTopByOrderByMeterNumberDesc()
            .map(Account::getMeterNumber)
            .map(this::extractMeterSequence)
            .orElse(0L) + 1L;
    }

    private String nextAvailableMeterNumber() {
        long nextSequence = resolveNextMeterSequence();
        String candidate;

        do {
            candidate = formatMeterNumber(nextSequence);
            nextSequence++;
        } while (accountRepository.existsByMeterNumber(candidate));

        return candidate;
    }

    private String formatMeterNumber(long sequence) {
        return METER_PREFIX + String.format("%0" + METER_SEQUENCE_WIDTH + "d", sequence);
    }

    private long extractMeterSequence(String meterNumber) {
        if (meterNumber == null) {
            return 0L;
        }
        String digits = meterNumber.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String normalizeMeterNumber(String rawMeterNumber) {
        if (rawMeterNumber == null) {
            throw new IllegalArgumentException("Meter number is required");
        }
        String trimmed = rawMeterNumber.trim().toUpperCase(Locale.ROOT);
        if (!trimmed.startsWith(METER_PREFIX)) {
            trimmed = METER_PREFIX + trimmed.replaceAll("\\D", "");
        }

        String digits = trimmed.replace(METER_PREFIX, "").replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Meter number must include numeric sequence");
        }

        try {
            long sequence = Long.parseLong(digits);
            digits = String.format("%0" + METER_SEQUENCE_WIDTH + "d", sequence);
            return METER_PREFIX + digits;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid meter number sequence: " + rawMeterNumber);
        }
    }

    private String normalizeTariffCategory(Account.ConnectionType connectionType, String tariffCategory) {
        if (tariffCategory == null) {
            throw new IllegalArgumentException("Tariff category is required");
        }

        String normalized = tariffCategory.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TARIFF_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid tariff category: " + tariffCategory + ". Supported: " + ALLOWED_TARIFF_CATEGORIES);
        }

        if (connectionType == null) {
            throw new IllegalArgumentException("Connection type is required to validate tariff category");
        }

        Set<String> permitted = CONNECTION_TARIFF_RULES.get(connectionType);
        if (permitted == null || !permitted.contains(normalized)) {
            throw new IllegalArgumentException(
                String.format("Tariff %s is not permitted for %s connections", normalized, connectionType.name().toLowerCase(Locale.ROOT))
            );
        }

        return normalized;
    }
}

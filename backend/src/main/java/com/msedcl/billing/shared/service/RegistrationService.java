package com.msedcl.billing.shared.service;

import com.msedcl.billing.shared.dto.AuthResponse;
import com.msedcl.billing.shared.dto.RegisterRequest;
import com.msedcl.billing.admin.customer.dto.customer.CustomerResponse;
import com.msedcl.billing.shared.entity.AreaDetails;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.shared.repository.AreaDetailsRepository;
import com.msedcl.billing.shared.security.JwtUtil;
import com.msedcl.billing.admin.customer.service.CustomerService;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AreaDetailsRepository areaDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerService customerService;
    private final AuditLogService auditLogService;

    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$"
    );

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        NormalizedCustomerRegistration data = normalizeCustomerRequest(request);
        validateCustomerRegistration(data);

        User savedUser = persistCustomerUser(data, request.getPassword());
    customerService.registerCustomerProfile(data.toCustomer(savedUser, areaDetailsRepository));

        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole().name(), savedUser.getUserId());
        return new AuthResponse(
            token,
            savedUser.getUserId(),
            savedUser.getUsername(),
            savedUser.getRole().name(),
            savedUser.getFullName()
        );
    }

    @Transactional
    public CustomerResponse registerCustomerByAdmin(RegisterRequest request, User actor, String ipAddress) {
        NormalizedCustomerRegistration data = normalizeCustomerRequest(request);
        validateCustomerRegistration(data);

        User savedUser = persistCustomerUser(data, request.getPassword());
        Customer customer = customerService.createCustomer(data.toCustomer(savedUser, areaDetailsRepository), actor, ipAddress);
        CustomerResponse response = customerService.toResponse(customer);

        auditLogService.record(actor,
            "REGISTER_CUSTOMER_ACCOUNT",
            "User",
            savedUser.getUserId(),
            String.format("Provisioned login for customer %s", savedUser.getUsername()),
            ipAddress);

        return response;
    }

    @Transactional(readOnly = true)
    public List<User> listAdmins() {
        return userRepository.findAllByRoleOrderByCreatedAtDesc(User.UserRole.ADMIN);
    }

    @Transactional
    public User createAdmin(String username,
                            String email,
                            String fullName,
                            String phoneNumber,
                            String rawPassword,
                            boolean active,
                            User actor,
                            String ipAddress) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phoneNumber);

        validateAdminInputs(normalizedUsername, normalizedEmail, normalizedPhone);
        ensureStrongPassword(rawPassword, true);

        User admin = new User();
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setFullName(fullName.trim());
        admin.setPhoneNumber(normalizedPhone);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(User.UserRole.ADMIN);
        admin.setIsActive(active);

        User savedAdmin = userRepository.save(admin);

        auditLogService.record(actor,
            "CREATE_ADMIN",
            "User",
            savedAdmin.getUserId(),
            String.format("Created admin %s", savedAdmin.getUsername()),
            ipAddress);

        return savedAdmin;
    }

    @Transactional
    public User updateAdminStatus(Long adminId, boolean active, User actor, String ipAddress) {
        User admin = userRepository.findById(adminId)
            .filter(user -> user.getRole() == User.UserRole.ADMIN)
            .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        if (!active && actor.getUserId().equals(adminId)) {
            throw new IllegalStateException("Administrators cannot deactivate their own account");
        }

        if (Boolean.TRUE.equals(admin.getIsActive()) && !active && isLastActiveAdmin(adminId)) {
            throw new IllegalStateException("At least one active admin must remain in the system");
        }

        admin.setIsActive(active);
        User updated = userRepository.save(admin);

        auditLogService.record(actor,
            active ? "ACTIVATE_ADMIN" : "DEACTIVATE_ADMIN",
            "User",
            updated.getUserId(),
            String.format("%s admin %s", active ? "Activated" : "Deactivated", updated.getUsername()),
            ipAddress);

        return updated;
    }

    private User persistCustomerUser(NormalizedCustomerRegistration data, String rawPassword) {
        ensureStrongPassword(rawPassword, false);

        User user = new User();
        user.setUsername(data.username);
        user.setEmail(data.email);
        user.setFullName(data.fullName);
        user.setPhoneNumber(data.phoneNumber);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(User.UserRole.CUSTOMER);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    private void validateCustomerRegistration(NormalizedCustomerRegistration data) {
        validateAdminInputs(data.username, data.email, data.phoneNumber);
        customerRepository.findByPhoneNumber(data.phoneNumber)
            .ifPresent(customer -> {
                throw new IllegalArgumentException("Phone number is already registered");
            });
        if (data.aadharNumber != null) {
            customerRepository.findByAadharNumber(data.aadharNumber)
                .ifPresent(c -> { throw new IllegalArgumentException("Aadhar number is already linked to another customer"); });
        }
    }

    private void validateAdminInputs(String username, String email, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (phone != null && userRepository.existsByPhoneNumber(phone)) {
            throw new IllegalArgumentException("Phone number is already registered");
        }
    }

    private boolean isLastActiveAdmin(Long adminId) {
        return userRepository.findAllByRoleOrderByCreatedAtDesc(User.UserRole.ADMIN)
            .stream()
            .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
            .filter(user -> !user.getUserId().equals(adminId))
            .findAny()
            .isEmpty();
    }

    private NormalizedCustomerRegistration normalizeCustomerRequest(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhoneNumber());
        String fullName = request.getFullName().trim();
        String address = request.getAddress().trim();
        String city = request.getCity() != null ? request.getCity().trim() : null;
        String state = request.getState() != null && !request.getState().isBlank()
            ? request.getState().trim()
            : null;
        String pincode = request.getPincode() != null ? request.getPincode().trim() : null;
        String aadhar = request.getAadharNumber() != null && !request.getAadharNumber().isBlank()
            ? request.getAadharNumber().trim()
            : null;
        Long areaId = request.getAreaId();
        Double advancePayment = request.getAdvancePayment() != null ? request.getAdvancePayment() : 0.00;

        return new NormalizedCustomerRegistration(username, email, phone, fullName, address, city, state, pincode, aadhar, areaId, advancePayment);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.replaceAll("\\s+", "");
    }

    private void ensureStrongPassword(String password, boolean adminContext) {
        if (password == null || !STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(adminContext
                ? "Admin password must include upper, lower, digit, special character and be at least 8 characters long"
                : "Password must include upper, lower, digit, special character and be at least 8 characters long");
        }
    }

    private record NormalizedCustomerRegistration(
        String username,
        String email,
        String phoneNumber,
        String fullName,
        String address,
        String city,
        String state,
        String pincode,
        String aadharNumber,
        Long areaId,
        Double advancePayment
    ) {
        Customer toCustomer(User user, AreaDetailsRepository areaRepo) {
            Customer customer = new Customer();
            customer.setUser(user);
            customer.setFullName(fullName);
            customer.setEmail(email);
            customer.setPhoneNumber(phoneNumber);
            customer.setAddress(address);
            customer.setCity(city);
            if (state != null) {
                customer.setState(state);
            }
            customer.setPincode(pincode);
            customer.setAadharNumber(aadharNumber);
            customer.setAdvancePayment(advancePayment != null ? advancePayment : 0.00);
            
            // Set area details if provided
            if (areaId != null) {
                AreaDetails areaDetails = areaRepo.findById(areaId).orElse(null);
                customer.setAreaDetails(areaDetails);
            }
            
            return customer;
        }
    }
}

package com.msedcl.billing.admin.billing.service;

import com.msedcl.billing.shared.entity.Bill;
import com.msedcl.billing.shared.entity.Bill.BillStatus;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.shared.service.NotificationService;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingReminderScheduler {

    private static final EnumSet<BillStatus> PENDING_STATUSES = EnumSet.of(BillStatus.UNPAID, BillStatus.PARTIALLY_PAID);

    private final BillRepository billRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendUpcomingDueReminders() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(3);
        List<Bill> upcomingBills = billRepository.findByDueDateBetweenAndBillStatusIn(today, windowEnd, PENDING_STATUSES);
        upcomingBills.forEach(bill -> {
            try {
                notificationService.sendBillReminderEmail(bill, false);
                auditLogService.record("SYSTEM", "BILL_REMINDER", "Bill", bill.getBillId(),
                    "Sent upcoming due reminder for invoice " + bill.getInvoiceNumber(), null);
            } catch (Exception ex) {
                log.error("Failed to send upcoming due reminder for invoice {}", bill.getInvoiceNumber(), ex);
            }
        });
    }

    @Scheduled(cron = "0 0 18 * * ?")
    public void sendOverdueReminders() {
        LocalDate today = LocalDate.now();
        List<Bill> overdueBills = billRepository.findByBillStatusInAndDueDateBefore(PENDING_STATUSES, today);
        overdueBills.forEach(bill -> {
            try {
                notificationService.sendBillReminderEmail(bill, true);
                auditLogService.record("SYSTEM", "BILL_OVERDUE_REMINDER", "Bill", bill.getBillId(),
                    "Sent overdue reminder for invoice " + bill.getInvoiceNumber(), null);
            } catch (Exception ex) {
                log.error("Failed to send overdue reminder for invoice {}", bill.getInvoiceNumber(), ex);
            }
        });
    }
}

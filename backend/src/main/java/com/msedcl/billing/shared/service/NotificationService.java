package com.msedcl.billing.shared.service;

import com.msedcl.billing.shared.entity.Bill;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.Payment;
import com.msedcl.billing.shared.service.template.TemplateRenderer;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JavaMailSender mailSender;
    private final TemplateRenderer templateRenderer;
    private final SmsService smsService;
    @Value("${notifications.email.from:no-reply@vit.edu}")
    private String fromAddress;

    @Async
    public void sendBillGeneratedEmail(Bill bill) {
        Customer customer = bill.getAccount().getCustomer();
        if (!StringUtils.hasText(customer.getEmail())) {
            log.info("Skipping email notification for customer {} due to missing email", customer.getCustomerNumber());
            return;
        }

        Map<String, Object> model = Map.of(
            "customerName", customer.getFullName(),
            "accountNumber", bill.getAccount().getAccountNumber(),
            "invoiceNumber", bill.getInvoiceNumber(),
            "billDate", DATE_FORMATTER.format(bill.getBillDate()),
            "dueDate", DATE_FORMATTER.format(bill.getDueDate()),
            "netPayable", bill.getNetPayable(),
            "pdfPath", bill.getPdfPath()
        );

        sendEmail(customer.getEmail(),
            "Your electricity bill is ready - " + bill.getInvoiceNumber(),
            templateRenderer.render("bill-generated", model));

        smsService.sendSms(
            customer.getPhoneNumber(),
            String.format(
                "VIT Billing: Invoice %s generated. Amount ₹%s due by %s.",
                bill.getInvoiceNumber(),
                bill.getNetPayable().setScale(2, java.math.RoundingMode.HALF_UP),
                DATE_FORMATTER.format(bill.getDueDate())
            )
        );
    }

    @Async
    public void sendPaymentReceiptEmail(Payment payment) {
        Customer customer = payment.getAccount().getCustomer();
        if (!StringUtils.hasText(customer.getEmail())) {
            log.info("Skipping receipt email for customer {} due to missing email", customer.getCustomerNumber());
            return;
        }

        Map<String, Object> model = Map.of(
            "customerName", customer.getFullName(),
            "accountNumber", payment.getAccount().getAccountNumber(),
            "invoiceNumber", payment.getBill().getInvoiceNumber(),
            "paymentReference", payment.getPaymentReference(),
            "paymentDate", payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")),
            "paymentAmount", payment.getPaymentAmount(),
            "convenienceFee", payment.getConvenienceFee(),
            "netAmount", payment.getNetAmount()
        );

        sendEmail(customer.getEmail(),
            "Payment received - " + payment.getPaymentReference(),
            templateRenderer.render("payment-receipt", model));

        smsService.sendSms(
            customer.getPhoneNumber(),
            String.format(
                "VIT Billing: Payment %s of ₹%s received. Thank you!",
                payment.getPaymentReference(),
                payment.getNetAmount().setScale(2, java.math.RoundingMode.HALF_UP)
            )
        );
    }

    @Async
    public void sendBillReminderEmail(Bill bill, boolean overdue) {
        Customer customer = bill.getAccount().getCustomer();
        if (!StringUtils.hasText(customer.getEmail())) {
            log.info("Skipping reminder email for customer {} due to missing email", customer.getCustomerNumber());
            return;
        }

        Map<String, Object> model = Map.of(
            "customerName", customer.getFullName(),
            "accountNumber", bill.getAccount().getAccountNumber(),
            "invoiceNumber", bill.getInvoiceNumber(),
            "dueDate", DATE_FORMATTER.format(bill.getDueDate()),
            "netPayable", bill.getNetPayable()
        );

        String template = overdue ? "bill-overdue" : "bill-reminder";
        String subject = overdue
            ? "Overdue electricity bill - " + bill.getInvoiceNumber()
            : "Upcoming bill due - " + bill.getInvoiceNumber();

        sendEmail(customer.getEmail(), subject, templateRenderer.render(template, model));

        smsService.sendSms(
            customer.getPhoneNumber(),
            overdue
                ? String.format(
                    "VIT Billing: Invoice %s is overdue. Please pay ₹%s immediately.",
                    bill.getInvoiceNumber(),
                    bill.getNetPayable().setScale(2, java.math.RoundingMode.HALF_UP)
                )
                : String.format(
                    "VIT Billing: Invoice %s due on %s. Amount ₹%s.",
                    bill.getInvoiceNumber(),
                    DATE_FORMATTER.format(bill.getDueDate()),
                    bill.getNetPayable().setScale(2, java.math.RoundingMode.HALF_UP)
                )
        );
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            if (StringUtils.hasText(fromAddress)) {
                helper.setFrom(fromAddress);
            }
            mailSender.send(mimeMessage);
        } catch (MailException ex) {
            log.error("Failed to send email to {}", to, ex);
        } catch (Exception ex) {
            log.error("Unexpected error while sending email to {}", to, ex);
        }
    }
}

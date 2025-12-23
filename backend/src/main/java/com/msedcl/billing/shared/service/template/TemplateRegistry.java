package com.msedcl.billing.shared.service.template;

import java.util.Map;

public final class TemplateRegistry {

    private static final Map<String, String> TEMPLATES = Map.ofEntries(
        Map.entry("bill-generated",
            """
            <html>
            <body>
                <p>Dear {{customerName}},</p>
                <p>Your electricity bill for account <strong>{{accountNumber}}</strong> has been generated.</p>
                <p>
                    Invoice Number: <strong>{{invoiceNumber}}</strong><br/>
                    Bill Date: {{billDate}}<br/>
                    Due Date: {{dueDate}}<br/>
                    Amount Due: Rs {{netPayable}}
                </p>
                <p>You can download the bill PDF from your portal.</p>
                <p>Thank you,<br/>VIT Billing Team</p>
            </body>
            </html>
            """),
        Map.entry("payment-receipt",
            """
            <html>
            <body>
                <p>Dear {{customerName}},</p>
                <p>We have received your payment for invoice <strong>{{invoiceNumber}}</strong>.</p>
                <p>
                    Payment Reference: <strong>{{paymentReference}}</strong><br/>
                    Date: {{paymentDate}}<br/>
                    Amount: Rs {{paymentAmount}}<br/>
                    Convenience Fee: Rs {{convenienceFee}}<br/>
                    Net Amount: Rs {{netAmount}}
                </p>
                <p>Thank you for staying current with your electricity bills.</p>
                <p>Regards,<br/>VIT Billing Team</p>
            </body>
            </html>
            """),
        Map.entry("bill-reminder",
            """
            <html>
            <body>
                <p>Dear {{customerName}},</p>
                <p>This is a friendly reminder that your electricity bill <strong>{{invoiceNumber}}</strong> for account <strong>{{accountNumber}}</strong> is due on {{dueDate}}.</p>
                <p>The outstanding amount is Rs {{netPayable}}. Please make the payment before the due date to avoid late fees.</p>
                <p>Thank you,<br/>VIT Billing Team</p>
            </body>
            </html>
            """),
        Map.entry("bill-overdue",
            """
            <html>
            <body>
                <p>Dear {{customerName}},</p>
                <p>Your electricity bill <strong>{{invoiceNumber}}</strong> for account <strong>{{accountNumber}}</strong> is now overdue since {{dueDate}}.</p>
                <p>The outstanding amount is Rs {{netPayable}} including any applicable late fees. Kindly clear the dues immediately to avoid service interruption.</p>
                <p>If you have already paid, please ignore this message.</p>
                <p>Regards,<br/>VIT Billing Team</p>
            </body>
            </html>
            """ )
    );

    private TemplateRegistry() {
    }

    public static String getTemplate(String templateName) {
        return TEMPLATES.get(templateName);
    }
}


package com.msedcl.billing.shared.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.msedcl.billing.shared.entity.Bill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class QrCodeService {

    @Value("${pdf.storage.path}")
    private String qrStoragePath;

    private static final String UPI_ID = "msedcl@upi";
    private static final String PAYEE_NAME = "MSEDCL";

    public String generateQrCode(Bill bill) {
        try {
            File directory = new File(qrStoragePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String upiString = buildUpiString(bill);

            String fileName = "QR_" + bill.getInvoiceNumber().replace("/", "_") + ".png";
            String filePath = qrStoragePath + fileName;

            generateQRCodeImage(upiString, 300, 300, filePath);

            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Error generating QR code: " + e.getMessage(), e);
        }
    }

    private String buildUpiString(Bill bill) {
        StringBuilder upiString = new StringBuilder();
        upiString.append("upi://pay?");
        upiString.append("pa=").append(UPI_ID);
        upiString.append("&pn=").append(PAYEE_NAME);
    upiString.append("&am=").append(formatAmount(Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable())));
        upiString.append("&tn=").append("Bill Payment - ").append(bill.getInvoiceNumber());
        upiString.append("&cu=INR");

        return upiString.toString();
    }

    private void generateQRCodeImage(String text, int width, int height, String filePath)
            throws WriterException, IOException {

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}

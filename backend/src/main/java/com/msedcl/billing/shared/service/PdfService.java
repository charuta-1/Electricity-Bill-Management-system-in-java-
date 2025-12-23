package com.msedcl.billing.shared.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.msedcl.billing.shared.entity.Bill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfService {

    @Value("${pdf.storage.path}")
    private String pdfStoragePath;

    @Value("${branding.logo.path:}")
    private String logoPath;

    private static final float LOGO_MAX_WIDTH = 110f;
    private static final String BRAND_NAME = "VIT EnergySuite";

    public String generateBillPdf(Bill bill) {
        try {
            File directory = new File(pdfStoragePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

        String fileName = "VIT_ENERGYSUITE_BILL_" + bill.getInvoiceNumber().replace("/", "_") + ".pdf";
            String filePath = pdfStoragePath + fileName;

            PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 3}))
            .useAllAvailableWidth()
            .setMarginBottom(15);

        Image logo = loadLogoImage();
        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.LEFT);
        if (logo != null) {
        logoCell.add(logo);
        }
        headerTable.addCell(logoCell);

        Cell textCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        textCell.add(new Paragraph(BRAND_NAME)
            .setFontSize(16)
            .setBold()
            .setMarginBottom(2));
        textCell.add(new Paragraph("Official Electricity Bill")
            .setFontSize(12)
            .setBold()
            .setMarginBottom(4));
        textCell.add(new Paragraph("Trusted energy insights for campuses & communities")
            .setFontSize(9)
            .setItalic()
            .setFontColor(ColorConstants.GRAY));
        headerTable.addCell(textCell);

        document.add(headerTable);

        document.add(new Paragraph("\n"));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                    .useAllAvailableWidth();

            infoTable.addCell(createCell("Invoice Number:", true));
            infoTable.addCell(createCell(bill.getInvoiceNumber(), false));

            infoTable.addCell(createCell("Account Number:", true));
            infoTable.addCell(createCell(bill.getAccount().getAccountNumber(), false));

            infoTable.addCell(createCell("Customer Name:", true));
            infoTable.addCell(createCell(bill.getAccount().getCustomer().getFullName(), false));

            // Add area details if available
            if (bill.getAccount().getCustomer().getAreaDetails() != null) {
                var areaDetails = bill.getAccount().getCustomer().getAreaDetails();
                infoTable.addCell(createCell("Area:", true));
                infoTable.addCell(createCell(areaDetails.getAreaName(), false));
                
                infoTable.addCell(createCell("Transformer No:", true));
                infoTable.addCell(createCell(areaDetails.getTransformerNo(), false));
                
                infoTable.addCell(createCell("Feeder No:", true));
                infoTable.addCell(createCell(areaDetails.getFeederNo(), false));
                
                infoTable.addCell(createCell("Pole No:", true));
                infoTable.addCell(createCell(areaDetails.getPoleNo(), false));
            }

            infoTable.addCell(createCell("Bill Date:", true));
            infoTable.addCell(createCell(formatDate(bill.getBillDate()), false));

            infoTable.addCell(createCell("Due Date:", true));
            infoTable.addCell(createCell(formatDate(bill.getDueDate()), false));

            infoTable.addCell(createCell("Billing Month:", true));
            infoTable.addCell(createCell(bill.getBillMonth(), false));

            document.add(infoTable);
            document.add(new Paragraph("\n"));

            Table chargesTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .useAllAvailableWidth();

            chargesTable.addCell(createHeaderCell("Description"));
            chargesTable.addCell(createHeaderCell("Amount (Rs)"));

            chargesTable.addCell(createCell("Units Consumed: " + bill.getUnitsConsumed() + " kWh", false));
            chargesTable.addCell(createCell("", false));

            chargesTable.addCell(createCell("Energy Charges", false));
            chargesTable.addCell(createCell(formatAmount(bill.getEnergyCharges()), false));

            chargesTable.addCell(createCell("Fixed Charges", false));
            chargesTable.addCell(createCell(formatAmount(bill.getFixedCharges()), false));

            chargesTable.addCell(createCell("Meter Rent", false));
            chargesTable.addCell(createCell(formatAmount(bill.getMeterRent()), false));

            chargesTable.addCell(createCell("Electricity Duty", false));
            chargesTable.addCell(createCell(formatAmount(bill.getElectricityDuty()), false));

            BigDecimal otherCharges = bill.getOtherCharges() == null ? BigDecimal.ZERO : bill.getOtherCharges();
            if (otherCharges.compareTo(BigDecimal.ZERO) > 0) {
                chargesTable.addCell(createCell("Other Charges (Fuel / Wheeling)", false));
                chargesTable.addCell(createCell(formatAmount(otherCharges), false));
            }

            BigDecimal subsidy = bill.getSubsidyAmount() == null ? BigDecimal.ZERO : bill.getSubsidyAmount();
            if (subsidy.compareTo(BigDecimal.ZERO) > 0) {
                chargesTable.addCell(createCell("Subsidy", false));
                chargesTable.addCell(createCell("-" + formatAmount(subsidy), false));
            }

            BigDecimal lateFee = bill.getLateFee() == null ? BigDecimal.ZERO : bill.getLateFee();
            if (lateFee.compareTo(BigDecimal.ZERO) > 0) {
                chargesTable.addCell(createCell("Late Fee", false));
                chargesTable.addCell(createCell(formatAmount(lateFee), false));
            }

            chargesTable.addCell(createHeaderCell("Current Bill Amount"));
            chargesTable.addCell(createHeaderCell(formatAmount(bill.getTotalAmount())));

            if (bill.getPreviousDue().compareTo(BigDecimal.ZERO) > 0) {
                chargesTable.addCell(createCell("Previous Due", false));
                chargesTable.addCell(createCell(formatAmount(bill.getPreviousDue()), false));
            }

            // Show advance payment if available
            Double advancePayment = bill.getAccount().getCustomer().getAdvancePayment();
            if (advancePayment != null && advancePayment > 0) {
                chargesTable.addCell(createCell("Advance Payment Credit", false));
                chargesTable.addCell(createCell("-" + formatAmount(BigDecimal.valueOf(advancePayment)), false));
            }

            chargesTable.addCell(createHeaderCell("NET PAYABLE AMOUNT"));
            chargesTable.addCell(createHeaderCell(formatAmount(bill.getNetPayable())));

            document.add(chargesTable);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Pay before " + formatDate(bill.getDueDate()) + " to avoid late fees.")
                    .setItalic()
                    .setFontSize(10));

    document.add(new Paragraph("Thank you for choosing VIT EnergySuite.")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20)
                    .setFontSize(10));

            document.close();

            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    private Image loadLogoImage() {
        if (!StringUtils.hasText(logoPath)) {
            return null;
        }

        try (InputStream inputStream = resolveLogoStream()) {
            if (inputStream == null) {
                log.warn("Logo path '{}' could not be resolved; continuing without logo", logoPath);
                return null;
            }
            byte[] bytes = inputStream.readAllBytes();
            ImageData imageData = ImageDataFactory.create(bytes);
            Image image = new Image(imageData);
            image.setAutoScale(false);
            if (image.getImageScaledWidth() > LOGO_MAX_WIDTH) {
                image.scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_WIDTH);
            }
            image.setMarginBottom(0);
            return image;
        } catch (IOException ex) {
            log.warn("Unable to load logo from '{}': {}", logoPath, ex.getMessage());
            return null;
        }
    }

    private InputStream resolveLogoStream() throws IOException {
        if (logoPath.startsWith("classpath:")) {
            String path = logoPath.substring("classpath:".length());
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return resource.getInputStream();
            }
            return null;
        }

        Path filePath = Paths.get(logoPath);
        if (Files.exists(filePath)) {
            return Files.newInputStream(filePath);
        }
        return null;
    }

    private Cell createCell(String content, boolean bold) {
        Cell cell = new Cell().add(new Paragraph(content));
        if (bold) {
            cell.setBold();
        }
        cell.setPadding(5);
        return cell;
    }

    private Cell createHeaderCell(String content) {
        return new Cell()
                .add(new Paragraph(content))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setPadding(5);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
    }
}

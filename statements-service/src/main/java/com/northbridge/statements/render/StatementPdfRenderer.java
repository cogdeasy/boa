package com.northbridge.statements.render;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/** Renders the customer-facing PDF statement (the same layout the print vendor uses). */
@Component
public class StatementPdfRenderer {

    @SuppressWarnings("unchecked")
    public byte[] render(Map<String, Object> statement) {
        PDDocument doc = new PDDocument();
        try {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream out = new PDPageContentStream(doc, page);
            out.beginText();
            out.setFont(PDType1Font.HELVETICA_BOLD, 16);
            out.newLineAtOffset(50, 760);
            out.showText("Northbridge Federal Bank");
            out.setFont(PDType1Font.HELVETICA, 11);
            out.newLineAtOffset(0, -24);
            out.showText("Statement for " + statement.get("nickname") + " " + statement.get("maskedNumber"));
            out.newLineAtOffset(0, -16);
            out.showText("Period " + statement.get("period")
                    + "   Opening " + statement.get("openingBalance")
                    + "   Closing " + statement.get("closingBalance"));
            out.setFont(PDType1Font.COURIER, 9);
            List<Map<String, Object>> lines = (List<Map<String, Object>>) statement.get("lines");
            int printed = 0;
            for (Map<String, Object> line : lines) {
                if (printed++ >= 40) {
                    break;
                }
                out.newLineAtOffset(0, -12);
                out.showText(String.format("%-12s %-36s %12s",
                        String.valueOf(line.get("businessDate")),
                        trim(String.valueOf(line.get("description")), 36),
                        String.valueOf(line.get("amount"))));
            }
            out.endText();
            out.close();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            doc.save(bytes);
            return bytes.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to render statement PDF", e);
        } finally {
            try {
                doc.close();
            } catch (Exception ignored) {
                // nothing useful to do on close failure
            }
        }
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}

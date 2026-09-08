package in.joyway.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import in.joyway.dto.booking.BookingReceiptDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReceiptPdfService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public byte[] render(BookingReceiptDto receipt) {
        try {
            Document document = new Document(PageSize.A4, 48, 48, 56, 56);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph(nullToDash(receipt.brandName()), title));
            document.add(new Paragraph(nullToDash(receipt.title()), heading));
            document.add(new Paragraph(" "));
            document.add(line("Booking reference", receipt.bookingReference(), body));
            document.add(line("Customer", receipt.customerName(), body));
            document.add(line("Package", receipt.packageTitle(), body));
            document.add(line("Destination", receipt.destination(), body));
            document.add(line("Travel dates", formatRange(receipt), body));
            document.add(line("Travellers", String.valueOf(receipt.numberOfTravellers()), body));
            document.add(line("Amount", receipt.currency() + " " + receipt.packageAmount(), body));
            document.add(line("Payment status", receipt.paymentStatus(), body));
            document.add(line("Payment ID", nullToDash(receipt.razorpayPaymentId()), body));
            if (receipt.paymentDate() != null) {
                document.add(line("Payment date", DATE.format(receipt.paymentDate().atZone(ZONE)), body));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph("This receipt confirms payment for a JOYWAY group travel booking.", body));

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Unable to generate receipt PDF.");
        }
    }

    private static Paragraph line(String label, String value, Font font) {
        return new Paragraph(label + ": " + nullToDash(value), font);
    }

    private static String formatRange(BookingReceiptDto receipt) {
        if (receipt.travelDate() == null) {
            return "—";
        }
        String start = DATE.format(receipt.travelDate());
        if (receipt.endDate() == null) {
            return start;
        }
        return start + " – " + DATE.format(receipt.endDate());
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}

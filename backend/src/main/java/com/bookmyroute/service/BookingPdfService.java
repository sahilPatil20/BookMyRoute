package com.bookmyroute.service;

import com.bookmyroute.entity.Booking;
import com.bookmyroute.entity.BookingSeat;
import com.bookmyroute.entity.Payment;
import com.bookmyroute.entity.Schedule;
import com.bookmyroute.exception.BusinessException;
import com.bookmyroute.exception.ResourceNotFoundException;
import com.bookmyroute.repository.BookingRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BookingPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final BookingRepository bookingRepository;

    public BookingPdfService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(String bookingRef, String userEmail) {
        Booking booking = bookingRepository.findByBookingRefWithTicketDetails(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("Access denied to this booking");
        }

        return generateTicketPdf(booking);
    }

    public byte[] generateTicketPdf(Booking booking) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawTicket(content, booking);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException | IllegalArgumentException ex) {
            throw new BusinessException("Could not generate booking PDF");
        }
    }

    private void drawTicket(PDPageContentStream content, Booking booking) throws IOException {
        float margin = 48;
        float width = PDRectangle.A4.getWidth();
        float y = 780;

        content.setNonStrokingColor(23, 32, 51);
        content.addRect(0, 742, width, 100);
        content.fill();

        content.setNonStrokingColor(255, 255, 255);
        writeText(content, PDType1Font.HELVETICA_BOLD, 26, margin, 790, "BookMyRoute");
        writeText(content, PDType1Font.HELVETICA, 11, margin, 770, "Booking confirmation ticket");
        writeText(content, PDType1Font.HELVETICA_BOLD, 12, 395, 790, "Booking Ref");
        writeText(content, PDType1Font.HELVETICA, 12, 395, 770, safe(booking.getBookingRef()));

        content.setNonStrokingColor(255, 255, 255);
        content.addRect(margin, 704, width - (margin * 2), 32);
        content.fill();
        content.setStrokingColor(216, 78, 85);
        content.addRect(margin, 704, width - (margin * 2), 32);
        content.stroke();
        content.setNonStrokingColor(216, 78, 85);
        writeText(content, PDType1Font.HELVETICA_BOLD, 14, margin + 14, 714,
                booking.getSchedule().getRoute().getOrigin() + "  to  " + booking.getSchedule().getRoute().getDestination());

        y = 665;
        sectionTitle(content, margin, y, "Journey Details");
        y -= 28;

        Schedule schedule = booking.getSchedule();
        drawKeyValue(content, margin, y, "Bus", schedule.getBus().getBusName());
        drawKeyValue(content, 325, y, "Status", enumValue(booking.getStatus()));
        y -= 28;
        drawKeyValue(content, margin, y, "Departure", formatDateTime(schedule.getDepartureTime()));
        drawKeyValue(content, 325, y, "Arrival", formatDateTime(schedule.getArrivalTime()));
        y -= 28;
        drawKeyValue(content, margin, y, "Booked At", formatDateTime(booking.getBookedAt()));

        Payment payment = booking.getPayment();
        drawKeyValue(content, 325, y, "Payment",
                (payment == null ? "--" : enumValue(payment.getPaymentMethod()) + " / " + enumValue(payment.getStatus())));

        y -= 55;
        sectionTitle(content, margin, y, "Passengers");
        y -= 24;
        drawPassengerTable(content, booking, margin, y);

        float totalY = y - 32 - (booking.getBookingSeats().size() * 24);
        content.setNonStrokingColor(255, 248, 235);
        content.addRect(margin, totalY - 10, width - (margin * 2), 42);
        content.fill();
        content.setNonStrokingColor(23, 32, 51);
        writeText(content, PDType1Font.HELVETICA_BOLD, 13, margin + 14, totalY + 6, "Total Amount");
        writeText(content, PDType1Font.HELVETICA_BOLD, 18, 420, totalY + 3, money(booking.getTotalAmount()));

        content.setNonStrokingColor(100, 116, 139);
        writeText(content, PDType1Font.HELVETICA, 9, margin, 58,
                "Please carry a valid ID proof. This ticket is valid only for the passenger and journey shown above.");
    }

    private void drawPassengerTable(PDPageContentStream content, Booking booking, float x, float y) throws IOException {
        float[] colX = {x, x + 72, x + 150, x + 330, x + 395};

        content.setNonStrokingColor(23, 32, 51);
        content.addRect(x, y - 8, 500, 24);
        content.fill();

        content.setNonStrokingColor(255, 255, 255);
        writeText(content, PDType1Font.HELVETICA_BOLD, 10, colX[0] + 8, y, "Seat");
        writeText(content, PDType1Font.HELVETICA_BOLD, 10, colX[1] + 8, y, "Type");
        writeText(content, PDType1Font.HELVETICA_BOLD, 10, colX[2] + 8, y, "Passenger");
        writeText(content, PDType1Font.HELVETICA_BOLD, 10, colX[3] + 8, y, "Age");
        writeText(content, PDType1Font.HELVETICA_BOLD, 10, colX[4] + 8, y, "Fare");

        float rowY = y - 24;
        for (BookingSeat seat : booking.getBookingSeats()) {
            content.setNonStrokingColor(248, 250, 252);
            content.addRect(x, rowY - 8, 500, 22);
            content.fill();

            content.setNonStrokingColor(23, 32, 51);
            writeText(content, PDType1Font.HELVETICA, 10, colX[0] + 8, rowY, safe(seat.getSeat().getSeatNumber()));
            writeText(content, PDType1Font.HELVETICA, 10, colX[1] + 8, rowY, enumValue(seat.getSeat().getSeatType()));
            writeText(content, PDType1Font.HELVETICA, 10, colX[2] + 8, rowY, safe(seat.getPassengerName()));
            writeText(content, PDType1Font.HELVETICA, 10, colX[3] + 8, rowY, String.valueOf(seat.getPassengerAge()));
            writeText(content, PDType1Font.HELVETICA, 10, colX[4] + 8, rowY, money(seat.getFare()));
            rowY -= 24;
        }
    }

    private void sectionTitle(PDPageContentStream content, float x, float y, String text) throws IOException {
        content.setNonStrokingColor(216, 78, 85);
        writeText(content, PDType1Font.HELVETICA_BOLD, 13, x, y, text);
    }

    private void drawKeyValue(PDPageContentStream content, float x, float y, String key, String value) throws IOException {
        content.setNonStrokingColor(100, 116, 139);
        writeText(content, PDType1Font.HELVETICA_BOLD, 9, x, y + 12, key.toUpperCase());
        content.setNonStrokingColor(23, 32, 51);
        writeText(content, PDType1Font.HELVETICA, 11, x, y, safe(value));
    }

    private void writeText(PDPageContentStream content, PDType1Font font, float size, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(text));
        content.endText();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "--" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String money(BigDecimal amount) {
        return "Rs " + (amount == null ? "0.00" : amount.toPlainString());
    }

    private String enumValue(Enum<?> value) {
        return value == null ? "--" : value.name();
    }

    private String safe(String value) {
        return value == null ? "--" : value.replaceAll("[\\r\\n\\t]", " ").replaceAll("[^\\x20-\\x7E]", "?");
    }
}

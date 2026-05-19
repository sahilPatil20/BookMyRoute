package com.bookmyroute.controller;

import com.bookmyroute.dto.request.BookingRequest;
import com.bookmyroute.dto.response.ApiResponse;
import com.bookmyroute.dto.response.BookingResponse;
import com.bookmyroute.service.BookingPdfService;
import com.bookmyroute.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingPdfService bookingPdfService;

    public BookingController(BookingService bookingService, BookingPdfService bookingPdfService) {
        this.bookingService = bookingService;
        this.bookingPdfService = bookingPdfService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.createBooking(request, userDetails.getUsername());
        String message = Boolean.TRUE.equals(response.getNotificationEmailSent())
                ? "Booking confirmed and confirmation email sent"
                : "Booking confirmed. Confirmation email was not sent: " + response.getNotificationEmailMessage();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, message));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getMyBookings(userDetails.getUsername())));
    }

    @GetMapping("/{bookingRef}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingRef,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingByRef(bookingRef, userDetails.getUsername())));
    }

    @GetMapping("/{bookingRef}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @PathVariable String bookingRef,
            @AuthenticationPrincipal UserDetails userDetails) {
        byte[] pdf = bookingPdfService.generateTicketPdf(bookingRef, userDetails.getUsername());
        String filename = "BookMyRoute-" + bookingRef + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }

    // Changed from @PostMapping to @PatchMapping to match REST conventions
    // and the React frontend's bookingApi.cancelBooking which uses PATCH
    @PatchMapping("/{bookingRef}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingRef,
            @AuthenticationPrincipal UserDetails userDetails) {
        BookingResponse response = bookingService.cancelBooking(bookingRef, userDetails.getUsername());
        String message = Boolean.TRUE.equals(response.getNotificationEmailSent())
                ? "Booking cancelled, refund initiated, and cancellation email sent"
                : "Booking cancelled and refund initiated. Cancellation email was not sent: "
                        + response.getNotificationEmailMessage();
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getAllBookings()));
    }
}

package com.bookmyroute.service.impl;

import com.bookmyroute.dto.request.BookingRequest;
import com.bookmyroute.dto.response.BookingResponse;
import com.bookmyroute.dto.response.EmailDeliveryResponse;
import com.bookmyroute.entity.*;
import com.bookmyroute.enums.BookingStatus;
import com.bookmyroute.enums.PaymentStatus;
import com.bookmyroute.exception.BusinessException;
import com.bookmyroute.exception.ResourceNotFoundException;
import com.bookmyroute.repository.*;
import com.bookmyroute.service.BookingService;
import com.bookmyroute.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingServiceImpl implements BookingService {
    private static final AtomicLong SEQ = new AtomicLong(1);

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ScheduleRepository scheduleRepository,
                              SeatRepository seatRepository,
                              UserRepository userRepository,
                              PaymentRepository paymentRepository,
                              EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }


    private final BookingRepository bookingRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", request.getScheduleId()));

        if (!schedule.getIsActive()) {
            throw new BusinessException("Schedule is no longer active");
        }
        if (schedule.getAvailableSeats() < request.getPassengers().size()) {
            throw new BusinessException("Not enough seats available");
        }

        // Build booking seats & calculate total
        List<BookingSeat> bookingSeats = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (BookingRequest.PassengerSeat ps : request.getPassengers()) {
            Seat seat = seatRepository.findById(ps.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat", ps.getSeatId()));

            BookingSeat bs = BookingSeat.builder()
                    .seat(seat)
                    .passengerName(ps.getPassengerName())
                    .passengerAge(ps.getPassengerAge())
                    .fare(schedule.getBaseFare())
                    .build();
            bookingSeats.add(bs);
            total = total.add(schedule.getBaseFare());
        }

        Booking booking = Booking.builder()
                .user(user)
                .schedule(schedule)
                .bookingRef(generateRef())
                .totalAmount(total)
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingSeats.forEach(bs -> bs.setBooking(booking));
        booking.setBookingSeats(bookingSeats);

        // Payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .paymentMethod(request.getPaymentMethod())
                .amount(total)
                .status(PaymentStatus.SUCCESS)
                .paidAt(LocalDateTime.now())
                .build();
        booking.setPayment(payment);

        // Decrement available seats
        schedule.setAvailableSeats(schedule.getAvailableSeats() - request.getPassengers().size());
        scheduleRepository.save(schedule);

        Booking saved = bookingRepository.save(booking);
        EmailDeliveryResponse emailDelivery = emailService.sendBookingConfirmation(saved);
        return toResponse(saved, emailDelivery);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByRef(String bookingRef, String userEmail) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));
        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("Access denied to this booking");
        }
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findAllByUserId(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingRef, String userEmail) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException("Access denied to this booking");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        // Restore seats
        Schedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + booking.getBookingSeats().size());
        scheduleRepository.save(schedule);

        // Initiate refund
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
        }

        Booking saved = bookingRepository.save(booking);
        EmailDeliveryResponse emailDelivery = emailService.sendBookingCancellation(saved);

        return toResponse(saved, emailDelivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String generateRef() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "BMR-" + date + "-" + String.format("%05d", SEQ.getAndIncrement());
    }

    private BookingResponse toResponse(Booking b) {
        return toResponse(b, null);
    }

    private BookingResponse toResponse(Booking b, EmailDeliveryResponse emailDelivery) {
        List<BookingResponse.SeatDetail> seats = b.getBookingSeats().stream()
                .map(bs -> BookingResponse.SeatDetail.builder()
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatType(bs.getSeat().getSeatType())
                        .passengerName(bs.getPassengerName())
                        .passengerAge(bs.getPassengerAge())
                        .fare(bs.getFare())
                        .build())
                .toList();

        Payment pay = b.getPayment();

        return BookingResponse.builder()
                .bookingId(b.getId())
                .bookingRef(b.getBookingRef())
                .customerName(b.getUser().getName())
                .customerEmail(b.getUser().getEmail())
                .origin(b.getSchedule().getRoute().getOrigin())
                .destination(b.getSchedule().getRoute().getDestination())
                .departureTime(b.getSchedule().getDepartureTime())
                .arrivalTime(b.getSchedule().getArrivalTime())
                .busName(b.getSchedule().getBus().getBusName())
                .totalAmount(b.getTotalAmount())
                .bookingStatus(b.getStatus())
                .paymentStatus(pay != null ? pay.getStatus() : null)
                .paymentMethod(pay != null ? pay.getPaymentMethod() : null)
                .bookedAt(b.getBookedAt())
                .seats(seats)
                .notificationEmailSent(emailDelivery != null ? emailDelivery.isSent() : null)
                .notificationEmailMessage(emailDelivery != null ? emailDelivery.getMessage() : null)
                .build();
    }
}

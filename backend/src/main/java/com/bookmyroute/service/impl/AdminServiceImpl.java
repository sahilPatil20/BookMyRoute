package com.bookmyroute.service.impl;

import com.bookmyroute.dto.request.AdminRouteRequest;
import com.bookmyroute.dto.request.AdminScheduleRequest;
import com.bookmyroute.dto.request.AdminUserUpdateRequest;
import com.bookmyroute.dto.response.AdminBusResponse;
import com.bookmyroute.dto.response.AdminDashboardResponse;
import com.bookmyroute.dto.response.AdminRouteResponse;
import com.bookmyroute.dto.response.AdminScheduleResponse;
import com.bookmyroute.dto.response.AdminUserResponse;
import com.bookmyroute.dto.response.BookingResponse;
import com.bookmyroute.dto.response.EmailDeliveryResponse;
import com.bookmyroute.entity.Bus;
import com.bookmyroute.entity.Booking;
import com.bookmyroute.entity.Payment;
import com.bookmyroute.entity.Route;
import com.bookmyroute.entity.Schedule;
import com.bookmyroute.entity.User;
import com.bookmyroute.enums.BookingStatus;
import com.bookmyroute.enums.PaymentStatus;
import com.bookmyroute.enums.Role;
import com.bookmyroute.exception.BusinessException;
import com.bookmyroute.exception.ResourceNotFoundException;
import com.bookmyroute.repository.BookingRepository;
import com.bookmyroute.repository.BusRepository;
import com.bookmyroute.repository.PaymentRepository;
import com.bookmyroute.repository.RouteRepository;
import com.bookmyroute.repository.ScheduleRepository;
import com.bookmyroute.repository.UserRepository;
import com.bookmyroute.service.AdminService;
import com.bookmyroute.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmailService emailService;

    public AdminServiceImpl(UserRepository userRepository,
                            BookingRepository bookingRepository,
                            PaymentRepository paymentRepository,
                            BusRepository busRepository,
                            RouteRepository routeRepository,
                            ScheduleRepository scheduleRepository,
                            EmailService emailService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.busRepository = busRepository;
        this.routeRepository = routeRepository;
        this.scheduleRepository = scheduleRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setTotalUsers(userRepository.count());
        response.setActiveUsers(userRepository.countByIsActiveTrue());
        response.setTotalBookings(bookingRepository.count());
        response.setConfirmedBookings(bookingRepository.findAllByStatus(BookingStatus.CONFIRMED).size());
        response.setCancelledBookings(bookingRepository.findAllByStatus(BookingStatus.CANCELLED).size());
        response.setActiveBuses(busRepository.countByIsActiveTrue());
        response.setTotalRoutes(routeRepository.count());
        response.setActiveSchedules(scheduleRepository.countByIsActiveTrue());
        response.setTotalRevenue(calculateRevenue());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBusResponse> getBuses(Boolean active) {
        return busRepository.findAll().stream()
                .filter(bus -> active == null || bus.getIsActive().equals(active))
                .map(this::toBusResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRouteResponse> getRoutes() {
        return routeRepository.findAll().stream()
                .map(this::toRouteResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminRouteResponse createRoute(AdminRouteRequest request) {
        Route route = Route.builder()
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .distanceKm(request.getDistanceKm())
                .durationMins(request.getDurationMins())
                .build();
        return toRouteResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public AdminRouteResponse updateRoute(Long routeId, AdminRouteRequest request) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
        route.setOrigin(request.getOrigin());
        route.setDestination(request.getDestination());
        route.setDistanceKm(request.getDistanceKm());
        route.setDurationMins(request.getDurationMins());
        return toRouteResponse(routeRepository.save(route));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminScheduleResponse> getSchedules(Boolean active) {
        return scheduleRepository.findAdminSchedules(active).stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminScheduleResponse createSchedule(AdminScheduleRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.getBusId()));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", request.getRouteId()));
        validateScheduleRequest(request, bus);

        Schedule schedule = Schedule.builder()
                .bus(bus)
                .route(route)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .baseFare(request.getBaseFare())
                .availableSeats(request.getAvailableSeats())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public AdminScheduleResponse updateSchedule(Long scheduleId, AdminScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", scheduleId));
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.getBusId()));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", request.getRouteId()));
        validateScheduleRequest(request, bus);

        schedule.setBus(bus);
        schedule.setRoute(route);
        schedule.setDepartureTime(request.getDepartureTime());
        schedule.setArrivalTime(request.getArrivalTime());
        schedule.setBaseFare(request.getBaseFare());
        schedule.setAvailableSeats(request.getAvailableSeats());
        schedule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return toScheduleResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers(Role role, Boolean active) {
        return userRepository.findAll().stream()
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> active == null || user.getIsActive().equals(active))
                .map(this::toUserResponse)
                .toList();
    }

    @Override
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(BookingStatus status, Long userId, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("from must be before to");
        }
        return bookingRepository.findAdminBookings(status, userId, from, to).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingRef) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingRef));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Completed bookings cannot be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Schedule schedule = booking.getSchedule();
        schedule.setAvailableSeats(schedule.getAvailableSeats() + booking.getBookingSeats().size());
        scheduleRepository.save(schedule);

        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
        }

        Booking saved = bookingRepository.save(booking);
        EmailDeliveryResponse emailDelivery = emailService.sendBookingCancellation(saved);

        return toBookingResponse(saved, emailDelivery);
    }

    private BigDecimal calculateRevenue() {
        return paymentRepository.findAllByStatus(PaymentStatus.SUCCESS).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateScheduleRequest(AdminScheduleRequest request, Bus bus) {
        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessException("Arrival time must be after departure time");
        }
        if (request.getAvailableSeats() > bus.getTotalSeats()) {
            throw new BusinessException("Available seats cannot exceed bus total seats");
        }
    }

    private AdminBusResponse toBusResponse(Bus bus) {
        AdminBusResponse response = new AdminBusResponse();
        response.setBusId(bus.getId());
        response.setBusNumber(bus.getBusNumber());
        response.setBusName(bus.getBusName());
        response.setBusType(bus.getBusType());
        response.setTotalSeats(bus.getTotalSeats());
        response.setAmenities(bus.getAmenities());
        response.setIsActive(bus.getIsActive());
        return response;
    }

    private AdminRouteResponse toRouteResponse(Route route) {
        AdminRouteResponse response = new AdminRouteResponse();
        response.setRouteId(route.getId());
        response.setOrigin(route.getOrigin());
        response.setDestination(route.getDestination());
        response.setDistanceKm(route.getDistanceKm());
        response.setDurationMins(route.getDurationMins());
        return response;
    }

    private AdminScheduleResponse toScheduleResponse(Schedule schedule) {
        AdminScheduleResponse response = new AdminScheduleResponse();
        response.setScheduleId(schedule.getId());
        response.setBusId(schedule.getBus().getId());
        response.setBusNumber(schedule.getBus().getBusNumber());
        response.setBusName(schedule.getBus().getBusName());
        response.setBusType(schedule.getBus().getBusType());
        response.setRouteId(schedule.getRoute().getId());
        response.setOrigin(schedule.getRoute().getOrigin());
        response.setDestination(schedule.getRoute().getDestination());
        response.setDepartureTime(schedule.getDepartureTime());
        response.setArrivalTime(schedule.getArrivalTime());
        response.setBaseFare(schedule.getBaseFare());
        response.setAvailableSeats(schedule.getAvailableSeats());
        response.setIsActive(schedule.getIsActive());
        return response;
    }

    private AdminUserResponse toUserResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setUserId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setIsActive(user.getIsActive());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return toBookingResponse(booking, null);
    }

    private BookingResponse toBookingResponse(Booking booking, EmailDeliveryResponse emailDelivery) {
        Payment payment = booking.getPayment();
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingRef(booking.getBookingRef())
                .customerName(booking.getUser().getName())
                .customerEmail(booking.getUser().getEmail())
                .origin(booking.getSchedule().getRoute().getOrigin())
                .destination(booking.getSchedule().getRoute().getDestination())
                .departureTime(booking.getSchedule().getDepartureTime())
                .arrivalTime(booking.getSchedule().getArrivalTime())
                .busName(booking.getSchedule().getBus().getBusName())
                .totalAmount(booking.getTotalAmount())
                .bookingStatus(booking.getStatus())
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .bookedAt(booking.getBookedAt())
                .seats(booking.getBookingSeats().stream()
                        .map(seat -> BookingResponse.SeatDetail.builder()
                                .seatNumber(seat.getSeat().getSeatNumber())
                                .seatType(seat.getSeat().getSeatType())
                                .passengerName(seat.getPassengerName())
                                .passengerAge(seat.getPassengerAge())
                                .fare(seat.getFare())
                                .build())
                        .toList())
                .notificationEmailSent(emailDelivery != null ? emailDelivery.isSent() : null)
                .notificationEmailMessage(emailDelivery != null ? emailDelivery.getMessage() : null)
                .build();
    }
}

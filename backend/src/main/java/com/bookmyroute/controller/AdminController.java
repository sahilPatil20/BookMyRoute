package com.bookmyroute.controller;

import com.bookmyroute.dto.request.AdminRouteRequest;
import com.bookmyroute.dto.request.AdminScheduleRequest;
import com.bookmyroute.dto.request.AdminUserUpdateRequest;
import com.bookmyroute.dto.response.AdminBusResponse;
import com.bookmyroute.dto.response.AdminDashboardResponse;
import com.bookmyroute.dto.response.AdminRouteResponse;
import com.bookmyroute.dto.response.AdminScheduleResponse;
import com.bookmyroute.dto.response.AdminUserResponse;
import com.bookmyroute.dto.response.ApiResponse;
import com.bookmyroute.dto.response.BookingResponse;
import com.bookmyroute.dto.response.EmailDeliveryResponse;
import com.bookmyroute.enums.BookingStatus;
import com.bookmyroute.enums.Role;
import com.bookmyroute.service.AdminService;
import com.bookmyroute.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final EmailService emailService;

    public AdminController(AdminService adminService, EmailService emailService) {
        this.adminService = adminService;
        this.emailService = emailService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboard()));
    }

    @GetMapping("/buses")
    public ResponseEntity<ApiResponse<List<AdminBusResponse>>> buses(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getBuses(active)));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<AdminRouteResponse>>> routes() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getRoutes()));
    }

    @PostMapping("/routes")
    public ResponseEntity<ApiResponse<AdminRouteResponse>> createRoute(
            @Valid @RequestBody AdminRouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createRoute(request), "Route created"));
    }

    @PutMapping("/routes/{routeId}")
    public ResponseEntity<ApiResponse<AdminRouteResponse>> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody AdminRouteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateRoute(routeId, request), "Route updated"));
    }

    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<AdminScheduleResponse>>> schedules(
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSchedules(active)));
    }

    @PostMapping("/schedules")
    public ResponseEntity<ApiResponse<AdminScheduleResponse>> createSchedule(
            @Valid @RequestBody AdminScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createSchedule(request), "Schedule created"));
    }

    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<AdminScheduleResponse>> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody AdminScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateSchedule(scheduleId, request), "Schedule updated"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> users(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsers(role, active)));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateUser(userId, request), "User updated"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getBookings(status, userId, from, to)));
    }

    @PatchMapping("/bookings/{bookingRef}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable String bookingRef) {
        BookingResponse response = adminService.cancelBooking(bookingRef);
        String message = Boolean.TRUE.equals(response.getNotificationEmailSent())
                ? "Booking cancelled, refund initiated, and cancellation email sent"
                : "Booking cancelled and refund initiated. Cancellation email was not sent: "
                        + response.getNotificationEmailMessage();
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/email/test")
    public ResponseEntity<ApiResponse<EmailDeliveryResponse>> sendTestEmail(@RequestParam String to) {
        EmailDeliveryResponse delivery = emailService.sendTestEmail(to);
        String message = delivery.isSent() ? "Test email sent" : delivery.getMessage();
        return ResponseEntity.ok(ApiResponse.success(delivery, message));
    }
}

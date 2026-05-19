package com.bookmyroute.dto.response;

import com.bookmyroute.enums.BookingStatus;
import com.bookmyroute.enums.PaymentMethod;
import com.bookmyroute.enums.PaymentStatus;
import com.bookmyroute.enums.SeatType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BookingResponse {
    private Long bookingId;
    private String bookingRef;
    private String customerName;
    private String customerEmail;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String busName;
    private BigDecimal totalAmount;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private LocalDateTime bookedAt;
    private List<SeatDetail> seats;
    private Boolean notificationEmailSent;
    private String notificationEmailMessage;

    public BookingResponse() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long bookingId;
        private String bookingRef;
        private String customerName;
        private String customerEmail;
        private String origin;
        private String destination;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String busName;
        private BigDecimal totalAmount;
        private BookingStatus bookingStatus;
        private PaymentStatus paymentStatus;
        private PaymentMethod paymentMethod;
        private LocalDateTime bookedAt;
        private List<SeatDetail> seats;
        private Boolean notificationEmailSent;
        private String notificationEmailMessage;

        public Builder bookingId(Long bookingId) { this.bookingId = bookingId; return this; }
        public Builder bookingRef(String bookingRef) { this.bookingRef = bookingRef; return this; }
        public Builder customerName(String customerName) { this.customerName = customerName; return this; }
        public Builder customerEmail(String customerEmail) { this.customerEmail = customerEmail; return this; }
        public Builder origin(String origin) { this.origin = origin; return this; }
        public Builder destination(String destination) { this.destination = destination; return this; }
        public Builder departureTime(LocalDateTime departureTime) { this.departureTime = departureTime; return this; }
        public Builder arrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; return this; }
        public Builder busName(String busName) { this.busName = busName; return this; }
        public Builder totalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; return this; }
        public Builder bookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; return this; }
        public Builder paymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public Builder paymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder bookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; return this; }
        public Builder seats(List<SeatDetail> seats) { this.seats = seats; return this; }
        public Builder notificationEmailSent(Boolean notificationEmailSent) { this.notificationEmailSent = notificationEmailSent; return this; }
        public Builder notificationEmailMessage(String notificationEmailMessage) { this.notificationEmailMessage = notificationEmailMessage; return this; }

        public BookingResponse build() {
            BookingResponse r = new BookingResponse();
            r.bookingId = this.bookingId; r.bookingRef = this.bookingRef;
            r.customerName = this.customerName; r.customerEmail = this.customerEmail;
            r.origin = this.origin; r.destination = this.destination;
            r.departureTime = this.departureTime; r.arrivalTime = this.arrivalTime;
            r.busName = this.busName; r.totalAmount = this.totalAmount;
            r.bookingStatus = this.bookingStatus; r.paymentStatus = this.paymentStatus;
            r.paymentMethod = this.paymentMethod; r.bookedAt = this.bookedAt; r.seats = this.seats;
            r.notificationEmailSent = this.notificationEmailSent;
            r.notificationEmailMessage = this.notificationEmailMessage;
            return r;
        }
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getBookingRef() { return bookingRef; }
    public void setBookingRef(String bookingRef) { this.bookingRef = bookingRef; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public String getBusName() { return busName; }
    public void setBusName(String busName) { this.busName = busName; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public List<SeatDetail> getSeats() { return seats; }
    public void setSeats(List<SeatDetail> seats) { this.seats = seats; }
    public Boolean getNotificationEmailSent() { return notificationEmailSent; }
    public void setNotificationEmailSent(Boolean notificationEmailSent) { this.notificationEmailSent = notificationEmailSent; }
    public String getNotificationEmailMessage() { return notificationEmailMessage; }
    public void setNotificationEmailMessage(String notificationEmailMessage) { this.notificationEmailMessage = notificationEmailMessage; }

    public static class SeatDetail {
        private String seatNumber;
        private SeatType seatType;
        private String passengerName;
        private Integer passengerAge;
        private BigDecimal fare;

        public SeatDetail() {}

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private String seatNumber;
            private SeatType seatType;
            private String passengerName;
            private Integer passengerAge;
            private BigDecimal fare;

            public Builder seatNumber(String seatNumber) { this.seatNumber = seatNumber; return this; }
            public Builder seatType(SeatType seatType) { this.seatType = seatType; return this; }
            public Builder passengerName(String passengerName) { this.passengerName = passengerName; return this; }
            public Builder passengerAge(Integer passengerAge) { this.passengerAge = passengerAge; return this; }
            public Builder fare(BigDecimal fare) { this.fare = fare; return this; }

            public SeatDetail build() {
                SeatDetail sd = new SeatDetail();
                sd.seatNumber = this.seatNumber; sd.seatType = this.seatType;
                sd.passengerName = this.passengerName; sd.passengerAge = this.passengerAge;
                sd.fare = this.fare;
                return sd;
            }
        }

        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
        public SeatType getSeatType() { return seatType; }
        public void setSeatType(SeatType seatType) { this.seatType = seatType; }
        public String getPassengerName() { return passengerName; }
        public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
        public Integer getPassengerAge() { return passengerAge; }
        public void setPassengerAge(Integer passengerAge) { this.passengerAge = passengerAge; }
        public BigDecimal getFare() { return fare; }
        public void setFare(BigDecimal fare) { this.fare = fare; }
    }
}

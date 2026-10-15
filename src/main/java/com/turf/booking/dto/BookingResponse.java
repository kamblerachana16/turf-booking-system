package com.turf.booking.dto;

import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        Long courtId,
        String courtName,
        Long customerId,
        String customerName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        BookingStatus status,
        double totalPrice
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getCourt().getId(),
                b.getCourt().getName(),
                b.getCustomer().getId(),
                b.getCustomer().getName(),
                b.getBookingDate(),
                b.getStartTime(),
                b.getEndTime(),
                b.getStatus(),
                b.getTotalPrice());
    }
}

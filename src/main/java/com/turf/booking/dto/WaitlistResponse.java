package com.turf.booking.dto;

import com.turf.booking.model.WaitlistEntry;
import com.turf.booking.model.WaitlistStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record WaitlistResponse(
        Long id,
        Long courtId,
        String courtName,
        Long customerId,
        String customerName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        WaitlistStatus status,
        LocalDateTime createdAt
) {
    public static WaitlistResponse from(WaitlistEntry e) {
        return new WaitlistResponse(
                e.getId(),
                e.getCourt().getId(),
                e.getCourt().getName(),
                e.getCustomer().getId(),
                e.getCustomer().getName(),
                e.getBookingDate(),
                e.getStartTime(),
                e.getEndTime(),
                e.getStatus(),
                e.getCreatedAt());
    }
}

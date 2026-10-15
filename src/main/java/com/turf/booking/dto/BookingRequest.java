package com.turf.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Used for both "create booking" and "join waiting list". Date format: yyyy-MM-dd, time format: HH:mm */
public record BookingRequest(
        @NotNull(message = "courtId is required") Long courtId,
        @NotNull(message = "customerId is required") Long customerId,
        @NotNull(message = "date is required") @FutureOrPresent(message = "date cannot be in the past") LocalDate date,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime
) {
}

package com.turf.booking.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleRequest(
        @NotNull(message = "date is required") @FutureOrPresent(message = "date cannot be in the past") LocalDate date,
        @NotNull(message = "startTime is required") LocalTime startTime,
        @NotNull(message = "endTime is required") LocalTime endTime
) {
}

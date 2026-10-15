package com.turf.booking.service;

import com.turf.booking.exception.InvalidBookingException;
import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;
import com.turf.booking.model.Court;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Core scheduling logic (no database access, so it is easy to unit test).
 *
 * DSA used here: a TreeMap (sorted by start time) to detect overlapping bookings in O(log n)
 * per lookup, instead of scanning every booking.
 */
@Service
public class SchedulingService {

    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(23, 0);
    public static final long MIN_DURATION_MINUTES = 30;

    /** Checks the basic rules for a time range. Throws InvalidBookingException if something is wrong. */
    public void validateTimes(LocalDate date, LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidBookingException("End time must be after start time");
        }
        if (Duration.between(start, end).toMinutes() < MIN_DURATION_MINUTES) {
            throw new InvalidBookingException("Minimum booking duration is " + MIN_DURATION_MINUTES + " minutes");
        }
        if (start.isBefore(OPENING_TIME) || end.isAfter(CLOSING_TIME)) {
            throw new InvalidBookingException(
                    "Bookings must be between " + OPENING_TIME + " and " + CLOSING_TIME);
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new InvalidBookingException("Date cannot be in the past");
        }
        if (date.equals(today) && start.isBefore(LocalTime.now())) {
            throw new InvalidBookingException("Start time has already passed today");
        }
    }

    /**
     * Returns true if [newStart, newEnd) overlaps any BOOKED booking in the list.
     *
     * Two ranges overlap when: newStart < existingEnd AND newEnd > existingStart.
     * Back-to-back bookings (10:00-11:00 and 11:00-12:00) do NOT overlap.
     *
     * Because bookings for one court on one day never overlap each other, only two neighbours
     * need to be checked:
     *   - the booking starting at or just before newStart (does it run past newStart?)
     *   - the booking starting at or just after newStart (does it start before newEnd?)
     *
     * @param ignoreBookingId booking to skip (used when rescheduling, so a booking doesn't conflict with itself)
     */
    public boolean hasConflict(List<Booking> bookings, LocalTime newStart, LocalTime newEnd, Long ignoreBookingId) {
        TreeMap<LocalTime, Booking> byStartTime = new TreeMap<>();
        for (Booking b : bookings) {
            if (b.getStatus() != BookingStatus.BOOKED) {
                continue; // cancelled bookings do not block a slot
            }
            if (ignoreBookingId != null && ignoreBookingId.equals(b.getId())) {
                continue;
            }
            byStartTime.put(b.getStartTime(), b);
        }

        Map.Entry<LocalTime, Booking> before = byStartTime.floorEntry(newStart);
        if (before != null && before.getValue().getEndTime().isAfter(newStart)) {
            return true;
        }

        Map.Entry<LocalTime, Booking> after = byStartTime.ceilingEntry(newStart);
        return after != null && after.getKey().isBefore(newEnd);
    }

    /** price = hours * pricePerHour, rounded to 2 decimals */
    public double calculatePrice(Court court, LocalTime start, LocalTime end) {
        double hours = Duration.between(start, end).toMinutes() / 60.0;
        return Math.round(hours * court.getPricePerHour() * 100.0) / 100.0;
    }
}

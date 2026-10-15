package com.turf.booking.service;

import com.turf.booking.exception.InvalidBookingException;
import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;
import com.turf.booking.model.Court;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulingServiceTest {

    private final SchedulingService service = new SchedulingService();

    private Booking booking(Long id, String start, String end, BookingStatus status) {
        Booking b = new Booking();
        b.setId(id);
        b.setStartTime(LocalTime.parse(start));
        b.setEndTime(LocalTime.parse(end));
        b.setStatus(status);
        return b;
    }

    private Booking booked(Long id, String start, String end) {
        return booking(id, start, end, BookingStatus.BOOKED);
    }

    private boolean conflict(List<Booking> existing, String start, String end) {
        return service.hasConflict(existing, LocalTime.parse(start), LocalTime.parse(end), null);
    }

    // ---------- conflict detection ----------

    @Test
    void noExistingBookings_isAllowed() {
        assertFalse(conflict(List.of(), "10:00", "11:00"));
    }

    @Test
    void exactSameSlot_isConflict() {
        assertTrue(conflict(List.of(booked(1L, "10:00", "11:00")), "10:00", "11:00"));
    }

    @Test
    void startsInsideExistingBooking_isConflict() {
        assertTrue(conflict(List.of(booked(1L, "10:00", "12:00")), "11:00", "13:00"));
    }

    @Test
    void endsInsideExistingBooking_isConflict() {
        assertTrue(conflict(List.of(booked(1L, "10:00", "12:00")), "09:00", "11:00"));
    }

    @Test
    void fullyInsideExistingBooking_isConflict() {
        assertTrue(conflict(List.of(booked(1L, "10:00", "13:00")), "11:00", "12:00"));
    }

    @Test
    void wrapsAroundExistingBooking_isConflict() {
        assertTrue(conflict(List.of(booked(1L, "10:00", "11:00")), "09:00", "12:00"));
    }

    @Test
    void backToBackAfterExisting_isAllowed() {
        assertFalse(conflict(List.of(booked(1L, "10:00", "11:00")), "11:00", "12:00"));
    }

    @Test
    void backToBackBeforeExisting_isAllowed() {
        assertFalse(conflict(List.of(booked(1L, "10:00", "11:00")), "09:00", "10:00"));
    }

    @Test
    void cancelledBookingDoesNotBlockSlot() {
        List<Booking> existing = List.of(booking(1L, "10:00", "11:00", BookingStatus.CANCELLED));
        assertFalse(conflict(existing, "10:00", "11:00"));
    }

    @Test
    void ignoringOwnBooking_allowsReschedulingOverlappingItself() {
        List<Booking> existing = List.of(booked(1L, "10:00", "11:00"));
        assertFalse(service.hasConflict(existing, LocalTime.parse("10:30"), LocalTime.parse("11:30"), 1L));
    }

    @Test
    void gapBetweenMultipleBookings_isFoundCorrectly() {
        List<Booking> existing = List.of(
                booked(1L, "08:00", "09:00"),
                booked(2L, "10:00", "11:00"),
                booked(3L, "12:00", "13:00"));
        assertFalse(conflict(existing, "09:00", "10:00")); // fits exactly in the gap
        assertTrue(conflict(existing, "09:00", "10:30"));  // runs into the 10:00 booking
        assertTrue(conflict(existing, "11:30", "12:30"));  // runs into the 12:00 booking
        assertFalse(conflict(existing, "13:00", "14:00")); // after the last booking
    }

    // ---------- price ----------

    @Test
    void priceIsHoursTimesRate() {
        Court court = new Court();
        court.setPricePerHour(1200);
        assertEquals(1800.0, service.calculatePrice(court, LocalTime.of(18, 0), LocalTime.of(19, 30)));
    }

    // ---------- validation ----------

    private LocalDate futureDate() {
        return LocalDate.now().plusDays(5);
    }

    @Test
    void validTimes_passValidation() {
        service.validateTimes(futureDate(), LocalTime.of(18, 0), LocalTime.of(19, 0));
    }

    @Test
    void endBeforeStart_isRejected() {
        assertThrows(InvalidBookingException.class,
                () -> service.validateTimes(futureDate(), LocalTime.of(19, 0), LocalTime.of(18, 0)));
    }

    @Test
    void tooShortBooking_isRejected() {
        assertThrows(InvalidBookingException.class,
                () -> service.validateTimes(futureDate(), LocalTime.of(18, 0), LocalTime.of(18, 15)));
    }

    @Test
    void outsideOpeningHours_isRejected() {
        assertThrows(InvalidBookingException.class,
                () -> service.validateTimes(futureDate(), LocalTime.of(4, 0), LocalTime.of(5, 0)));
        assertThrows(InvalidBookingException.class,
                () -> service.validateTimes(futureDate(), LocalTime.of(22, 0), LocalTime.of(23, 30)));
    }

    @Test
    void pastDate_isRejected() {
        assertThrows(InvalidBookingException.class,
                () -> service.validateTimes(LocalDate.now().minusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0)));
    }
}

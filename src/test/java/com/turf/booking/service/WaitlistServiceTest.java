package com.turf.booking.service;

import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;
import com.turf.booking.model.Court;
import com.turf.booking.model.Customer;
import com.turf.booking.model.WaitlistEntry;
import com.turf.booking.model.WaitlistStatus;
import com.turf.booking.repository.BookingRepository;
import com.turf.booking.repository.CourtRepository;
import com.turf.booking.repository.CustomerRepository;
import com.turf.booking.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock
    private WaitlistRepository waitlistRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CourtRepository courtRepository;
    @Mock
    private CustomerRepository customerRepository;

    private WaitlistService waitlistService;
    private Court court;
    private final LocalDate date = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        waitlistService = new WaitlistService(
                waitlistRepository, bookingRepository, courtRepository, customerRepository, new SchedulingService());
        court = new Court();
        court.setId(1L);
        court.setPricePerHour(1000);
    }

    private WaitlistEntry entry(Long id, String start, String end, LocalDateTime createdAt) {
        WaitlistEntry e = new WaitlistEntry();
        e.setId(id);
        e.setCourt(court);
        e.setCustomer(new Customer());
        e.setBookingDate(date);
        e.setStartTime(LocalTime.parse(start));
        e.setEndTime(LocalTime.parse(end));
        e.setCreatedAt(createdAt);
        e.setStatus(WaitlistStatus.WAITING);
        return e;
    }

    private Booking booked(String start, String end) {
        Booking b = new Booking();
        b.setId(99L);
        b.setStartTime(LocalTime.parse(start));
        b.setEndTime(LocalTime.parse(end));
        b.setStatus(BookingStatus.BOOKED);
        return b;
    }

    @Test
    void earliestWaitingCustomerGetsTheFreedSlot() {
        LocalDateTime now = LocalDateTime.now();
        WaitlistEntry first = entry(1L, "18:00", "19:00", now.minusMinutes(30));
        WaitlistEntry second = entry(2L, "18:00", "19:00", now.minusMinutes(10));

        // list is given in the "wrong" order on purpose, the PriorityQueue must fix the order
        when(waitlistRepository.findByCourtIdAndBookingDateAndStatus(1L, date, WaitlistStatus.WAITING))
                .thenReturn(List.of(second, first));
        when(bookingRepository.findByCourtIdAndBookingDateAndStatus(1L, date, BookingStatus.BOOKED))
                .thenReturn(new ArrayList<>());

        waitlistService.processWaitlist(court, date);

        assertEquals(WaitlistStatus.ASSIGNED, first.getStatus());
        assertEquals(WaitlistStatus.WAITING, second.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void entryThatDoesNotFitStaysWaiting_whileLaterOneThatFitsIsAssigned() {
        LocalDateTime now = LocalDateTime.now();
        WaitlistEntry doesNotFit = entry(1L, "17:30", "18:30", now.minusMinutes(30)); // overlaps the 17:00-18:00 booking
        WaitlistEntry fits = entry(2L, "18:00", "19:00", now.minusMinutes(10));

        when(waitlistRepository.findByCourtIdAndBookingDateAndStatus(1L, date, WaitlistStatus.WAITING))
                .thenReturn(List.of(doesNotFit, fits));
        when(bookingRepository.findByCourtIdAndBookingDateAndStatus(1L, date, BookingStatus.BOOKED))
                .thenReturn(new ArrayList<>(List.of(booked("17:00", "18:00"))));

        waitlistService.processWaitlist(court, date);

        assertEquals(WaitlistStatus.WAITING, doesNotFit.getStatus());
        assertEquals(WaitlistStatus.ASSIGNED, fits.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void emptyWaitingList_createsNoBookings() {
        when(waitlistRepository.findByCourtIdAndBookingDateAndStatus(1L, date, WaitlistStatus.WAITING))
                .thenReturn(new ArrayList<>());

        waitlistService.processWaitlist(court, date);

        verify(bookingRepository, times(0)).save(any(Booking.class));
    }
}

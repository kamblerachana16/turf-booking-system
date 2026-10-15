package com.turf.booking.service;

import com.turf.booking.dto.BookingRequest;
import com.turf.booking.dto.WaitlistResponse;
import com.turf.booking.exception.InvalidBookingException;
import com.turf.booking.exception.ResourceNotFoundException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final CustomerRepository customerRepository;
    private final SchedulingService schedulingService;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           BookingRepository bookingRepository,
                           CourtRepository courtRepository,
                           CustomerRepository customerRepository,
                           SchedulingService schedulingService) {
        this.waitlistRepository = waitlistRepository;
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.customerRepository = customerRepository;
        this.schedulingService = schedulingService;
    }

    /** Join the waiting list for a slot that is currently taken. */
    @Transactional
    public WaitlistResponse join(BookingRequest req) {
        schedulingService.validateTimes(req.date(), req.startTime(), req.endTime());

        Court court = courtRepository.findByIdForUpdate(req.courtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + req.courtId()));
        if (!court.isActive()) {
            throw new InvalidBookingException("This court is not active");
        }
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + req.customerId()));

        List<Booking> booked = bookingRepository.findByCourtIdAndBookingDateAndStatus(
                court.getId(), req.date(), BookingStatus.BOOKED);
        if (!schedulingService.hasConflict(booked, req.startTime(), req.endTime(), null)) {
            throw new InvalidBookingException("This slot is free. Please book it directly instead of joining the waiting list.");
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setCourt(court);
        entry.setCustomer(customer);
        entry.setBookingDate(req.date());
        entry.setStartTime(req.startTime());
        entry.setEndTime(req.endTime());
        entry.setStatus(WaitlistStatus.WAITING);

        return WaitlistResponse.from(waitlistRepository.save(entry));
    }

    public List<WaitlistResponse> list(Long courtId, LocalDate date) {
        return waitlistRepository.findByCourtIdAndBookingDate(courtId, date).stream()
                .map(WaitlistResponse::from)
                .toList();
    }

    /**
     * Called after a booking is cancelled or moved. Tries to give the freed time to waiting customers.
     *
     * DSA used here: a PriorityQueue (min-heap) ordered by "who joined first", so the longest-waiting
     * customer is always served first (poll = O(log n)). Entries that do not fit are skipped
     * and stay on the waiting list.
     */
    @Transactional
    public void processWaitlist(Court court, LocalDate date) {
        List<WaitlistEntry> waiting = waitlistRepository.findByCourtIdAndBookingDateAndStatus(
                court.getId(), date, WaitlistStatus.WAITING);
        if (waiting.isEmpty()) {
            return;
        }

        PriorityQueue<WaitlistEntry> queue = new PriorityQueue<WaitlistEntry>(WaitlistService::compareEntries);
        queue.addAll(waiting);

        List<Booking> booked = new ArrayList<>(bookingRepository.findByCourtIdAndBookingDateAndStatus(
                court.getId(), date, BookingStatus.BOOKED));

        while (!queue.isEmpty()) {
            WaitlistEntry entry = queue.poll();

            if (schedulingService.hasConflict(booked, entry.getStartTime(), entry.getEndTime(), null)) {
                continue; // does not fit (yet), stays WAITING
            }

            Booking booking = new Booking();
            booking.setCourt(court);
            booking.setCustomer(entry.getCustomer());
            booking.setBookingDate(date);
            booking.setStartTime(entry.getStartTime());
            booking.setEndTime(entry.getEndTime());
            booking.setStatus(BookingStatus.BOOKED);
            booking.setTotalPrice(schedulingService.calculatePrice(court, entry.getStartTime(), entry.getEndTime()));
            bookingRepository.save(booking);
            booked.add(booking); // so the next waiting entry is checked against this new booking too

            entry.setStatus(WaitlistStatus.ASSIGNED);
            waitlistRepository.save(entry);
        }
    }

    /** Earliest createdAt first; if equal, lower id first. */
    private static int compareEntries(WaitlistEntry a, WaitlistEntry b) {
        int byTime = a.getCreatedAt().compareTo(b.getCreatedAt());
        if (byTime != 0) {
            return byTime;
        }
        if (a.getId() == null || b.getId() == null) {
            return 0;
        }
        return a.getId().compareTo(b.getId());
    }
}

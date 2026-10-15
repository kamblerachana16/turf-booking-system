package com.turf.booking.service;

import com.turf.booking.dto.BookingRequest;
import com.turf.booking.dto.BookingResponse;
import com.turf.booking.dto.RescheduleRequest;
import com.turf.booking.exception.InvalidBookingException;
import com.turf.booking.exception.ResourceNotFoundException;
import com.turf.booking.exception.SlotConflictException;
import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;
import com.turf.booking.model.Court;
import com.turf.booking.model.Customer;
import com.turf.booking.repository.BookingRepository;
import com.turf.booking.repository.CourtRepository;
import com.turf.booking.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final CustomerRepository customerRepository;
    private final SchedulingService schedulingService;
    private final WaitlistService waitlistService;

    public BookingService(BookingRepository bookingRepository,
                          CourtRepository courtRepository,
                          CustomerRepository customerRepository,
                          SchedulingService schedulingService,
                          WaitlistService waitlistService) {
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.customerRepository = customerRepository;
        this.schedulingService = schedulingService;
        this.waitlistService = waitlistService;
    }

    @Transactional
    public BookingResponse create(BookingRequest req) {
        schedulingService.validateTimes(req.date(), req.startTime(), req.endTime());

        // Lock the court row so two simultaneous requests cannot both pass the conflict check
        Court court = courtRepository.findByIdForUpdate(req.courtId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + req.courtId()));
        if (!court.isActive()) {
            throw new InvalidBookingException("This court is not active");
        }
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + req.customerId()));

        List<Booking> existing = bookingRepository.findByCourtIdAndBookingDateAndStatus(
                court.getId(), req.date(), BookingStatus.BOOKED);
        if (schedulingService.hasConflict(existing, req.startTime(), req.endTime(), null)) {
            throw new SlotConflictException(
                    "This slot is already booked. You can join the waiting list using POST /waitlist");
        }

        Booking booking = new Booking();
        booking.setCourt(court);
        booking.setCustomer(customer);
        booking.setBookingDate(req.date());
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setTotalPrice(schedulingService.calculatePrice(court, req.startTime(), req.endTime()));

        return BookingResponse.from(bookingRepository.save(booking));
    }

    public BookingResponse getById(Long id) {
        return BookingResponse.from(findBooking(id));
    }

    public List<BookingResponse> search(Long customerId, Long courtId, LocalDate date) {
        List<Booking> result;
        if (customerId != null) {
            result = bookingRepository.findByCustomerId(customerId);
        } else if (courtId != null && date != null) {
            result = bookingRepository.findByCourtIdAndBookingDate(courtId, date);
        } else {
            result = bookingRepository.findAll();
        }
        return result.stream().map(BookingResponse::from).toList();
    }

    /** Cancels (soft delete, history is kept) and then offers the freed slot to the waiting list. */
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = findBooking(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking is already cancelled");
        }

        Court court = courtRepository.findByIdForUpdate(booking.getCourt().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        waitlistService.processWaitlist(court, booking.getBookingDate());
        return BookingResponse.from(booking);
    }

    /** Moves a booking to a new date/time. Runs in one transaction, so it is all-or-nothing. */
    @Transactional
    public BookingResponse reschedule(Long id, RescheduleRequest req) {
        Booking booking = findBooking(id);
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new InvalidBookingException("Only active bookings can be rescheduled");
        }
        schedulingService.validateTimes(req.date(), req.startTime(), req.endTime());

        Court court = courtRepository.findByIdForUpdate(booking.getCourt().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Court not found"));

        List<Booking> existing = bookingRepository.findByCourtIdAndBookingDateAndStatus(
                court.getId(), req.date(), BookingStatus.BOOKED);
        // ignore this booking itself, otherwise it would conflict with its own old slot
        if (schedulingService.hasConflict(existing, req.startTime(), req.endTime(), booking.getId())) {
            throw new SlotConflictException("The new slot is already booked");
        }

        LocalDate oldDate = booking.getBookingDate();
        booking.setBookingDate(req.date());
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setTotalPrice(schedulingService.calculatePrice(court, req.startTime(), req.endTime()));
        bookingRepository.save(booking);

        // the old slot is now free, so offer it to the waiting list
        waitlistService.processWaitlist(court, oldDate);
        return BookingResponse.from(booking);
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
    }
}

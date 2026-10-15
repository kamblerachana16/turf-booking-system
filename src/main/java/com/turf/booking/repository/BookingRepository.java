package com.turf.booking.repository;

import com.turf.booking.model.Booking;
import com.turf.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCourtIdAndBookingDateAndStatus(Long courtId, LocalDate bookingDate, BookingStatus status);

    List<Booking> findByCourtIdAndBookingDate(Long courtId, LocalDate bookingDate);

    List<Booking> findByCustomerId(Long customerId);
}

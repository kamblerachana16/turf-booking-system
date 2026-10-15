package com.turf.booking.repository;

import com.turf.booking.model.WaitlistEntry;
import com.turf.booking.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findByCourtIdAndBookingDateAndStatus(Long courtId, LocalDate bookingDate, WaitlistStatus status);

    List<WaitlistEntry> findByCourtIdAndBookingDate(Long courtId, LocalDate bookingDate);
}

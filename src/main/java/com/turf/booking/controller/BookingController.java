package com.turf.booking.controller;

import com.turf.booking.dto.BookingRequest;
import com.turf.booking.dto.BookingResponse;
import com.turf.booking.dto.RescheduleRequest;
import com.turf.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable("id") Long id) {
        return bookingService.getById(id);
    }

    /** GET /bookings?customerId=1   or   GET /bookings?courtId=1&date=2026-10-20   or   GET /bookings (all) */
    @GetMapping
    public List<BookingResponse> search(
            @RequestParam(name = "customerId", required = false) Long customerId,
            @RequestParam(name = "courtId", required = false) Long courtId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.search(customerId, courtId, date);
    }

    @PutMapping("/{id}/reschedule")
    public BookingResponse reschedule(@PathVariable("id") Long id, @Valid @RequestBody RescheduleRequest request) {
        return bookingService.reschedule(id, request);
    }

    /** Cancels the booking (status becomes CANCELLED) and triggers the waiting list. */
    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable("id") Long id) {
        return bookingService.cancel(id);
    }
}

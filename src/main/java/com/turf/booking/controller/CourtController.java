package com.turf.booking.controller;

import com.turf.booking.model.Court;
import com.turf.booking.service.CourtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courts")
public class CourtController {

    private final CourtService courtService;

    public CourtController(CourtService courtService) {
        this.courtService = courtService;
    }

    @PostMapping
    public ResponseEntity<Court> create(@Valid @RequestBody Court court) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courtService.create(court));
    }

    @GetMapping
    public List<Court> getAll() {
        return courtService.getAll();
    }

    @GetMapping("/{id}")
    public Court getById(@PathVariable("id") Long id) {
        return courtService.getById(id);
    }

    @PutMapping("/{id}")
    public Court update(@PathVariable("id") Long id, @Valid @RequestBody Court court) {
        return courtService.update(id, court);
    }
}

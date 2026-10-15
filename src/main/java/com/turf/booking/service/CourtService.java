package com.turf.booking.service;

import com.turf.booking.exception.ResourceNotFoundException;
import com.turf.booking.model.Court;
import com.turf.booking.repository.CourtRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourtService {

    private final CourtRepository courtRepository;

    public CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    public Court create(Court court) {
        court.setId(null); // make sure we always create a new row
        return courtRepository.save(court);
    }

    public List<Court> getAll() {
        return courtRepository.findAll();
    }

    public Court getById(Long id) {
        return courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Court not found with id " + id));
    }

    public Court update(Long id, Court updated) {
        Court court = getById(id);
        court.setName(updated.getName());
        court.setSportType(updated.getSportType());
        court.setPricePerHour(updated.getPricePerHour());
        court.setActive(updated.isActive());
        return courtRepository.save(court);
    }
}

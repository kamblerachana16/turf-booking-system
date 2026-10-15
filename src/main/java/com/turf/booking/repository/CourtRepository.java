package com.turf.booking.repository;

import com.turf.booking.model.Court;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {

    /**
     * Loads the court row with a database lock (SELECT ... FOR UPDATE).
     * Two requests booking the same court at the same moment run one after the other,
     * so the conflict check cannot be fooled by a race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);
}

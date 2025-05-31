package com.example.movieticketsystem.repository;

import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.model.Seat;
import com.example.movieticketsystem.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    List<SeatReservation> findByScreeningId(Long screeningId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SeatReservation> findByScreeningAndSeat(Screening screening, Seat seat);

    boolean existsByScreeningAndSeatAndReserved(Screening screening, Seat seat, boolean reserved);

    List<SeatReservation> findByReservationExpiryLessThan(LocalDateTime dateTime);
}

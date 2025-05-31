package com.example.movieticketsystem.repository;

import com.example.movieticketsystem.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByHallNumber(Integer hallNumber);

    Optional<Seat> findByHallNumberAndRowNumberAndSeatNumber(Integer hallNumber, Integer rowNumber, Integer seatNumber);
}
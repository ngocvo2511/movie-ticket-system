package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.model.Seat;
import com.example.movieticketsystem.model.SeatReservation;
import com.example.movieticketsystem.repository.SeatRepository;
import com.example.movieticketsystem.repository.SeatReservationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Seat> findAllSeats() {
        return seatRepository.findAll();
    }

    public List<Seat> findSeatsByHall(Integer hallNumber) {
        return seatRepository.findByHallNumber(hallNumber);
    }

    public Optional<Seat> findSeatById(Long id) {
        return seatRepository.findById(id);
    }

    @Transactional
    public Seat saveSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    /**
     * Create seats for a specific screening configuration
     */
    @Transactional
    public List<Seat> createSeatsForScreening(int hallNumber, int totalSeats) {
        List<Seat> newSeats = new ArrayList<>();
        final int SEATS_PER_ROW = 10;
        int numberOfRows = (int) Math.ceil((double) totalSeats / SEATS_PER_ROW);

        for (int row = 0; row < numberOfRows; row++) {
            char rowLetter = (char) ('A' + row);
            int seatsInThisRow = (row == numberOfRows - 1 && totalSeats % SEATS_PER_ROW != 0) 
                ? totalSeats % SEATS_PER_ROW 
                : SEATS_PER_ROW;

            for (int seatNum = 1; seatNum <= seatsInThisRow; seatNum++) {
                Seat seat = new Seat();
                seat.setHallNumber(hallNumber);
                seat.setRowNumber(row + 1);
                seat.setSeatNumber(seatNum);
                seat.setSeatName(String.format("%c%d", rowLetter, seatNum));
                newSeats.add(seatRepository.save(seat));
            }
        }
        
        entityManager.flush();
        return newSeats;
    }

    /**
     * Initialize seat reservations for a screening with custom seat layout
     */
    @Transactional
    public void initializeSeatReservations(Screening screening) {
        // Delete existing reservations for this screening only
        entityManager.createNativeQuery("DELETE FROM seat_reservations WHERE screening_id = :screeningId")
                .setParameter("screeningId", screening.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Create new seats for this screening
        List<Seat> seats = createSeatsForScreening(screening.getHallNumber(), screening.getTotalSeats());

        // Create new reservations for each seat
        List<SeatReservation> newReservations = new ArrayList<>();
        for (Seat seat : seats) {
            SeatReservation reservation = new SeatReservation();
            reservation.setScreening(screening);
            reservation.setSeat(seat);
            reservation.setReserved(false);
            reservation.setVersion(0);
            newReservations.add(reservation);
        }

        // Save all reservations in batch
        seatReservationRepository.saveAll(newReservations);
        entityManager.flush();
    }
}

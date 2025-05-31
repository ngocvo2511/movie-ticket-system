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
import java.util.List;
import java.util.Optional;

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
     * Initialize seats for a cinema hall with customizable rows and seats per row
     *
     * @param hallNumber Hall number
     * @param rows Number of rows
     * @param seatsPerRow Number of seats per row
     * @param useLetterRowNames Whether to use letters (A,B,C...) instead of numbers (1,2,3...) for row names
     */
    @Transactional
    public void initializeSeatsForHall(int hallNumber, int rows, int seatsPerRow, boolean useLetterRowNames) {
        // Delete existing seats using a direct query to avoid Hibernate caching issues
        entityManager.createNativeQuery("DELETE FROM seat_reservations WHERE seat_id IN (SELECT id FROM seats WHERE hall_number = :hallNumber)")
                .setParameter("hallNumber", hallNumber)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM seats WHERE hall_number = :hallNumber")
                .setParameter("hallNumber", hallNumber)
                .executeUpdate();

        // Clear the persistence context to avoid stale data
        entityManager.flush();
        entityManager.clear();

        // Create new seats
        List<Seat> newSeats = new ArrayList<>();
        for (int row = 1; row <= rows; row++) {
            for (int seatNum = 1; seatNum <= seatsPerRow; seatNum++) {
                Seat seat = new Seat();
                seat.setHallNumber(hallNumber);
                seat.setRowNumber(row);
                seat.setSeatNumber(seatNum);

                // Set the custom seat name property
                if (useLetterRowNames) {
                    char rowLetter = (char) ('A' + row - 1); // Convert 1 to 'A', 2 to 'B', etc.
                    seat.setSeatName(rowLetter + String.valueOf(seatNum)); // Example: "A1", "B5", etc.
                } else {
                    seat.setSeatName("Row " + row + ", Seat " + seatNum);
                }

                newSeats.add(seatRepository.save(seat));
            }
        }

        entityManager.flush();
    }

    /**
     * Initialize seat reservations for a screening with custom seat layout
     */
    @Transactional
    public void initializeSeatReservations(Screening screening, int rows, int seatsPerRow, boolean useLetterRowNames) {
        int hallNumber = screening.getHallNumber();

        // Create or update hall layout
        initializeSeatsForHall(hallNumber, rows, seatsPerRow, useLetterRowNames);

        // Delete existing reservations if any
        entityManager.createNativeQuery("DELETE FROM seat_reservations WHERE screening_id = :screeningId")
                .setParameter("screeningId", screening.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Reload the screening to avoid cache issues
        Screening refreshedScreening = entityManager.find(Screening.class, screening.getId());

        // Create new reservations for each seat
        List<Seat> seats = seatRepository.findByHallNumber(hallNumber);

        for (Seat seat : seats) {
            SeatReservation reservation = new SeatReservation();
            reservation.setScreening(refreshedScreening);
            reservation.setSeat(seat);
            reservation.setReserved(false);
            reservation.setVersion(0);

            seatReservationRepository.save(reservation);
        }

        entityManager.flush();
    }

    /**
     * Initialize seat reservations with default values
     */
    @Transactional
    public void initializeSeatReservations(Screening screening) {
        // Default to 10 seats per row and 10 rows, with letter row names
        initializeSeatReservations(screening, 10, 10, true);
    }
}

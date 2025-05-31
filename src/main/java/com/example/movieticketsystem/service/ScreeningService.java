package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.repository.MovieRepository;
import com.example.movieticketsystem.repository.ScreeningRepository;
import com.example.movieticketsystem.repository.SeatReservationRepository;
import com.example.movieticketsystem.repository.TicketRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final TicketRepository ticketRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Screening> findAllScreenings() {
        return screeningRepository.findAll();
    }

    public List<Screening> findUpcomingScreenings() {
        return screeningRepository.findByStartTimeAfterOrderByStartTime(LocalDateTime.now());
    }

    public List<Screening> findScreeningsByMovie(Long movieId) {
        return screeningRepository.findByMovieIdAndStartTimeAfterOrderByStartTime(movieId, LocalDateTime.now());
    }

    public Optional<Screening> findScreeningById(Long id) {
        return screeningRepository.findById(id);
    }

    @Transactional
    public Screening saveScreening(Screening screening) {
        // Clear any existing seat reservations from the screening to avoid conflict
        if (screening.getId() != null) {
            screening.setSeatReservations(null);
            entityManager.flush();
        }

        // Save the screening
        return screeningRepository.save(screening);
    }

    /**
     * Safely delete a screening and all associated seat reservations
     */
    @Transactional
    public boolean deleteScreening(Long id) {
        // Check if screening exists
        Optional<Screening> screeningOpt = screeningRepository.findById(id);

        if (screeningOpt.isPresent()) {
            // First, delete any tickets associated with this screening
            entityManager.createNativeQuery("DELETE FROM tickets WHERE screening_id = :screeningId")
                    .setParameter("screeningId", id)
                    .executeUpdate();

            // Then delete seat reservations
            entityManager.createNativeQuery("DELETE FROM seat_reservations WHERE screening_id = :screeningId")
                    .setParameter("screeningId", id)
                    .executeUpdate();

            // Clear the session to avoid cache issues
            entityManager.flush();
            entityManager.clear();

            // Reload the screening
            Screening screening = entityManager.find(Screening.class, id);

            // Delete the screening itself if it still exists
            if (screening != null) {
                entityManager.remove(screening);
                return true;
            }
        }

        return false;
    }
}
package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.model.Ticket;
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
        if (screening.getId() != null) {
            // For existing screening, we need to handle seat reservations properly
            Screening existingScreening = screeningRepository.findById(screening.getId())
                    .orElseThrow(() -> new RuntimeException("Screening not found"));
            
            // Check if there are any active tickets for this screening
            if (ticketRepository.existsByScreeningIdAndStatusNot(existingScreening.getId(), Ticket.TicketStatus.CANCELED)) {
                throw new RuntimeException("Cannot modify screening that has active tickets");
            }
            
            // Clear existing seat reservations
            seatReservationRepository.deleteAll(existingScreening.getSeatReservations());
            
            // Clear the collection to avoid orphan removal issues
            existingScreening.getSeatReservations().clear();
            
            // Update the screening properties
            existingScreening.setMovie(screening.getMovie());
            existingScreening.setStartTime(screening.getStartTime());
            existingScreening.setEndTime(screening.getEndTime());
            existingScreening.setHallNumber(screening.getHallNumber());
            existingScreening.setPrice(screening.getPrice());
            
            // Update total seats if provided
            if (screening.getTotalSeats() != null) {
                existingScreening.setTotalSeats(screening.getTotalSeats());
            }
            
            // Save the updated screening
            return screeningRepository.save(existingScreening);
        }
        
        // For new screening
        return screeningRepository.save(screening);
    }

    /**
     * Check if a screening can be modified (no active tickets)
     */
    public boolean canModifyScreening(Long id) {
        return !ticketRepository.existsByScreeningIdAndStatusNot(id, Ticket.TicketStatus.CANCELED);
    }

    /**
     * Safely delete a screening and all associated seat reservations
     */
    @Transactional
    public boolean deleteScreening(Long id) {
        // Check if screening exists
        Optional<Screening> screeningOpt = screeningRepository.findById(id);

        if (screeningOpt.isPresent()) {
            // Check if there are any active tickets
            if (ticketRepository.existsByScreeningIdAndStatusNot(id, Ticket.TicketStatus.CANCELED)) {
                throw new RuntimeException("Cannot delete screening that has active tickets");
            }

            // Delete seat reservations
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
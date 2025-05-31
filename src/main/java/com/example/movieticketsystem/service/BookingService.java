package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.*;
import com.example.movieticketsystem.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    /**
     * Get all available seats for a screening
     */
    public List<SeatReservation> getAvailableSeats(Long screeningId) {
        return seatReservationRepository.findByScreeningId(screeningId);
    }

    /**
     * Reserve a seat for a screening
     */
    @Transactional
    public boolean reserveSeat(Long screeningId, Long seatId) throws InterruptedException {
        Optional<Screening> screeningOpt = screeningRepository.findById(screeningId);
        Optional<Seat> seatOpt = seatRepository.findById(seatId);

        if (screeningOpt.isEmpty() || seatOpt.isEmpty()) {
            return false;
        }

        Screening screening = screeningOpt.get();
        Seat seat = seatOpt.get();

        Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(screening, seat);

        if (reservationOpt.isPresent()) {
            SeatReservation reservation = reservationOpt.get();

            // Check if seat is already reserved
            if (reservation.isReserved()) {
                return false;
            }

            // Mark as reserved
            reservation.setReserved(true);
            seatReservationRepository.save(reservation);
            return true;
        }

        return false;
    }

    /**
     * Release a previously reserved seat
     */
    @Transactional
    public boolean releaseSeat(Long screeningId, Long seatId) {
        try {
            Optional<Screening> screeningOpt = screeningRepository.findById(screeningId);
            Optional<Seat> seatOpt = seatRepository.findById(seatId);

            if (screeningOpt.isEmpty() || seatOpt.isEmpty()) {
                return false;
            }

            Screening screening = screeningOpt.get();
            Seat seat = seatOpt.get();

            // Find the reservation
            Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(screening, seat);

            if (reservationOpt.isPresent()) {
                SeatReservation reservation = reservationOpt.get();

                // Mark as not reserved
                reservation.setReserved(false);
                seatReservationRepository.save(reservation);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Error releasing seat: screeningId={}, seatId={}", screeningId, seatId, e);
            return false;
        }
    }

    /**
     * Create a new ticket for a user
     */
    @Transactional
    public Optional<Ticket> createTicket(User user, Long screeningId, Long seatId) {
        try {
            Optional<Screening> screeningOpt = screeningRepository.findById(screeningId);
            Optional<Seat> seatOpt = seatRepository.findById(seatId);

            if (screeningOpt.isEmpty() || seatOpt.isEmpty() || user == null) {
                return Optional.empty();
            }

            Screening screening = screeningOpt.get();
            Seat seat = seatOpt.get();

            // Find reservation to check if the seat is already reserved
            Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(screening, seat);

            if (reservationOpt.isPresent() && reservationOpt.get().isReserved()) {
                Ticket ticket = new Ticket();
                ticket.setUser(user);
                ticket.setScreening(screening);
                ticket.setSeat(seat);
                ticket.setStatus(Ticket.TicketStatus.ACTIVE);

                return Optional.of(ticketRepository.save(ticket));
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error creating ticket: userId={}, screeningId={}, seatId={}", user.getId(), screeningId, seatId, e);
            return Optional.empty();
        }
    }

    /**
     * Cancel a ticket
     */
    @Transactional
    public boolean cancelTicket(Long ticketId, Long userId) {
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);

            if (ticketOpt.isEmpty()) {
                return false;
            }

            Ticket ticket = ticketOpt.get();

            // Ensure the ticket belongs to the user
            if (!ticket.getUser().getId().equals(userId)) {
                return false;
            }

            // Cancel the ticket
            ticket.setStatus(Ticket.TicketStatus.CANCELED);
            ticketRepository.save(ticket);

            // Release the seat
            Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(ticket.getScreening(), ticket.getSeat());

            if (reservationOpt.isPresent()) {
                SeatReservation reservation = reservationOpt.get();
                reservation.setReserved(false);
                seatReservationRepository.save(reservation);
            }

            return true;
        } catch (Exception e) {
            log.error("Error canceling ticket: ticketId={}, userId={}", ticketId, userId, e);
            return false;
        }
    }

    /**
     * Get active tickets for a user
     */
    public List<Ticket> getUserActiveTickets(Long userId) {
        return ticketRepository.findByUserIdAndStatusOrderByPurchaseTimeDesc(userId, Ticket.TicketStatus.ACTIVE);
    }

    /**
     * Get all tickets for a user
     */
    public List<Ticket> getAllTicketsForUser(Long userId) {
        return ticketRepository.findByUserIdOrderByPurchaseTimeDesc(userId);
    }

    /**
     * Get upcoming tickets for a user
     * (tickets for screenings that haven't started yet)
     */
    public List<Ticket> getUpcomingTicketsForUser(Long userId) {
        return ticketRepository.findByUserIdAndScreeningStartTimeAfterOrderByScreeningStartTime(userId, LocalDateTime.now());
    }
}
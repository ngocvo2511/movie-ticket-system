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

    // Constant for reservation timeout (in minutes)
    private static final int RESERVATION_TIMEOUT_MINUTES = 15;

    /**
     * Get all available seats for a screening
     */
    public List<SeatReservation> getAvailableSeats(Long screeningId) {
        // Clean up expired reservations first
        cleanupExpiredReservations();
        
        // Get seats with proper ordering
        List<SeatReservation> seats = seatReservationRepository.findByScreeningIdOrderBySeat_RowNumberAscSeat_SeatNumberAsc(screeningId);
        
        // Sort seats by row number and seat number
        seats.sort((a, b) -> {
            int rowCompare = a.getSeat().getRowNumber().compareTo(b.getSeat().getRowNumber());
            if (rowCompare == 0) {
                return a.getSeat().getSeatNumber().compareTo(b.getSeat().getSeatNumber());
            }
            return rowCompare;
        });
        
        return seats;
    }

    /**
     * Clean up expired reservations
     */
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<SeatReservation> expiredReservations = seatReservationRepository.findByReservationExpiryLessThanAndConfirmedFalse(now);
        for (SeatReservation reservation : expiredReservations) {
            reservation.setReserved(false);
            reservation.setReservationExpiry(null);
            seatReservationRepository.save(reservation);
        }
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

            // Kiểm tra xem ghế đã được xác nhận chưa
            if (reservation.isConfirmed()) {
                return false;
            }

            // Kiểm tra xem ghế có đang được đặt tạm thời và chưa hết hạn không
            if (reservation.isReserved() && (reservation.getReservationExpiry() == null || 
                reservation.getReservationExpiry().isAfter(LocalDateTime.now()))) {
                return false;
            }

            // Đặt chỗ tạm thời với thời gian hết hạn
            reservation.setReserved(true);
            reservation.setConfirmed(false);
            reservation.setReservationExpiry(LocalDateTime.now().plusMinutes(RESERVATION_TIMEOUT_MINUTES));
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

                // Chỉ giải phóng ghế nếu nó chưa được xác nhận
                if (!reservation.isConfirmed()) {
                    reservation.setReserved(false);
                    reservation.setReservationExpiry(null);
                    seatReservationRepository.save(reservation);
                    return true;
                }
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

            // Tìm reservation để kiểm tra và xác nhận
            Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(screening, seat);

            if (reservationOpt.isPresent() && reservationOpt.get().isReserved()) {
                SeatReservation reservation = reservationOpt.get();
                
                // Xác nhận đặt chỗ và xóa thời gian hết hạn
                reservation.setConfirmed(true);
                reservation.setReservationExpiry(null);
                seatReservationRepository.save(reservation);

                // Tạo vé
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

            // Check if ticket is already canceled
            if (ticket.getStatus() == Ticket.TicketStatus.CANCELED) {
                return false;
            }

            // Cancel the ticket
            ticket.setStatus(Ticket.TicketStatus.CANCELED);
            ticketRepository.save(ticket);

            // Release the seat
            Optional<SeatReservation> reservationOpt = seatReservationRepository.findByScreeningAndSeat(ticket.getScreening(), ticket.getSeat());

            if (reservationOpt.isPresent()) {
                SeatReservation reservation = reservationOpt.get();
                // Reset both reserved and confirmed status
                reservation.setReserved(false);
                reservation.setConfirmed(false);
                reservation.setReservationExpiry(null);
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
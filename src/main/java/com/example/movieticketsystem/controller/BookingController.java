package com.example.movieticketsystem.controller;

import com.example.movieticketsystem.dto.BookingRequest;
import com.example.movieticketsystem.model.*;
import com.example.movieticketsystem.service.BookingService;
import com.example.movieticketsystem.service.ScreeningService;
import com.example.movieticketsystem.service.SeatService;
import com.example.movieticketsystem.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final ScreeningService screeningService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/screening/{id}")
    public String showSeatSelection(@PathVariable Long id, Model model) {
        Optional<Screening> screening = screeningService.findScreeningById(id);

        if (screening.isEmpty()) {
            return "redirect:/movies";
        }

        // Get available seats
        List<SeatReservation> availableSeats = bookingService.getAvailableSeats(id);

        model.addAttribute("screening", screening.get());
        model.addAttribute("seats", availableSeats);

        return "customer/seat-selection";
    }

    @PostMapping("/reserve")
    public String reserveSeats(@RequestParam("screeningId") Long screeningId,
                               @RequestParam("seatIds") String seatIds,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {

        try {
            // Parse seat IDs from comma-separated string
            List<Long> selectedSeatIds = Arrays.stream(seatIds.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (selectedSeatIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No seats selected.");
                return "redirect:/booking/screening/" + screeningId;
            }

            // Try to reserve all selected seats
            List<Long> reservedSeatIds = new ArrayList<>();
            boolean allSeatsReserved = true;

            for (Long seatId : selectedSeatIds) {
                boolean reserved = bookingService.reserveSeat(screeningId, seatId);
                if (reserved) {
                    reservedSeatIds.add(seatId);
                } else {
                    allSeatsReserved = false;
                }
            }

            if (!allSeatsReserved) {
                // If some seats couldn't be reserved, release the ones that were
                for (Long seatId : reservedSeatIds) {
                    bookingService.releaseSeat(screeningId, seatId);
                }

                redirectAttributes.addFlashAttribute("error",
                        "Sorry, some seats are no longer available. Please select different seats.");
                return "redirect:/booking/screening/" + screeningId;
            }

            // Store booking details in session for the confirmation step
            session.setAttribute("screeningId", screeningId);
            session.setAttribute("seatIds", selectedSeatIds);

            return "redirect:/booking/confirm";

        } catch (InterruptedException e) {
            redirectAttributes.addFlashAttribute("error",
                    "The booking process was interrupted. Please try again.");
            return "redirect:/movies";
        }
    }

    @GetMapping("/confirm")
    public String showConfirmation(HttpSession session, Model model) {
        Long screeningId = (Long) session.getAttribute("screeningId");
        @SuppressWarnings("unchecked")
        List<Long> seatIds = (List<Long>) session.getAttribute("seatIds");

        if (screeningId == null || seatIds == null || seatIds.isEmpty()) {
            return "redirect:/movies";
        }

        Optional<Screening> screening = screeningService.findScreeningById(screeningId);
        if (screening.isEmpty()) {
            return "redirect:/movies";
        }

        List<Seat> seats = new ArrayList<>();
        for (Long seatId : seatIds) {
            Optional<Seat> seat = seatService.findSeatById(seatId);
            seat.ifPresent(seats::add);
        }

        if (seats.isEmpty()) {
            return "redirect:/movies";
        }

        model.addAttribute("screening", screening.get());
        model.addAttribute("seats", seats);
        model.addAttribute("totalPrice", seats.size() * screening.get().getPrice().doubleValue());

        return "customer/confirmation";
    }

    @PostMapping("/complete")
    public String completeBooking(Authentication authentication,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        Optional<User> user = userService.findByUsername(username);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        Long screeningId = (Long) session.getAttribute("screeningId");
        @SuppressWarnings("unchecked")
        List<Long> seatIds = (List<Long>) session.getAttribute("seatIds");

        if (screeningId == null || seatIds == null || seatIds.isEmpty()) {
            return "redirect:/movies";
        }

        List<Ticket> tickets = new ArrayList<>();
        for (Long seatId : seatIds) {
            Optional<Ticket> ticket = bookingService.createTicket(user.get(), screeningId, seatId);
            ticket.ifPresent(tickets::add);
        }

        if (tickets.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Sorry, there was an error completing your booking. Please try again.");
            return "redirect:/movies";
        }

        // Clear session data
        session.removeAttribute("screeningId");
        session.removeAttribute("seatIds");

        redirectAttributes.addFlashAttribute("success",
                "Your booking is complete! " + tickets.size() + " tickets have been issued.");
        return "redirect:/customer/tickets";
    }

    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        Optional<User> user = userService.findByUsername(username);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        boolean canceled = bookingService.cancelTicket(id, user.get().getId());

        if (!canceled) {
            redirectAttributes.addFlashAttribute("error",
                    "Unable to cancel the ticket. Please contact customer support.");
        } else {
            redirectAttributes.addFlashAttribute("success", "Your ticket has been canceled successfully.");
        }

        return "redirect:/customer/tickets";
    }
}
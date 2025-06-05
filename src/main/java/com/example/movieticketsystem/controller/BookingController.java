package com.example.movieticketsystem.controller;

import com.example.movieticketsystem.dto.BookingRequest;
import com.example.movieticketsystem.model.*;
import com.example.movieticketsystem.service.BookingService;
import com.example.movieticketsystem.service.ScreeningService;
import com.example.movieticketsystem.service.SeatService;
import com.example.movieticketsystem.service.UserService;
import com.example.movieticketsystem.service.ConcurrentBookingService;

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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final ScreeningService screeningService;
    private final SeatService seatService;
    private final BookingService bookingService;
    private final UserService userService;
    private final ConcurrentBookingService concurrentBookingService;

    @GetMapping("/screening/{id}")
    public String showSeatSelection(@PathVariable Long id, Model model, HttpSession session) {
        // If there are previously selected seats in session, release them
        @SuppressWarnings("unchecked")
        List<Long> previouslySelectedSeatIds = (List<Long>) session.getAttribute("seatIds");
        Long previousScreeningId = (Long) session.getAttribute("screeningId");
        
        if (previouslySelectedSeatIds != null && previousScreeningId != null) {
            for (Long seatId : previouslySelectedSeatIds) {
                bookingService.releaseSeat(previousScreeningId, seatId);
            }
            // Clear the session attributes
            session.removeAttribute("seatIds");
            session.removeAttribute("screeningId");
        }

        Optional<Screening> screening = screeningService.findScreeningById(id);

        if (screening.isEmpty()) {
            return "redirect:/movies";
        }

        // Get available seats
        List<SeatReservation> availableSeats = bookingService.getAvailableSeats(id);

        // Group seats by row using TreeMap to maintain row order
        Map<Integer, List<SeatReservation>> seatsByRow = availableSeats.stream()
                .collect(Collectors.groupingBy(
                    sr -> sr.getSeat().getRowNumber(),
                    TreeMap::new,
                    Collectors.toList()
                ));

        model.addAttribute("screening", screening.get());
        model.addAttribute("seatRows", seatsByRow);

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

            String username = authentication.getName();
            Optional<User> user = userService.findByUsername(username);

            if (user.isEmpty()) {
                return "redirect:/login";
            }

            // Sử dụng ConcurrentBookingService để đặt vé đồng thời
            CompletableFuture<BookingResult> bookingFuture = 
                concurrentBookingService.reserveSeatsAsync(screeningId, selectedSeatIds, user.get());

            // Đợi kết quả với timeout
            BookingResult result = bookingFuture.get(15, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                redirectAttributes.addFlashAttribute("error", result.getMessage());
                return "redirect:/booking/screening/" + screeningId;
            }

            // Store booking details in session for the confirmation step
            session.setAttribute("screeningId", screeningId);
            session.setAttribute("seatIds", selectedSeatIds);

            return "redirect:/booking/confirm";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            redirectAttributes.addFlashAttribute("error",
                    "The booking process was interrupted. Please try again.");
            return "redirect:/movies";
        } catch (TimeoutException e) {
            redirectAttributes.addFlashAttribute("error",
                    "The booking process timed out. Please try again.");
            return "redirect:/movies";
        } catch (ExecutionException e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred during booking. Please try again.");
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

        try {
            // Tạo vé sau khi thanh toán thành công
            CompletableFuture<BookingResult> ticketsFuture = 
                concurrentBookingService.createTicketsAfterPayment(screeningId, seatIds, user.get());
            
            BookingResult result = ticketsFuture.get(15, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                redirectAttributes.addFlashAttribute("error", result.getMessage());
                return "redirect:/booking/screening/" + screeningId;
        }

        // Clear session data
        session.removeAttribute("screeningId");
        session.removeAttribute("seatIds");

        redirectAttributes.addFlashAttribute("success",
                    "Your booking is complete! " + result.getTickets().size() + " tickets have been issued.");
        return "redirect:/customer/tickets";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            redirectAttributes.addFlashAttribute("error",
                    "The booking process was interrupted. Please try again.");
            return "redirect:/movies";
        } catch (TimeoutException e) {
            redirectAttributes.addFlashAttribute("error",
                    "The booking process timed out. Please try again.");
            return "redirect:/movies";
        } catch (ExecutionException e) {
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred during booking. Please try again.");
            return "redirect:/movies";
        }
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
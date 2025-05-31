package com.example.movieticketsystem.controller;

import com.example.movieticketsystem.model.Ticket;
import com.example.movieticketsystem.model.User;
import com.example.movieticketsystem.service.BookingService;
import com.example.movieticketsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final UserService userService;
    private final BookingService bookingService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "customer/dashboard";
    }

    @GetMapping("/tickets")
    public String viewTickets(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<User> user = userService.findByUsername(username);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        List<Ticket> activeTickets = bookingService.getUserActiveTickets(user.get().getId());
        model.addAttribute("tickets", activeTickets);

        return "customer/tickets";
    }

    @GetMapping("/history")
    public String viewBookingHistory(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<User> user = userService.findByUsername(username);

        if (user.isEmpty()) {
            return "redirect:/login";
        }

        List<Ticket> allTickets = bookingService.getAllTicketsForUser(user.get().getId());
        model.addAttribute("tickets", allTickets);

        return "customer/booking-history";
    }
}
package com.example.movieticketsystem.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BookingResult {
    private boolean success;
    private String message;
    private List<Ticket> tickets;

    public BookingResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.tickets = new ArrayList<>();
    }

    public BookingResult(boolean success, String message, List<Ticket> tickets) {
        this.success = success;
        this.message = message;
        this.tickets = tickets;
    }
} 
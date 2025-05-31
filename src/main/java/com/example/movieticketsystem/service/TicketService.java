package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.model.Ticket;
import com.example.movieticketsystem.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public List<Ticket> findAllTickets() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> findTicketById(Long id) {
        return ticketRepository.findById(id);
    }

    public List<Ticket> findTicketsByScreening(Screening screening) {
        return ticketRepository.findByScreeningId(screening.getId());
    }

    @Transactional
    public Ticket saveTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        ticketRepository.deleteById(id);
    }
}
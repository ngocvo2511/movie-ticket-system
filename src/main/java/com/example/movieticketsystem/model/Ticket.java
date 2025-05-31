package com.example.movieticketsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @CreationTimestamp
    @Column(name = "purchase_time", updatable = false)
    private LocalDateTime purchaseTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.ACTIVE;

    public enum TicketStatus {
        ACTIVE,
        USED,
        CANCELED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return id != null && Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        // Use a constant or simple hashCode to avoid recursive calls
        return id != null ? id.hashCode() : 31;
    }

    // Rename this method to avoid overriding the JPA property getter
    public String getSeatName() {
        return this.seat != null ? this.seat.toString() : "Unknown Seat";
    }
}
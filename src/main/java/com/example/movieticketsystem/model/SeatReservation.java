package com.example.movieticketsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "seat_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @ManyToOne
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "is_reserved")
    private boolean reserved;

    @Version
    @Column(name = "lock_version")
    private Integer version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeatReservation that = (SeatReservation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Use a constant hash code to avoid recursive calls
        return id != null ? id.hashCode() : 31;
    }
}
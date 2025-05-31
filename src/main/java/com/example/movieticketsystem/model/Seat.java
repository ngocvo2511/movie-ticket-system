package com.example.movieticketsystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hall_number", nullable = false)
    private Integer hallNumber;

    @Column(name = "row_num", nullable = false)
    private Integer rowNumber;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "seat_name")
    private String seatName;

    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL)
    private Set<SeatReservation> seatReservations = new HashSet<>();

    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL)
    private Set<Ticket> tickets = new HashSet<>();

    @Override
    public String toString() {
        return seatName != null ? seatName : "Row " + rowNumber + ", Seat " + seatNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return id != null && Objects.equals(id, seat.id);
    }

    @Override
    public int hashCode() {
        // Use a constant hash code to avoid recursive calls
        return id != null ? id.hashCode() : 31;
    }
}
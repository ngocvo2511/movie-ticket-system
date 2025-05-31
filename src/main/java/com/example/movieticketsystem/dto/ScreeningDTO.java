package com.example.movieticketsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningDTO {

    private Long id;

    @NotNull(message = "Movie is required")
    private Long movieId;

    @NotNull(message = "Start time is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @NotNull(message = "Hall number is required")
    @Positive(message = "Hall number must be positive")
    private Integer hallNumber;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Price cannot exceed 8 digits in integer part and 2 digits in fraction part")
    private BigDecimal price;

    // Derived field, calculated from movie duration
    private LocalDateTime endTime;
}
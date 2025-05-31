package com.example.movieticketsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration;

    @Size(max = 100, message = "Genre must be less than 100 characters")
    private String genre;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;

    private String imageUrl;
}
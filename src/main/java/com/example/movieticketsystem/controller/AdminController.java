package com.example.movieticketsystem.controller;

import com.example.movieticketsystem.dto.ScreeningCreateDTO;
import com.example.movieticketsystem.model.Movie;
import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.service.MovieService;
import com.example.movieticketsystem.service.ScreeningService;
import com.example.movieticketsystem.service.SeatService;
import com.example.movieticketsystem.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MovieService movieService;
    private final ScreeningService screeningService;
    private final SeatService seatService;
    private final TicketService ticketService;
    
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/movies/";

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("movies", movieService.findAllMovies());
        model.addAttribute("screenings", screeningService.findAllScreenings());
        return "admin/dashboard";
    }

    // Movie management
    @GetMapping("/movies/add")
    public String showAddMovieForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movie-form";
    }

    @PostMapping("/movies/add")
    public String addMovie(@Valid @ModelAttribute Movie movie, 
                         BindingResult result,
                         @RequestParam("imageFile") MultipartFile imageFile) {
        if (result.hasErrors()) {
            return "admin/movie-form";
        }

        if (!imageFile.isEmpty()) {
            try {
                // Generate unique filename
                String originalFilename = imageFile.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String filename = UUID.randomUUID().toString() + extension;
                
                // Save file
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.copy(imageFile.getInputStream(), uploadPath.resolve(filename));
                
                // Set image URL
                movie.setImageUrl("/uploads/movies/" + filename);
            } catch (IOException e) {
                result.rejectValue("imageUrl", "error.movie", "Failed to upload image");
                return "admin/movie-form";
            }
        }

        movieService.saveMovie(movie);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/movies/edit/{id}")
    public String showEditMovieForm(@PathVariable Long id, Model model) {
        Optional<Movie> movie = movieService.findMovieById(id);
        if (movie.isPresent()) {
            model.addAttribute("movie", movie.get());
            return "admin/movie-form";
        } else {
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/movies/edit/{id}")
    public String updateMovie(@PathVariable Long id, 
                           @Valid @ModelAttribute Movie movie, 
                           BindingResult result,
                           @RequestParam("imageFile") MultipartFile imageFile) {
        if (result.hasErrors()) {
            return "admin/movie-form";
        }

        // Get existing movie to preserve image if no new one is uploaded
        Optional<Movie> existingMovie = movieService.findMovieById(id);
        if (existingMovie.isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        if (!imageFile.isEmpty()) {
            try {
                // Delete old image if exists
                String oldImageUrl = existingMovie.get().getImageUrl();
                if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/movies/")) {
                    Path oldImagePath = Paths.get(UPLOAD_DIR + oldImageUrl.substring("/uploads/movies/".length()));
                    Files.deleteIfExists(oldImagePath);
                }

                // Save new image
                String originalFilename = imageFile.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String filename = UUID.randomUUID().toString() + extension;
                
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Files.copy(imageFile.getInputStream(), uploadPath.resolve(filename));
                
                movie.setImageUrl("/uploads/movies/" + filename);
            } catch (IOException e) {
                result.rejectValue("imageUrl", "error.movie", "Failed to upload image");
                return "admin/movie-form";
            }
        } else {
            // Keep existing image URL if no new image is uploaded
            movie.setImageUrl(existingMovie.get().getImageUrl());
        }

        movie.setId(id);
        movieService.saveMovie(movie);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/movies/delete/{id}")
    public String deleteMovie(@PathVariable Long id) {
        // Delete movie image if exists
        Optional<Movie> movie = movieService.findMovieById(id);
        if (movie.isPresent() && movie.get().getImageUrl() != null 
            && movie.get().getImageUrl().startsWith("/uploads/movies/")) {
            try {
                String filename = movie.get().getImageUrl().substring("/uploads/movies/".length());
                Path imagePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(imagePath);
            } catch (IOException e) {
                // Log error but continue with deletion
                e.printStackTrace();
            }
        }
        
        movieService.deleteMovie(id);
        return "redirect:/admin/dashboard";
    }

    // Screening management
    @GetMapping("/screenings/add")
    public String showAddScreeningForm(Model model) {
        model.addAttribute("screeningDTO", new ScreeningCreateDTO());
        model.addAttribute("movies", movieService.findAllMovies());
        return "admin/screening-form";
    }

    @PostMapping("/screenings/add")
    public String addScreening(@Valid @ModelAttribute("screeningDTO") ScreeningCreateDTO screeningDTO,
                               BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.findAllMovies());
            return "admin/screening-form";
        }

        try {
            // Create screening from DTO
            Screening screening = new Screening();
            Optional<Movie> movieOpt = movieService.findMovieById(screeningDTO.getMovieId());

            if (movieOpt.isEmpty()) {
                result.rejectValue("movieId", "error.screening", "Movie not found");
                model.addAttribute("movies", movieService.findAllMovies());
                return "admin/screening-form";
            }

            Movie movie = movieOpt.get();
            screening.setMovie(movie);
            screening.setStartTime(screeningDTO.getStartTime());
            screening.setHallNumber(screeningDTO.getHallNumber());
            screening.setPrice(screeningDTO.getPrice());

            // Calculate end time based on movie duration
            LocalDateTime endTime = screeningDTO.getStartTime().plusMinutes(movie.getDuration());
            screening.setEndTime(endTime);

            // Set total seats
            screening.setTotalSeats(screeningDTO.getTotalSeats());

            // Save screening first
            Screening savedScreening = screeningService.saveScreening(screening);

            // Then initialize seat reservations separately
            seatService.initializeSeatReservations(savedScreening);

            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            // Log the exception
            e.printStackTrace();

            // Add error message
            model.addAttribute("error", "An error occurred: " + e.getMessage());
            model.addAttribute("movies", movieService.findAllMovies());
            return "admin/screening-form";
        }
    }

    @GetMapping("/screenings/edit/{id}")
    public String showEditScreeningForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Screening> screeningOpt = screeningService.findScreeningById(id);

        if (screeningOpt.isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        Screening screening = screeningOpt.get();

        // Check if screening can be modified
        if (!screeningService.canModifyScreening(id)) {
            redirectAttributes.addFlashAttribute("error", 
                "Cannot modify screening because it has active tickets. Create a new screening instead.");
            return "redirect:/admin/dashboard";
        }

        // Convert Screening to DTO
        ScreeningCreateDTO screeningDTO = new ScreeningCreateDTO();
        screeningDTO.setId(screening.getId());
        screeningDTO.setMovieId(screening.getMovie().getId());
        screeningDTO.setStartTime(screening.getStartTime());
        screeningDTO.setHallNumber(screening.getHallNumber());
        screeningDTO.setPrice(screening.getPrice());

        model.addAttribute("screeningDTO", screeningDTO);
        model.addAttribute("movies", movieService.findAllMovies());
        return "admin/screening-form";
    }

    @PostMapping("/screenings/edit/{id}")
    public String updateScreening(@PathVariable Long id,
                                  @Valid @ModelAttribute("screeningDTO") ScreeningCreateDTO screeningDTO,
                                  BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("movies", movieService.findAllMovies());
            return "admin/screening-form";
        }

        Optional<Screening> existingScreeningOpt = screeningService.findScreeningById(id);
        if (existingScreeningOpt.isEmpty()) {
            return "redirect:/admin/dashboard";
        }

        Screening screening = existingScreeningOpt.get();

        try {
            // Update screening from DTO
            Optional<Movie> movieOpt = movieService.findMovieById(screeningDTO.getMovieId());
            if (movieOpt.isEmpty()) {
                result.rejectValue("movieId", "error.screening", "Movie not found");
                model.addAttribute("movies", movieService.findAllMovies());
                return "admin/screening-form";
            }

            Movie movie = movieOpt.get();
            screening.setMovie(movie);
            screening.setStartTime(screeningDTO.getStartTime());
            screening.setHallNumber(screeningDTO.getHallNumber());
            screening.setPrice(screeningDTO.getPrice());

            // Calculate end time based on movie duration
            LocalDateTime endTime = screeningDTO.getStartTime().plusMinutes(movie.getDuration());
            screening.setEndTime(endTime);

            // Update total seats
            screening.setTotalSeats(screeningDTO.getTotalSeats());

            // Save screening
            Screening savedScreening = screeningService.saveScreening(screening);

            // Re-initialize seat layout
            seatService.initializeSeatReservations(savedScreening);

            redirectAttributes.addFlashAttribute("success", "Screening updated successfully.");
            return "redirect:/admin/dashboard";
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Cannot modify screening that has active tickets")) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot modify screening because it has active tickets. Create a new screening instead.");
            } else {
                redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
            }
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred.");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/screenings/delete/{id}")
    public String deleteScreening(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = screeningService.deleteScreening(id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Screening deleted successfully.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to delete screening. It might have been already deleted or does not exist.");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Cannot delete screening that has active tickets")) {
                redirectAttributes.addFlashAttribute("error", 
                    "Cannot delete screening because it has active tickets.");
            } else {
                redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
            }
        }
        return "redirect:/admin/dashboard";
    }

    // Reports section
    @GetMapping("/reports/tickets")
    public String ticketReports(Model model) {
        model.addAttribute("tickets", ticketService.findAllTickets());
        return "admin/ticket-reports";
    }
}
package com.example.movieticketsystem.controller;

import com.example.movieticketsystem.model.Movie;
import com.example.movieticketsystem.model.Screening;
import com.example.movieticketsystem.service.MovieService;
import com.example.movieticketsystem.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final ScreeningService screeningService;

    @GetMapping("/")
    public String home(Model model) {
        List<Movie> movies = movieService.findAllMovies();
        model.addAttribute("movies", movies);
        return "index";
    }

    @GetMapping("/movies")
    public String listMovies(@RequestParam(required = false) String title, Model model) {
        List<Movie> movies;
        if (title != null && !title.isEmpty()) {
            movies = movieService.findMoviesByTitle(title);
        } else {
            movies = movieService.findAllMovies();
        }
        model.addAttribute("movies", movies);
        return "customer/movie-list";
    }

    @GetMapping("/movies/{id}")
    public String movieDetails(@PathVariable Long id, Model model) {
        Optional<Movie> movie = movieService.findMovieById(id);
        if (movie.isPresent()) {
            model.addAttribute("movie", movie.get());
            List<Screening> screenings = screeningService.findScreeningsByMovie(id);
            model.addAttribute("screenings", screenings);
            return "customer/movie-details";
        } else {
            return "redirect:/movies";
        }
    }
}

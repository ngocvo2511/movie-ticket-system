package com.example.movieticketsystem.service;

import com.example.movieticketsystem.model.Movie;
import com.example.movieticketsystem.repository.MovieRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    public List<Movie> findAllMovies() {
        return movieRepository.findAll();
    }

    public Optional<Movie> findMovieById(Long id) {
        return movieRepository.findById(id);
    }

    public List<Movie> findMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional
    public Movie saveMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    @Transactional
    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
    }
}

package com.example.movieticketsystem.repository;

import com.example.movieticketsystem.model.Movie;
import com.example.movieticketsystem.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    List<Screening> findByMovie(Movie movie);

    List<Screening> findByStartTimeAfterOrderByStartTime(LocalDateTime date);

    List<Screening> findByMovieIdAndStartTimeAfterOrderByStartTime(Long movieId, LocalDateTime date);
}

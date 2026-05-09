package edu.arch.movie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MovieCastRepository extends JpaRepository<MovieCastEntity, Long> {
    List<MovieCastEntity> findByMovieIdIn(Collection<Long> movieIds);

    void deleteByMovieId(Long movieId);
}

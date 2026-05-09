package edu.arch.movie;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MoviePromotionRepository extends JpaRepository<MoviePromotionEntity, Long> {
    List<MoviePromotionEntity> findByMovieIdIn(Collection<Long> movieIds);

    void deleteByMovieId(Long movieId);
}

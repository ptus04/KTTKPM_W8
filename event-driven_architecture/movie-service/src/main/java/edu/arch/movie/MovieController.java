package edu.arch.movie;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieRepository movieRepository;
    private final MovieCastRepository movieCastRepository;
    private final MoviePromotionRepository moviePromotionRepository;

    public MovieController(
            MovieRepository movieRepository,
            MovieCastRepository movieCastRepository,
            MoviePromotionRepository moviePromotionRepository) {
        this.movieRepository = movieRepository;
        this.movieCastRepository = movieCastRepository;
        this.moviePromotionRepository = moviePromotionRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        List<MovieEntity> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            return List.of();
        }
        List<Long> ids = movies.stream().map(MovieEntity::getId).toList();
        Map<Long, List<String>> castByMovie =
                movieCastRepository.findByMovieIdIn(ids).stream()
                        .collect(Collectors.groupingBy(
                                MovieCastEntity::getMovieId,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> list.stream()
                                                .sorted(Comparator.comparingInt(MovieCastEntity::getBillingOrder))
                                                .map(MovieCastEntity::getPersonName)
                                                .toList())));
        Map<Long, MoviePromotionEntity> promoByMovie =
                moviePromotionRepository.findByMovieIdIn(ids).stream()
                        .collect(Collectors.toMap(MoviePromotionEntity::getMovieId, p -> p, (a, b) -> a));

        return movies.stream().map(m -> toRow(m, castByMovie.getOrDefault(m.getId(), List.of()), promoByMovie.get(m.getId())))
                .toList();
    }

    private static Map<String, Object> toRow(MovieEntity m, List<String> cast, MoviePromotionEntity promo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("slug", m.getSlug());
        row.put("title", m.getTitle());
        row.put("subtitle", m.getSubtitle() != null ? m.getSubtitle() : "");
        row.put("tagline", m.getTagline() != null ? m.getTagline() : "");
        row.put("description", m.getDescription() != null ? m.getDescription() : "");
        row.put("ageRating", m.getAgeRating() != null ? m.getAgeRating() : "");
        row.put("genre", m.getGenre() != null ? m.getGenre() : "");
        row.put("language", m.getLanguage() != null ? m.getLanguage() : "");
        row.put("director", m.getDirector() != null ? m.getDirector() : "");
        row.put("producer", m.getProducer() != null ? m.getProducer() : "");
        row.put("releaseDate", m.getReleaseDate() != null ? m.getReleaseDate().toString() : "");
        row.put("posterUrl", m.getPosterUrl() != null ? m.getPosterUrl() : "");
        row.put("trailerUrl", m.getTrailerUrl() != null ? m.getTrailerUrl() : "");
        row.put("status", m.getStatus() != null ? m.getStatus() : "");
        row.put("cast", cast);
        if (promo != null) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("label", promo.getLabel());
            p.put("promoPrice", promo.getPromoPrice());
            p.put("currency", promo.getCurrency());
            p.put("validFrom", promo.getValidFrom().toString());
            p.put("validTo", promo.getValidTo().toString());
            p.put("marketingTagline", promo.getMarketingTagline() != null ? promo.getMarketingTagline() : "");
            row.put("promotion", p);
        } else {
            row.put("promotion", null);
        }
        return row;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, String> body) {
        String title = body.getOrDefault("title", "").trim();
        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title required"));
        }
        String slug = body.getOrDefault("slug", "").trim();
        if (slug.isEmpty()) {
            slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        }
        MovieEntity m = new MovieEntity();
        m.setSlug(slug);
        m.setTitle(title);
        m.setSubtitle(emptyToNull(body.get("subtitle")));
        m.setTagline(emptyToNull(body.get("tagline")));
        m.setDescription(emptyToNull(body.get("description")));
        m.setAgeRating(emptyToNull(body.get("ageRating")));
        m.setGenre(emptyToNull(body.get("genre")));
        m.setLanguage(Optional.ofNullable(emptyToNull(body.get("language"))).orElse("vi"));
        m.setDirector(emptyToNull(body.get("director")));
        m.setProducer(emptyToNull(body.get("producer")));
        m.setPosterUrl(emptyToNull(body.get("posterUrl")));
        m.setTrailerUrl(emptyToNull(body.get("trailerUrl")));
        m = movieRepository.save(m);
        return ResponseEntity.ok(toRow(m, List.of(), null));
    }

    private static String emptyToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

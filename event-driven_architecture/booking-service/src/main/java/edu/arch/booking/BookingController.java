package edu.arch.booking;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.JwtException;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final JwtParser jwtParser;
    private final BookingEventPublisher bookingEventPublisher;

    public BookingController(
            BookingRepository bookingRepository,
            JwtParser jwtParser,
            BookingEventPublisher bookingEventPublisher) {
        this.bookingRepository = bookingRepository;
        this.jwtParser = jwtParser;
        this.bookingEventPublisher = bookingEventPublisher;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody Map<String, Object> body) {
        final var claims = parseClaims(authorization);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
        Long userId = jwtParser.userId(claims);
        String username = jwtParser.username(claims);
        Long movieId = toLong(body.get("movieId"));
        List<String> seats = extractSeats(body);
        if (movieId == null || seats.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "movieId and seat/seats required"));
        }
        try {
            String seatJoined = String.join(",", seats);
            BookingEntity b = new BookingEntity();
            b.setUserId(userId);
            b.setUsername(username);
            b.setMovieId(movieId);
            b.setSeat(seatJoined);
            b.setTicketCode(generateTicketCode());
            b.setStatus("PENDING");
            b = bookingRepository.save(b);
            bookingEventPublisher.publishBookingCreated(b);
            return ResponseEntity.status(HttpStatus.CREATED).body(toRow(b));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "booking create failed"));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        final var claims = parseClaims(authorization);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token"));
        }
        Long userId = jwtParser.userId(claims);
        try {
            List<Map<String, Object>> rows = bookingRepository.findByUserIdOrderByIdDesc(userId).stream()
                    .map(this::toRow)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(rows);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "booking list failed"));
        }
    }

    @GetMapping("/ticket/{ticketCode}")
    public ResponseEntity<?> ticketDetail(@PathVariable String ticketCode) {
        try {
            return bookingRepository.findByTicketCode(ticketCode)
                    .<ResponseEntity<?>>map(b -> ResponseEntity.ok(toRow(b)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "ticket not found")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "ticket detail failed"));
        }
    }

    private io.jsonwebtoken.Claims parseClaims(String authorization) {
        try {
            return jwtParser.parse(authorization);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private static List<String> splitSeats(String seatJoined) {
        if (seatJoined == null || seatJoined.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(seatJoined.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static List<String> extractSeats(Map<String, Object> body) {
        Object rawSeats = body.get("seats");
        if (rawSeats instanceof List<?> list) {
            return list.stream()
                    .filter(v -> v != null)
                    .map(v -> v.toString().trim().toUpperCase())
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            ArrayList::new));
        }
        String oneSeat = body.get("seat") != null ? body.get("seat").toString().trim().toUpperCase() : "";
        return oneSeat.isEmpty() ? List.of() : List.of(oneSeat);
    }

    private String generateTicketCode() {
        return "MBE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private Map<String, Object> toRow(BookingEntity b) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", b.getId());
        row.put("userId", b.getUserId());
        row.put("username", b.getUsername());
        row.put("movieId", b.getMovieId());
        row.put("seat", b.getSeat());
        row.put("seats", splitSeats(b.getSeat()));
        row.put("ticketCode", b.getTicketCode());
        row.put("status", b.getStatus());
        return row;
    }
}

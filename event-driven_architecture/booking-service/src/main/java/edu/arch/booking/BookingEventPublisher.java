package edu.arch.booking;

import edu.arch.events.DomainEvent;
import edu.arch.events.EventTypes;
import edu.arch.events.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BookingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingCreated(BookingEntity b) {
        DomainEvent event = new DomainEvent(
                EventTypes.BOOKING_CREATED,
                Map.of(
                        "bookingId", b.getId(),
                        "userId", b.getUserId(),
                        "username", b.getUsername(),
                        "movieId", b.getMovieId(),
                        "seat", b.getSeat()));
        kafkaTemplate.send(KafkaTopics.DOMAIN_EVENTS, String.valueOf(b.getId()), event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish BOOKING_CREATED: {}", ex.getMessage());
                    } else {
                        log.info("[EVENT_PUBLISHED] type={} bookingId={}", EventTypes.BOOKING_CREATED, b.getId());
                    }
                });
    }
}

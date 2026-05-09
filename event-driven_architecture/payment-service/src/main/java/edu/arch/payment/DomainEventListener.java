package edu.arch.payment;

import edu.arch.events.DomainEvent;
import edu.arch.events.EventTypes;
import edu.arch.events.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(DomainEventListener.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Set<Long> processedBookings = ConcurrentHashMap.newKeySet();

    public DomainEventListener(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KafkaTopics.DOMAIN_EVENTS, groupId = "payment-notification-service")
    public void onEvent(DomainEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        if (EventTypes.BOOKING_CREATED.equals(event.getEventType())) {
            handleBookingCreated(event);
        } else if (EventTypes.PAYMENT_COMPLETED.equals(event.getEventType())) {
            handlePaymentCompleted(event);
        }
    }

    private void handleBookingCreated(DomainEvent event) {
        Map<String, Object> p = event.getPayload();
        if (p == null) {
            return;
        }
        Long bookingId = toLong(p.get("bookingId"));
        if (bookingId == null) {
            return;
        }
        if (!processedBookings.add(bookingId)) {
            log.warn("[PAYMENT_SKIP] duplicate BOOKING_CREATED for bookingId={}", bookingId);
            return;
        }

        DomainEvent out = new DomainEvent(
                EventTypes.PAYMENT_COMPLETED,
                Map.of(
                        "bookingId", bookingId,
                        "userId", toLong(p.get("userId")),
                        "username", String.valueOf(p.get("username")),
                        "movieId", toLong(p.get("movieId")),
                        "seat", String.valueOf(p.get("seat"))));
        kafkaTemplate.send(KafkaTopics.DOMAIN_EVENTS, String.valueOf(bookingId), out);
        log.info("[EVENT_PUBLISHED] type={} bookingId={}", EventTypes.PAYMENT_COMPLETED, bookingId);
    }

    private void handlePaymentCompleted(DomainEvent event) {
        Map<String, Object> p = event.getPayload();
        if (p == null) {
            return;
        }
        Long bookingId = toLong(p.get("bookingId"));
        String username = p.get("username") != null ? String.valueOf(p.get("username")) : "unknown";
        String msg = String.format("Booking #%s thành công!", bookingId);
        String detail = String.format("User %s đã đặt đơn #%s thành công", username, bookingId);
        log.info("[NOTIFICATION] {}", msg);
        log.info("[NOTIFICATION_DETAIL] {}", detail);
    }

    private static Long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}

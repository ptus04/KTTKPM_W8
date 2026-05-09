package edu.arch.booking;

import edu.arch.events.DomainEvent;
import edu.arch.events.EventTypes;
import edu.arch.events.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class PaymentOutcomeListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutcomeListener.class);

    private final BookingRepository bookingRepository;

    public PaymentOutcomeListener(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @KafkaListener(topics = KafkaTopics.DOMAIN_EVENTS, groupId = "booking-service-payment-outcomes")
    @Transactional
    public void onDomainEvent(DomainEvent event) {
        if (event == null || event.getEventType() == null) {
            return;
        }
        Map<String, Object> p = event.getPayload();
        if (p == null) {
            return;
        }
        if (EventTypes.PAYMENT_COMPLETED.equals(event.getEventType())) {
            Long bookingId = toLong(p.get("bookingId"));
            if (bookingId == null) {
                return;
            }
            bookingRepository.findById(bookingId).ifPresent(b -> {
                b.setStatus("PAID");
                bookingRepository.save(b);
                log.info("[BOOKING_UPDATED] bookingId={} status=PAID (from PAYMENT_COMPLETED)", bookingId);
            });
        } else if (EventTypes.BOOKING_FAILED.equals(event.getEventType())) {
            Long bookingId = toLong(p.get("bookingId"));
            if (bookingId == null) {
                return;
            }
            bookingRepository.findById(bookingId).ifPresent(b -> {
                b.setStatus("FAILED");
                bookingRepository.save(b);
                log.info("[BOOKING_UPDATED] bookingId={} status=FAILED (from BOOKING_FAILED)", bookingId);
            });
        }
    }

    private static Long toLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}

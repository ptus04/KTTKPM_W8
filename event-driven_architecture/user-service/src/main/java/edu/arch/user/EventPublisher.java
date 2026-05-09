package edu.arch.user;

import edu.arch.events.DomainEvent;
import edu.arch.events.EventTypes;
import edu.arch.events.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(Long userId, String username) {
        DomainEvent event = new DomainEvent(
                EventTypes.USER_REGISTERED,
                Map.of("userId", userId, "username", username));
        kafkaTemplate.send(KafkaTopics.DOMAIN_EVENTS, username, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish USER_REGISTERED: {}", ex.getMessage());
                    } else {
                        log.info("[EVENT_PUBLISHED] type={} userId={} username={} offset={}",
                                EventTypes.USER_REGISTERED, userId, username,
                                r != null && r.getRecordMetadata() != null
                                        ? r.getRecordMetadata().offset()
                                        : "n/a");
                    }
                });
    }
}

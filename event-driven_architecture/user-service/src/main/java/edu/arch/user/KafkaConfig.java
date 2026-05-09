package edu.arch.user;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import edu.arch.events.KafkaTopics;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic domainEventsTopic() {
        return TopicBuilder.name(KafkaTopics.DOMAIN_EVENTS).partitions(3).replicas(1).build();
    }
}

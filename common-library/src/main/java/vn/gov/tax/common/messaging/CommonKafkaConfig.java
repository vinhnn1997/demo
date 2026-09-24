package vn.gov.tax.common.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class CommonKafkaConfig {
    @Bean
    NewTopic fineCreatedTopic() {
        return new NewTopic(KafkaTopics.FINE_CREATED, 1, (short) 1);
    }
}
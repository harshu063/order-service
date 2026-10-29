package com.example.orderservice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "orders.kafka.payment-outcomes.enabled", havingValue = "true")
public class PaymentOutcomeKafkaListener {
    private final ObjectMapper objectMapper;
    private final PaymentOutcomeApplicationService outcomes;

    public PaymentOutcomeKafkaListener(ObjectMapper objectMapper, PaymentOutcomeApplicationService outcomes) {
        this.objectMapper = objectMapper;
        this.outcomes = outcomes;
    }

    @KafkaListener(
            id = "${orders.kafka.payment-outcomes.consumer-group}",
            topics = "${orders.kafka.payment-outcomes.topic}",
            concurrency = "${orders.kafka.payment-outcomes.concurrency}")
    public void consume(String payload, @Header(KafkaHeaders.RECEIVED_KEY) String recordKey) {
        try {
            PaymentOutcomeCommand command = objectMapper.readValue(payload, PaymentOutcomeCommand.class);
            if (recordKey == null || !recordKey.equals(command.orderingKey())) {
                throw new IllegalArgumentException("Kafka record key does not match payment outcome ordering key");
            }
            outcomes.apply(command);
        } catch (Exception invalidPayload) {
            throw new IllegalArgumentException("Payment outcome message cannot be applied", invalidPayload);
        }
    }
}

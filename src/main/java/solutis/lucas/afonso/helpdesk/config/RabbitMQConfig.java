package solutis.lucas.afonso.helpdesk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String TECHNICIAN_ASSIGNMENT_RESULT_QUEUE = "ticket.technician.assignment.result";

    @Bean
    public DirectExchange helpdeskExchange(@Value("${helpdesk.rabbitmq.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue technicianAssignmentResultQueue() {
        return QueueBuilder.durable(TECHNICIAN_ASSIGNMENT_RESULT_QUEUE).build();
    }

    @Bean
    public Binding technicianAssignmentResultBinding(Queue technicianAssignmentResultQueue,
            DirectExchange helpdeskExchange,
            @Value("${helpdesk.rabbitmq.response-routing-key}") String routingKey) {
        return BindingBuilder.bind(technicianAssignmentResultQueue)
                .to(helpdeskExchange)
                .with(routingKey);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

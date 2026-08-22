package solutis.lucas.afonso.helpdesk.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import solutis.lucas.afonso.helpdesk.config.RabbitMQConfig;
import solutis.lucas.afonso.helpdesk.repository.TicketRepository;

@Component
public class TechnicianAssignmentResultListener {
    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String ticketAssignedRoutingKey;

    public TechnicianAssignmentResultListener(TicketRepository ticketRepository, ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate,
            @Value("${helpdesk.rabbitmq.exchange}") String exchange,
            @Value("${helpdesk.rabbitmq.ticket-assigned-routing-key}") String ticketAssignedRoutingKey) {
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.ticketAssignedRoutingKey = ticketAssignedRoutingKey;
    }

    @RabbitListener(queues = RabbitMQConfig.TECHNICIAN_ASSIGNMENT_RESULT_QUEUE)
    public void apply(String payload) throws Exception {
        String resultJson = payload;
        if (payload.startsWith("\"") && payload.endsWith("\"")) {
            resultJson = objectMapper.readValue(payload, String.class);
        }
        TechnicianAssignmentResult result = objectMapper.readValue(resultJson, TechnicianAssignmentResult.class);
        if (!result.accepted() || result.ticketId() == null || result.technicianId() == null) {
            return;
        }

        ticketRepository.findById(result.ticketId()).ifPresent(ticket -> {
            ticket.setTechnicianId(result.technicianId());
            ticket.setUpdatedAt(java.time.LocalDateTime.now());
            ticketRepository.save(ticket);

            TicketAssigned event = new TicketAssigned(ticket.getId(), ticket.getTechnicianId());
            try {
                rabbitTemplate.convertAndSend(exchange, ticketAssignedRoutingKey,
                        objectMapper.writeValueAsString(event));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not publish TicketAssigned event", exception);
            }
        });
    }
}
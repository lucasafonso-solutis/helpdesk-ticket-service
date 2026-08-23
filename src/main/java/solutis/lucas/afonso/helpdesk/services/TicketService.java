package solutis.lucas.afonso.helpdesk.services;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.events.TicketCreated;
import solutis.lucas.afonso.helpdesk.events.TicketAssigned;
import solutis.lucas.afonso.helpdesk.events.TicketStatusChanged;
import solutis.lucas.afonso.helpdesk.events.Event;
import solutis.lucas.afonso.helpdesk.events.TechnicianAssignmentEvent;
import solutis.lucas.afonso.helpdesk.events.TechnicianAssignmentResult;
import solutis.lucas.afonso.helpdesk.config.RabbitMQConfig;
import solutis.lucas.afonso.helpdesk.repository.TicketRepository;

@Service
public class TicketService {
    private TicketRepository ticketRepository;
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private String rabbitExchange;
    private String rabbitRoutingKey;
    private String ticketAssignedRoutingKey;
    private String ticketCreatedRoutingKey;
    private String ticketStatusChangedRoutingKey;

    public TicketService(TicketRepository ticketRepository, RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                            @Value("${helpdesk.rabbitmq.exchange}") String rabbitExchange,
                            @Value("${helpdesk.rabbitmq.routing-key}") String rabbitRoutingKey,
                            @Value("${helpdesk.rabbitmq.ticket-assigned-routing-key}") String ticketAssignedRoutingKey,
                            @Value("${helpdesk.rabbitmq.ticket-created-routing-key}") String ticketCreatedRoutingKey,
                            @Value("${helpdesk.rabbitmq.ticket-status-changed-routing-key}") String ticketStatusChangedRoutingKey) {
        this.ticketRepository = ticketRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.rabbitExchange = rabbitExchange;
        this.rabbitRoutingKey = rabbitRoutingKey;
        this.ticketAssignedRoutingKey = ticketAssignedRoutingKey;
        this.ticketCreatedRoutingKey = ticketCreatedRoutingKey;
        this.ticketStatusChangedRoutingKey = ticketStatusChangedRoutingKey;
    }

    public TicketDTO create(TicketDTO ticketDTO) {
        Ticket ticket = new Ticket(ticketDTO);
        ticket = ticketRepository.save(ticket);
        this.publishEvent(new TicketCreated(ticket.getId(), ticket.getCustomerId(), ticket.getTitle(),
            ticket.getPriority(), ticket.getTicketStatus(), ticket.getTicketCategory()),
            this.ticketCreatedRoutingKey, "Could not create ticket created event");

        return new TicketDTO(ticket);
    }

    public List<TicketDTO> listById(Long id) {
        return this.ticketRepository.findById(id).stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> list() {
        return this.ticketRepository.findAll().stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> searchByTitle(String title) {
        return this.ticketRepository.findByTitle(title).stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> filterTickets(TicketStatus ticketStatus, TicketPriority ticketPriority, TicketCategory ticketCategory) {
        return this.ticketRepository.findAll().stream()
            .filter(ticket -> ticketPriority == null || ticket.getPriority() == ticketPriority)
            .filter(ticket -> ticketStatus == null || ticket.getTicketStatus() == ticketStatus)
            .filter(ticket -> ticketCategory == null || ticket.getTicketCategory() == ticketCategory)
            .map(TicketDTO::new)
            .toList();
    }

    public TicketDTO updateTicket(Long id, TicketPriority ticketPriority, TicketCategory ticketCategory, String description, TicketStatus ticketStatus) {                                 
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        TicketStatus previousStatus = ticket.getTicketStatus();
        if (ticketPriority != null) {
            ticket.setPriority(ticketPriority);
        }
        if (ticketCategory != null) {
            ticket.setTicketCategory(ticketCategory);
        }
        if (description != null) {
            ticket.setDescription(description);
        }
        if (ticketStatus != null) {
            ticket.setTicketStatus(ticketStatus);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket savedTicket = this.ticketRepository.save(ticket);
        if (ticketStatus != null && previousStatus != ticketStatus) {
            this.publishEvent(new TicketStatusChanged(savedTicket.getId(), savedTicket.getCustomerId(),
                previousStatus, savedTicket.getTicketStatus()),
                this.ticketStatusChangedRoutingKey, "Could not create ticket status changed event");
        }

        return new TicketDTO(savedTicket);
    }

    public TicketDTO assignTechnician(Long id, Long technicianId) {
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        if (technicianId == null) {
            throw new IllegalArgumentException("technicianId is required");
        }
        TechnicianAssignmentEvent event = new TechnicianAssignmentEvent(ticket.getId(), technicianId, ticket.getCustomerId());
        this.publishEvent(event, this.rabbitRoutingKey, "Could not create technician assignment event");

        return new TicketDTO(ticket);
    }

    @RabbitListener(queues = RabbitMQConfig.TECHNICIAN_ASSIGNMENT_RESULT_QUEUE)
    public void handleTechnicianAssignmentResult(String eventJson) {
        try {
            TechnicianAssignmentResult event = this.objectMapper.readValue(eventJson, TechnicianAssignmentResult.class);
                if (!Boolean.TRUE.equals(event.accepted())) {
                    return;
                }
            Ticket ticket = this.ticketRepository.findById(event.ticketId()).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + event.ticketId()));
            ticket.setTechnicianId(event.technicianId());
            ticket.setUpdatedAt(LocalDateTime.now());
            Ticket savedTicket = this.ticketRepository.save(ticket);
            this.publishEvent(new TicketAssigned(savedTicket.getId(), savedTicket.getCustomerId(), savedTicket.getTechnicianId()),
                this.ticketAssignedRoutingKey, "Could not create ticket assigned event");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read technician assignment result event", exception);
        }
    }

    public TicketDTO closeTicket(Long id) {
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        TicketStatus previousStatus = ticket.getTicketStatus();
        ticket.setTicketStatus(TicketStatus.CLOSED);
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket savedTicket = this.ticketRepository.save(ticket);
        if (previousStatus != TicketStatus.CLOSED) {
            this.publishEvent(new TicketStatusChanged(savedTicket.getId(), savedTicket.getCustomerId(),
                    previousStatus, savedTicket.getTicketStatus()),
                    this.ticketStatusChangedRoutingKey, "Could not create ticket status changed event");
        }

        return new TicketDTO(savedTicket);
    }

    private void publishEvent(Event event, String routingKey, String errorMessage) {
        try {
            this.rabbitTemplate.convertAndSend(this.rabbitExchange, routingKey, this.objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }
}

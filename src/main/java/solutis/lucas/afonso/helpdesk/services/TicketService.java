package solutis.lucas.afonso.helpdesk.services;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.repository.TicketRepository;

@Service
public class TicketService {
    private TicketRepository ticketRepository;
    private RabbitTemplate rabbitTemplate;
    private ObjectMapper objectMapper;
    private String rabbitExchange;
    private String rabbitRoutingKey;

    public TicketService(TicketRepository ticketRepository, RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${helpdesk.rabbitmq.exchange}") String rabbitExchange,
            @Value("${helpdesk.rabbitmq.routing-key}") String rabbitRoutingKey) {
        this.ticketRepository = ticketRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.rabbitExchange = rabbitExchange;
        this.rabbitRoutingKey = rabbitRoutingKey;
    }

    public TicketDTO create(TicketDTO ticketDTO) {
        Ticket ticket = new Ticket(ticketDTO);
        ticket = ticketRepository.save(ticket);

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

    public TicketDTO updateTicket(Long id, TicketPriority ticketPriority, TicketCategory ticketCategory, 
                                    String description, TicketStatus ticketStatus) {                                 
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
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

        return new TicketDTO(this.ticketRepository.save(ticket));
    }

    public TicketDTO assignTechnician(Long id, Long technicianId) {
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        if (technicianId == null) {
            throw new IllegalArgumentException("technicianId is required");
        }

        TechnicianAssignmentEvent event = new TechnicianAssignmentEvent(
            ticket.getId(), technicianId);
        try {
            String eventJson = this.objectMapper.writeValueAsString(event);
            this.rabbitTemplate.convertAndSend(this.rabbitExchange, this.rabbitRoutingKey, eventJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not create technician assignment event", exception);
        }

        return new TicketDTO(ticket);
    }

    private record TechnicianAssignmentEvent(Long ticketId, Long technicianId) {
    }

    public TicketDTO closeTicket(Long id) {
        Ticket ticket = this.ticketRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
        ticket.setTicketStatus(TicketStatus.CLOSED);
        ticket.setUpdatedAt(LocalDateTime.now());

        return new TicketDTO(this.ticketRepository.save(ticket));
    }
}

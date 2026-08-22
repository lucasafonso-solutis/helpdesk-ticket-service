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

    public TicketDTO updateTicket(Long id, TicketDTO ticketDTO, TicketPriority ticketPriority, TicketCategory ticketCategory, 
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

        TechnicianUser technician = requestTechnician(technicianId);
        if (technician == null || technician.userId() == null) {
            throw new IllegalArgumentException("The user is not an active technician: " + technicianId);
        }

        ticket.setTechnicianId(technician.userId());
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket updatedTicket = this.ticketRepository.save(ticket);

        return new TicketDTO(updatedTicket);
    }

    private TechnicianUser requestTechnician(Long technicianId) {
        TechnicianRequest request = new TechnicianRequest(technicianId);
        try {
            String requestJson = this.objectMapper.writeValueAsString(request);
            Object response = this.rabbitTemplate.convertSendAndReceive(this.rabbitExchange, this.rabbitRoutingKey, requestJson);
            if (response == null) {
                throw new IllegalStateException("User Service did not respond");
            }

            String responseJson = response instanceof byte[] bytes? new String(bytes, java.nio.charset.StandardCharsets.UTF_8)
                    : response.toString();
            return this.objectMapper.readValue(responseJson, TechnicianUser.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid technician response", exception);
        }
    }

    private record TechnicianRequest(Long technicianId) {
    }

    private record TechnicianUser(Long userId) {
    }
}

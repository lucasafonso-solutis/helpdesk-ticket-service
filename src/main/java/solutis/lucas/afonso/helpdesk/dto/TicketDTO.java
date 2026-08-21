package solutis.lucas.afonso.helpdesk.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;

public record TicketDTO(Long id, @NotBlank String title, @NotBlank String description,
            @NotNull TicketPriority priority, TicketStatus ticketStatus,
            @NotNull TicketCategory ticketCategory, Long customerId, Long technicianId, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        
    public TicketDTO(Ticket ticket) {
        this(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getPriority(), ticket.getTicketStatus(), 
            ticket.getTicketCategory(), ticket.getCustomerId(), ticket.getTechnicianId(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}

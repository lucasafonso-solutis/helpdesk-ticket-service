package solutis.lucas.afonso.helpdesk.events;

import solutis.lucas.afonso.helpdesk.entities.TicketStatus;

public record TicketStatusChanged(Long ticketId, Long customerId, TicketStatus previousStatus,
        TicketStatus status) {
}
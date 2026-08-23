package solutis.lucas.afonso.helpdesk.events;

import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;

public record TicketCreated(Long ticketId, Long customerId, String title, TicketPriority priority, TicketStatus status, TicketCategory category) implements Event{

}
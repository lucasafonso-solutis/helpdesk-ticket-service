package solutis.lucas.afonso.helpdesk.events;

public record TicketAssigned(Long ticketId, Long customerId, Long technicianId) implements Event {

}

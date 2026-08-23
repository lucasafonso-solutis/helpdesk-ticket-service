package solutis.lucas.afonso.helpdesk.events;

public record TechnicianAssignmentEvent(Long ticketId, Long technicianId, Long custumerId) implements Event{

}

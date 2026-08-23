package solutis.lucas.afonso.helpdesk.events;

public record TechnicianAssignmentResult(Long ticketId, Long technicianId, Boolean accepted) implements Event {

}

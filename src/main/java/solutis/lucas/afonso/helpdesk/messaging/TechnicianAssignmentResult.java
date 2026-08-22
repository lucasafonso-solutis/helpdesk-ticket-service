package solutis.lucas.afonso.helpdesk.messaging;

public record TechnicianAssignmentResult(Long ticketId, Long technicianId, boolean accepted) {
}
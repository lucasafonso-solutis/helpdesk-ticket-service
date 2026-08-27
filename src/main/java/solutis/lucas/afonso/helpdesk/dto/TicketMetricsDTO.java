package solutis.lucas.afonso.helpdesk.dto;

public record TicketMetricsDTO(
        long total,
        long open,
        long inProgress,
        long resolved,
        long critical) {
}

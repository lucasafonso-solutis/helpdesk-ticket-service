package solutis.lucas.afonso.helpdesk.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;

public final class TicketSpecification {

    private TicketSpecification() {

    }

    public static Specification<Ticket> withFilters(String search, TicketStatus status, TicketPriority priority, TicketCategory category,
                                                        Long customerId, Long technicianId, LocalDateTime createdFrom, LocalDateTime createdTo,
                                                        boolean sortByPriorityDescending) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")), searchPattern);
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.or(titleMatch, descriptionMatch));
            }
            if (status != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("ticketStatus"), status));
            }
            if (priority != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("priority"), priority));
            }
            if (category != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("ticketCategory"), category));
            }
            if (customerId != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("customerId"), customerId));
            }
            if (technicianId != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.equal(root.get("technicianId"), technicianId));
            }
            if (createdFrom != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicate = criteriaBuilder.and(predicate,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            if (sortByPriorityDescending) {
                query.orderBy(criteriaBuilder.desc(criteriaBuilder.selectCase(root.get("priority"))
                        .when(TicketPriority.CRITICAL, 4)
                        .when(TicketPriority.HIGH, 3)
                        .when(TicketPriority.MEDIUM, 2)
                        .when(TicketPriority.LOW, 1)
                        .otherwise(0)));
            }

            return predicate;
        };
    }
}

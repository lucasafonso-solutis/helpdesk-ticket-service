package solutis.lucas.afonso.helpdesk.controllers;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.dto.TicketMetricsDTO;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.services.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Create Ticket", description = "Create Ticket")
    @ApiResponse(responseCode = "201", description = "Create Ticket")
    @PreAuthorize("hasAnyRole('CLIENT', 'TECHNICIAN', 'ADMIN')")
    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(@Valid @RequestBody TicketDTO ticketDTO, UriComponentsBuilder uriComponentsBuilder) {
        TicketDTO ticket = this.ticketService.create(ticketDTO);
        URI uri = uriComponentsBuilder.path("/tickets/{id}").buildAndExpand(ticket.id()).toUri();

        return ResponseEntity.created(uri).body(ticket);
    }

    @Operation(summary = "List Ticket by ID", description = "List Ticket By ID")
    @ApiResponse(responseCode = "200", description = "List Ticket By ID")
    @PreAuthorize("hasAnyRole('CLIENT', 'TECHNICIAN', 'ADMIN')")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TicketDTO> findById(@PathVariable Long id) {
        return ticketService.listById(id)
                .stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "List All Tickets", description = "List All Tickets")
    @ApiResponse(responseCode = "200", description = "List All Tickets")
    @GetMapping
    public Page<TicketDTO> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo, Pageable pageable) {
        return this.ticketService.list(
                search, status, priority, category, customerId, technicianId,
                createdFrom, createdTo, pageable);
    }

    @Operation(summary = "Ticket metrics", description = "Return ticket metrics without pagination")
    @ApiResponse(responseCode = "200", description = "Ticket metrics")
    @GetMapping("/metrics")
    public TicketMetricsDTO metrics(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long technicianId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        return this.ticketService.metrics(search, category, customerId, technicianId, createdFrom, createdTo);
    }

    @Operation(summary = "Search Ticket By Title", description = "Search Ticket By Title")
    @ApiResponse(responseCode = "200", description = "Search Ticket By Title")
    @GetMapping("/search/{title}")
    public List<TicketDTO> searchByTitle(@PathVariable String title) {
        return this.ticketService.searchByTitle(title);
    }

    @Operation(summary = "Filter Tickets", description = "Filter Tickets")
    @ApiResponse(responseCode = "200", description = "Filter Tickets")
    @GetMapping("/filter")
    public List<TicketDTO> filterTickets(
            @RequestParam(required = false) TicketStatus ticketStatus,
            @RequestParam(required = false) TicketPriority ticketPriority,
            @RequestParam(required = false) TicketCategory ticketCategory) {
        return this.ticketService.filterTickets(ticketStatus, ticketPriority, ticketCategory);
    }

    @Operation(summary = "Update Ticket", description = "Update Ticket")
    @ApiResponse(responseCode = "200", description = "Update Ticket")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<TicketDTO> updateTicket(
            @PathVariable Long id,
            @RequestParam(required = false) TicketPriority ticketPriority,
            @RequestParam(required = false) TicketCategory ticketCategory,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TicketStatus ticketStatus) {
        TicketDTO updatedTicket = this.ticketService.updateTicket(
                id, ticketPriority, ticketCategory, description, ticketStatus);

        return ResponseEntity.ok(updatedTicket);
    }

    @Operation(summary = "Assign Technician", description = "Assign Technician to Ticket")
    @ApiResponse(responseCode = "200", description = "Technician assigned successfully")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/technician/{technicianId}")
    public ResponseEntity<TicketDTO> assignTechnician(@PathVariable Long id, @PathVariable Long technicianId) {
        TicketDTO updatedTicket = this.ticketService.assignTechnician(id, technicianId);

        return ResponseEntity.ok(updatedTicket);
    }

    @Operation(summary = "Close Ticket", description = "Close Ticket")
    @ApiResponse(responseCode = "200", description = "Close Ticket")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<TicketDTO> closeTicket(@PathVariable Long id) {
        TicketDTO updatedTicket = this.ticketService.closeTicket(id);

        return ResponseEntity.ok(updatedTicket);
    }
}

package solutis.lucas.afonso.helpdesk.controllers;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.services.TicketService;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Create Ticket", description = "Create Ticket")
    @ApiResponse(responseCode = "200", description = "Create Ticket")
    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(@Valid @RequestBody TicketDTO ticketDTO,
        UriComponentsBuilder uriComponentsBuilder) {
        TicketDTO ticket = this.ticketService.create(ticketDTO);
        URI uri = uriComponentsBuilder.path("/tickets/{id}").buildAndExpand(ticket.id()).toUri();

        return ResponseEntity.created(uri).body(ticket);
    }

    @Operation(summary = "List Ticket by ID", description = "List Ticket By ID")
    @ApiResponse(responseCode = "200", description = "List Ticket By ID")
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> findById(@PathVariable Long id) {
        return ticketService.listById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "List All Tickets", description = "List All Tickets")
    @ApiResponse(responseCode = "200", description = "List All Tickets")
    @GetMapping
    public List<TicketDTO> list() {
        return this.ticketService.list();
    }
}

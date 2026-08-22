package solutis.lucas.afonso.helpdesk.services;

import java.util.List;

import org.springframework.stereotype.Service;

import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.repository.TicketRepository;

@Service
public class TicketService {
    private TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public TicketDTO create(TicketDTO ticketDTO) {
        Ticket ticket = new Ticket(ticketDTO);
        ticket = ticketRepository.save(ticket);

        return new TicketDTO(ticket);
    }

    public List<TicketDTO> listById(Long id) {
        return this.ticketRepository.findById(id).stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> list() {
        return this.ticketRepository.findAll().stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> searchByTitle(String title) {
        return this.ticketRepository.findByTitle(title).stream().map(TicketDTO::new).toList();
    }

    public List<TicketDTO> filterTickets(TicketStatus ticketStatus, TicketPriority ticketPriority, TicketCategory ticketCategory) {
        return this.ticketRepository.findAll().stream()
            .filter(ticket -> ticketPriority == null || ticket.getPriority() == ticketPriority)
            .filter(ticket -> ticketStatus == null || ticket.getTicketStatus() == ticketStatus)
            .filter(ticket -> ticketCategory == null || ticket.getTicketCategory() == ticketCategory)
            .map(TicketDTO::new)
            .toList();
    }


}

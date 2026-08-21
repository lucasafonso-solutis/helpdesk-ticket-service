package solutis.lucas.afonso.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import solutis.lucas.afonso.helpdesk.entities.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
    
}

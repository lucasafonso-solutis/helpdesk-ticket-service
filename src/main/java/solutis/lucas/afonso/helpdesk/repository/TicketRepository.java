package solutis.lucas.afonso.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import solutis.lucas.afonso.helpdesk.entities.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
	public Optional<Ticket> findByTitle(String title);
}

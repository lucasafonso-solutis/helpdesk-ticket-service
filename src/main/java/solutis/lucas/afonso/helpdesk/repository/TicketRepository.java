package solutis.lucas.afonso.helpdesk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import solutis.lucas.afonso.helpdesk.entities.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>{
	public List<Ticket> findByTitle(String title);
	Page<Ticket> findAll(Pageable pageable);
	Page<Ticket> findByTechnicianId(Long technicianId, Pageable pageable);
}

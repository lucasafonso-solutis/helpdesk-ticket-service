package solutis.lucas.afonso.helpdesk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.entities.Ticket;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;

    private TicketService ticketService;

    private final String exchange = "helpdesk.exchange";
    private final String routingKey = "technician.assignment";
    private final String ticketAssignedRoutingKey = "ticket.assigned";
    private final String ticketCreatedRoutingKey = "ticket.created";
    private final String ticketStatusChangedRoutingKey = "ticket.status-changed";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ticketService = new TicketService(
                ticketRepository,
                rabbitTemplate,
                objectMapper,
                exchange,
                routingKey,
                ticketAssignedRoutingKey,
                ticketCreatedRoutingKey,
                ticketStatusChangedRoutingKey
        );
    }

    @Test
    @DisplayName("Should create a ticket successfully and publish event")
    void shouldCreateTicketSuccessfully() {
        TicketDTO inputDto = new TicketDTO(null, "Problema no computador", "Não liga",
                TicketPriority.HIGH, TicketStatus.OPEN, TicketCategory.HARDWARE, 1L, null, null, null);

        Ticket savedTicket = new Ticket(inputDto);
        savedTicket.setId(10L);

        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        TicketDTO result = ticketService.create(inputDto);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("Problema no computador");
        assertThat(result.ticketStatus()).isEqualTo(TicketStatus.OPEN);

        verify(ticketRepository).save(any(Ticket.class));
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(ticketCreatedRoutingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should list ticket by ID when exists")
    void shouldListByIdWhenExists() {
        Ticket ticket = createSampleTicket(1L, "Suporte de Rede", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        List<TicketDTO> results = ticketService.listById(1L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
        assertThat(results.get(0).title()).isEqualTo("Suporte de Rede");
    }

    @Test
    @DisplayName("Should return empty list when searching by non-existing ID")
    void shouldReturnEmptyListWhenIdNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        List<TicketDTO> results = ticketService.listById(99L);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should update ticket priority, category and description")
    void shouldUpdateTicketSuccessfully() {
        Ticket existingTicket = createSampleTicket(1L, "Título Original", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(existingTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TicketDTO updated = ticketService.updateTicket(1L, TicketPriority.CRITICAL, TicketCategory.SOFTWARE, "Nova descrição", TicketStatus.IN_PROGRESS);
        assertThat(updated.priority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(updated.ticketCategory()).isEqualTo(TicketCategory.SOFTWARE);
        assertThat(updated.description()).isEqualTo("Nova descrição");
        assertThat(updated.ticketStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(ticketStatusChangedRoutingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent ticket")
    void shouldThrowExceptionWhenUpdatingNonExistentTicket() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ticketService.updateTicket(99L, TicketPriority.HIGH, null, "desc", null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ticket not found: 99");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when description is blank on update")
    void shouldThrowExceptionWhenDescriptionIsBlank() {
        Ticket ticket = createSampleTicket(1L, "Título", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        assertThatThrownBy(() -> ticketService.updateTicket(1L, null, null, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description can't be blank");
    }

    @Test
    @DisplayName("Should assign technician successfully and publish event")
    void shouldAssignTechnicianSuccessfully() {
        Ticket ticket = createSampleTicket(1L, "Sem técnico", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        TicketDTO result = ticketService.assignTechnician(1L, 5L);
        assertThat(result).isNotNull();
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(routingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when technicianId is null")
    void shouldThrowExceptionWhenTechnicianIdIsNull() {
        Ticket ticket = createSampleTicket(1L, "Título", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        assertThatThrownBy(() -> ticketService.assignTechnician(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("technicianId is required");
    }

    @Test
    @DisplayName("Should close ticket and publish status changed event")
    void shouldCloseTicketSuccessfully() {
        Ticket ticket = createSampleTicket(1L, "Chamado Aberto", TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TicketDTO result = ticketService.closeTicket(1L);
        assertThat(result.ticketStatus()).isEqualTo(TicketStatus.CLOSED);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(ticketStatusChangedRoutingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should not publish status changed event when ticket is already CLOSED")
    void shouldNotPublishEventWhenAlreadyClosed() {
        Ticket ticket = createSampleTicket(1L, "Chamado Fechado", TicketStatus.CLOSED);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TicketDTO result = ticketService.closeTicket(1L);
        assertThat(result.ticketStatus()).isEqualTo(TicketStatus.CLOSED);
        verify(rabbitTemplate, never()).convertAndSend(eq(exchange), eq(ticketStatusChangedRoutingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should handle accepted technician assignment result message")
    void shouldHandleAcceptedTechnicianAssignmentResult() throws Exception {
        String eventJson = """
                {
                    "ticketId": 1,
                    "technicianId": 5,
                    "accepted": true
                }
                """;

        Ticket ticket = createSampleTicket(1L, "Chamado pendente", TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ticketService.handleTechnicianAssignmentResult(eventJson);
        verify(ticketRepository).save(ticket);
        assertThat(ticket.getTechnicianId()).isEqualTo(5L);
        verify(rabbitTemplate).convertAndSend(eq(exchange), eq(ticketAssignedRoutingKey), any(String.class), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("Should ignore rejected technician assignment result message")
    void shouldIgnoreRejectedTechnicianAssignmentResult() throws Exception {
        String eventJson = """
                {
                    "ticketId": 1,
                    "technicianId": 5,
                    "accepted": false
                }
                """;

        ticketService.handleTechnicianAssignmentResult(eventJson);

        verify(ticketRepository, never()).findById(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AmqpRejectAndDontRequeueException on invalid JSON in listener")
    void shouldThrowAmqpExceptionOnInvalidJson() {
        String invalidJson = "{ invalid json }";
        assertThatThrownBy(() -> ticketService.handleTechnicianAssignmentResult(invalidJson))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private Ticket createSampleTicket(Long id, String title, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTitle(title);
        ticket.setDescription("Descrição do chamado");
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setTicketStatus(status);
        ticket.setTicketCategory(TicketCategory.HARDWARE);
        ticket.setCustomerId(100L);
        return ticket;
    }
}

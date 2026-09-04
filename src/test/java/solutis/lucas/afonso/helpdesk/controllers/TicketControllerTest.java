package solutis.lucas.afonso.helpdesk.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import solutis.lucas.afonso.helpdesk.dto.TicketDTO;
import solutis.lucas.afonso.helpdesk.entities.TicketCategory;
import solutis.lucas.afonso.helpdesk.entities.TicketPriority;
import solutis.lucas.afonso.helpdesk.entities.TicketStatus;
import solutis.lucas.afonso.helpdesk.services.TicketService;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {
    private MockMvc mockMvc;
    @Mock
    private TicketService ticketService;
    @InjectMocks
    private TicketController ticketController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /tickets - Should create ticket and return 201 Created with Location header")
    void shouldCreateTicketAndReturn201() throws Exception {
        TicketDTO inputDto = new TicketDTO(null, "Impressora travada", "Papel atolado",
                TicketPriority.HIGH, TicketStatus.OPEN, TicketCategory.HARDWARE, 10L, null, null, null);

        TicketDTO createdDto = new TicketDTO(1L, "Impressora travada", "Papel atolado",
                TicketPriority.HIGH, TicketStatus.OPEN, TicketCategory.HARDWARE, 10L, null, null, null);

        when(ticketService.create(any(TicketDTO.class))).thenReturn(createdDto);
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/tickets/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Impressora travada"));

        verify(ticketService).create(any(TicketDTO.class));
    }

    @Test
    @DisplayName("GET /tickets/{id} - Should return 200 OK when ticket exists")
    void shouldReturn200WhenTicketExists() throws Exception {
        TicketDTO ticket = new TicketDTO(1L, "Problema de login", "Erro no acesso",
                TicketPriority.MEDIUM, TicketStatus.OPEN, TicketCategory.SOFTWARE, 20L, null, null, null);

        when(ticketService.listById(1L)).thenReturn(List.of(ticket));
        mockMvc.perform(get("/tickets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Problema de login"));
    }

    @Test
    @DisplayName("GET /tickets/{id} - Should return 404 Not Found when ticket does not exist")
    void shouldReturn404WhenTicketNotFound() throws Exception {
        when(ticketService.listById(99L)).thenReturn(List.of());
        mockMvc.perform(get("/tickets/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets/search/{title} - Should return tickets matching title")
    void shouldSearchByTitle() throws Exception {
        TicketDTO ticket = new TicketDTO(1L, "Rede sem sinal", "Wifi caindo",
                TicketPriority.HIGH, TicketStatus.OPEN, TicketCategory.NETWORK, 30L, null, null, null);

        when(ticketService.searchByTitle("Rede")).thenReturn(List.of(ticket));

        mockMvc.perform(get("/tickets/search/Rede"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Rede sem sinal"));
    }
}

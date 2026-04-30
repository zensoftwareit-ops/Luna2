package it.zensoftware.luna2.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.PagamentoDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Pagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests per PagamentiController con MockMvc
 * Testa HTTP layer (request/response, validation, status codes)
 */
@WebMvcTest(PagamentiController.class)
@DisplayName("PagamentiController MockMvc Tests")
public class PagamentiControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagamentoDAO pagamentoDAO;

    @MockBean
    private FatturaDAO fatturaDAO;

    private Pagamento mockPagamento;
    private Fattura mockFattura;
    private PagamentiController.PagamentoDTO mockPagamentoDTO;

    @BeforeEach
    void setUp() {
        // Setup mock Fattura
        mockFattura = new Fattura();
        mockFattura.setId(1L);
        mockFattura.setNumero("2026/001");
        mockFattura.setTotale(new BigDecimal("1000.00"));

        // Setup mock Pagamento
        mockPagamento = new Pagamento();
        mockPagamento.setId(1L);
        mockPagamento.setFattura(mockFattura);
        mockPagamento.setImporto(new BigDecimal("1000.00"));
        mockPagamento.setDataPagamento(new Date());
        mockPagamento.setMetodoPagamento(Pagamento.MetodoPagamento.BONIFICO);
        mockPagamento.setRiferimento("REF-001");
        mockPagamento.setRiconciliato(false);

        // Setup mock DTO
        mockPagamentoDTO = new PagamentiController.PagamentoDTO(
            1L,
            "2026/001",
            new BigDecimal("1000.00"),
            LocalDate.now(),
            Pagamento.MetodoPagamento.BONIFICO,
            "REF-001",
            false,
            null
        );
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("GET /api/v1/pagamenti - List with paging")
    void testListPagamenti() throws Exception {
        List<Pagamento> pagamenti = new ArrayList<>();
        pagamenti.add(mockPagamento);

        when(pagamentoDAO.findAll()).thenReturn(pagamenti);

        mockMvc.perform(get("/api/v1/pagamenti")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti - Empty list")
    void testListPagamentiEmpty() throws Exception {
        when(pagamentoDAO.findAll()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/pagamenti")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/{id} - Get by ID")
    void testGetPagamentoById() throws Exception {
        when(pagamentoDAO.findById(1L)).thenReturn(mockPagamento);

        mockMvc.perform(get("/api/v1/pagamenti/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importo", equalTo(1000.00)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/{id} - Not Found")
    void testGetPagamentoByIdNotFound() throws Exception {
        when(pagamentoDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/pagamenti/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Create pagamento")
    void testCreatePagamento() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("500.00");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.BONIFICO;

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Validation error (negative amount)")
    void testCreatePagamentoNegativeAmount() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("-100.00");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.BONIFICO;

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Validation error (zero amount)")
    void testCreatePagamentoZeroAmount() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = BigDecimal.ZERO;
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.BONIFICO;

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/pagamenti/{id} - Update pagamento")
    void testUpdatePagamento() throws Exception {
        PagamentiController.PagamentoDTO updateRequest = new PagamentiController.PagamentoDTO();
        updateRequest.importo = new BigDecimal("2000.00");
        updateRequest.metodo = Pagamento.MetodoPagamento.ASSEGNO;

        when(pagamentoDAO.findById(1L)).thenReturn(mockPagamento);
        doNothing().when(pagamentoDAO).update(any(Pagamento.class));
        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});

        mockMvc.perform(put("/api/v1/pagamenti/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/pagamenti/{id} - Update non-existent")
    void testUpdatePagamentoNotFound() throws Exception {
        PagamentiController.PagamentoDTO updateRequest = new PagamentiController.PagamentoDTO();

        when(pagamentoDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/pagamenti/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/pagamenti/{id} - Delete pagamento")
    void testDeletePagamento() throws Exception {
        when(pagamentoDAO.findById(1L)).thenReturn(mockPagamento);
        doNothing().when(pagamentoDAO).delete(mockPagamento);
        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});

        mockMvc.perform(delete("/api/v1/pagamenti/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/pagamenti/{id} - Delete non-existent")
    void testDeletePagamentoNotFound() throws Exception {
        when(pagamentoDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/pagamenti/999"))
            .andExpect(status().isNotFound());
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("GET /api/v1/pagamenti/search - Search by numero fattura")
    void testSearchPagamentoByNumeroFattura() throws Exception {
        List<Pagamento> results = new ArrayList<>();
        results.add(mockPagamento);

        when(pagamentoDAO.findByNumeroFattura("2026/001")).thenReturn(results);

        mockMvc.perform(get("/api/v1/pagamenti/search")
                .param("numeroFattura", "2026/001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/search - Search by metodo")
    void testSearchPagamentoByMetodo() throws Exception {
        List<Pagamento> results = new ArrayList<>();
        results.add(mockPagamento);

        when(pagamentoDAO.findByMetodo(Pagamento.MetodoPagamento.BONIFICO)).thenReturn(results);

        mockMvc.perform(get("/api/v1/pagamenti/search")
                .param("metodo", "BONIFICO")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/search - Search by riferimento")
    void testSearchPagamentoByRiferimento() throws Exception {
        List<Pagamento> results = new ArrayList<>();
        results.add(mockPagamento);

        when(pagamentoDAO.findByRiferimento("REF-001")).thenReturn(results);

        mockMvc.perform(get("/api/v1/pagamenti/search")
                .param("riferimento", "REF-001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/search - Invalid metodo")
    void testSearchPagamentoInvalidMetodo() throws Exception {
        mockMvc.perform(get("/api/v1/pagamenti/search")
                .param("metodo", "INVALID_METHOD")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // ==================== RECONCILIATION TESTS ====================

    @Test
    @DisplayName("PUT /api/v1/pagamenti/{id}/riconcilia - Mark as reconciled")
    void testRiconciliaPagamento() throws Exception {
        when(pagamentoDAO.findById(1L)).thenReturn(mockPagamento);
        doNothing().when(pagamentoDAO).update(any(Pagamento.class));

        mockMvc.perform(put("/api/v1/pagamenti/1/riconcilia"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/pagamenti/{id}/riconcilia - Non-existent payment")
    void testRiconciliaPagamentoNotFound() throws Exception {
        when(pagamentoDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/pagamenti/999/riconcilia"))
            .andExpect(status().isNotFound());
    }

    // ==================== PAYMENT METHOD TESTS ====================

    @Test
    @DisplayName("POST /api/v1/pagamenti - Create with CARTACREDITO")
    void testCreatePagamentoCartaCredito() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("500.00");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.CARTACREDITO;

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Create with ASSEGNO")
    void testCreatePagamentoAssegno() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("500.00");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.ASSEGNO;
        createRequest.riferimento = "CHK-12345";

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Create with CONTANTI")
    void testCreatePagamentoContanti() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("500.00");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.CONTANTI;

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    // ==================== LARGE AMOUNT TESTS ====================

    @Test
    @DisplayName("POST /api/v1/pagamenti - Very large amount")
    void testCreatePagamentoLargeAmount() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("999999999.99");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.BONIFICO;

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/pagamenti - Very small amount")
    void testCreatePagamentoSmallAmount() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";
        createRequest.importo = new BigDecimal("0.01");
        createRequest.dataPagamento = LocalDate.now();
        createRequest.metodo = Pagamento.MetodoPagamento.BONIFICO;

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(new ArrayList<>() {{
            add(mockFattura);
        }});
        when(pagamentoDAO.save(any(Pagamento.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/pagamenti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("POST /api/v1/pagamenti - Missing content type")
    void testCreateWithoutContentType() throws Exception {
        PagamentiController.PagamentoDTO createRequest = new PagamentiController.PagamentoDTO();
        createRequest.numeroFattura = "2026/001";

        mockMvc.perform(post("/api/v1/pagamenti")
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti - Invalid page parameter")
    void testListWithInvalidPage() throws Exception {
        mockMvc.perform(get("/api/v1/pagamenti")
                .param("page", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/pagamenti/search - All parameters empty")
    void testSearchWithAllParametersEmpty() throws Exception {
        List<Pagamento> allPagamenti = new ArrayList<>();
        allPagamenti.add(mockPagamento);

        when(pagamentoDAO.findAll()).thenReturn(allPagamenti);

        mockMvc.perform(get("/api/v1/pagamenti/search")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}

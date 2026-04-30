package it.zensoftware.luna2.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.api.service.PdfService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration Tests per FattureController con MockMvc
 * Testa HTTP layer (request/response, validation, status codes)
 */
@WebMvcTest(FattureController.class)
@DisplayName("FattureController MockMvc Tests")
public class FattureControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FatturaDAO fatturaDAO;

    @MockBean
    private ClienteDAO clienteDAO;

    @MockBean
    private PdfService pdfService;

    private Fattura mockFattura;
    private FattureController.FatturaDTO mockFatturaDTO;
    private Cliente mockCliente;

    @BeforeEach
    void setUp() {
        // Setup mock Cliente
        mockCliente = new Cliente();
        mockCliente.setId(1L);
        mockCliente.setRagioneSociale("Test SPA");
        mockCliente.setPartitaIva("12345678901");
        mockCliente.setEmail("test@company.it");

        // Setup mock Fattura
        mockFattura = new Fattura();
        mockFattura.setId(1L);
        mockFattura.setNumero("2026/001");
        mockFattura.setTotale(new BigDecimal("1000.00"));
        mockFattura.setCliente(mockCliente);
        mockFattura.setTipoFattura(Fattura.TipoFattura.REALE);
        mockFattura.setStato(Fattura.StatoPagamento.DA_PAGARE);

        // Setup mock DTO
        mockFatturaDTO = new FattureController.FatturaDTO(
            1L, "2026/001", 1L, "Test SPA",
            Fattura.TipoFattura.REALE,
            Fattura.StatoPagamento.DA_PAGARE,
            new BigDecimal("1000.00"),
            LocalDate.now(),
            null
        );
    }

    @Test
    @DisplayName("GET /api/v1/fatture - List with paging")
    void testListFatture() throws Exception {
        List<Fattura> fatture = new ArrayList<>();
        fatture.add(mockFattura);

        when(fatturaDAO.findByAnno(any())).thenReturn(fatture);

        mockMvc.perform(get("/api/v1/fatture")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].numero", equalTo("2026/001")));
    }

    @Test
    @DisplayName("GET /api/v1/fatture/{id} - Get by ID")
    void testGetFatturaById() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);

        mockMvc.perform(get("/api/v1/fatture/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero", equalTo("2026/001")))
            .andExpect(jsonPath("$.importo", equalTo(1000.00)));
    }

    @Test
    @DisplayName("GET /api/v1/fatture/{id} - Not Found")
    void testGetFatturaByIdNotFound() throws Exception {
        when(fatturaDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/fatture/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/fatture - Create fattura")
    void testCreateFattura() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/002";
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.importo = new BigDecimal("500.00");
        createRequest.dataEmissione = LocalDate.now();

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        when(fatturaDAO.save(any(Fattura.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numero", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/fatture - Validation error (missing numero)")
    void testCreateFatturaValidationError() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = ""; // Empty numero - violates @NotBlank
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.importo = new BigDecimal("500.00");

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", equalTo(400)))
            .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("PUT /api/v1/fatture/{id} - Update fattura")
    void testUpdateFattura() throws Exception {
        FattureController.FatturaDTO updateRequest = new FattureController.FatturaDTO();
        updateRequest.numero = "2026/001-MOD";
        updateRequest.importo = new BigDecimal("2000.00");
        updateRequest.tipo = Fattura.TipoFattura.REALE;

        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        doNothing().when(fatturaDAO).update(any(Fattura.class));

        mockMvc.perform(put("/api/v1/fatture/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numero", notNullValue()));
    }

    @Test
    @DisplayName("DELETE /api/v1/fatture/{id} - Delete fattura")
    void testDeleteFattura() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        doNothing().when(fatturaDAO).delete(mockFattura);

        mockMvc.perform(delete("/api/v1/fatture/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/fatture/search - Search by numero")
    void testSearchFatturaByNumero() throws Exception {
        List<Fattura> results = new ArrayList<>();
        results.add(mockFattura);

        when(fatturaDAO.findByProperty("numero", "2026/001")).thenReturn(results);

        mockMvc.perform(get("/api/v1/fatture/search")
                .param("numero", "2026/001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].numero", equalTo("2026/001")));
    }

    @Test
    @DisplayName("GET /api/v1/fatture/{id}/pdf - Export PDF")
    void testExportPdf() throws Exception {
        byte[] pdfBytes = "PDF content mock".getBytes();

        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        when(pdfService.generateFatturaPdf(mockFattura)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/fatture/1/pdf"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(content().bytes(pdfBytes));
    }

    @Test
    @DisplayName("PUT /api/v1/fatture/{id}/stato - Update payment status")
    void testUpdateStatoPagamento() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        doNothing().when(fatturaDAO).update(any(Fattura.class));

        mockMvc.perform(put("/api/v1/fatture/1/stato")
                .param("stato", "PAGATA")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stato", notNullValue()));
    }

    @Test
    @DisplayName("POST - Validation error (negative importo)")
    void testValidationNegativeImporto() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/999";
        createRequest.importo = new BigDecimal("-100.00"); // Violates @DecimalMin
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.dataEmissione = LocalDate.now();

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", equalTo(400)));
    }

    // ==================== ADDITIONAL CRUD TESTS ====================

    @Test
    @DisplayName("GET /api/v1/fatture - Empty list")
    void testListFattureEmpty() throws Exception {
        when(fatturaDAO.findByAnno(any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/fatture")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/fatture - Multiple pages")
    void testListFatturePagination() throws Exception {
        List<Fattura> fatture = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Fattura f = new Fattura();
            f.setId((long) i);
            f.setNumero("2026/00" + i);
            f.setTotale(new BigDecimal("100.00"));
            fatture.add(f);
        }

        when(fatturaDAO.findByAnno(any())).thenReturn(fatture);

        mockMvc.perform(get("/api/v1/fatture")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("PUT /api/v1/fatture/{id} - Update non-existent")
    void testUpdateFatturaNotFound() throws Exception {
        FattureController.FatturaDTO updateRequest = new FattureController.FatturaDTO();
        updateRequest.numero = "2026/999";

        when(fatturaDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/fatture/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/fatture/{id} - Delete non-existent")
    void testDeleteFatturaNotFound() throws Exception {
        when(fatturaDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/fatture/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/fatture/search - Multiple results")
    void testSearchFatturaMultipleResults() throws Exception {
        List<Fattura> results = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Fattura f = new Fattura();
            f.setId((long) i);
            f.setNumero("2026/00" + i);
            f.setTotale(new BigDecimal("100.00"));
            results.add(f);
        }

        when(fatturaDAO.findByProperty("numero", "2026")).thenReturn(results);

        mockMvc.perform(get("/api/v1/fatture/search")
                .param("numero", "2026")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("GET /api/v1/fatture/search - No results")
    void testSearchFatturaNoResults() throws Exception {
        when(fatturaDAO.findByProperty("numero", "NONEXISTENT")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/fatture/search")
                .param("numero", "NONEXISTENT")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/v1/fatture - Create without cliente")
    void testCreateFatturaWithoutCliente() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/003";
        createRequest.clienteId = 999L;
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.importo = new BigDecimal("500.00");

        when(clienteDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest());
    }

    // ==================== INVOICE TYPE TESTS ====================

    @Test
    @DisplayName("POST /api/v1/fatture - Create proforma invoice")
    void testCreateProformaInvoice() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/PROFORMA";
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.PROFORMA;
        createRequest.importo = new BigDecimal("1000.00");
        createRequest.dataEmissione = LocalDate.now();

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        when(fatturaDAO.save(any(Fattura.class))).thenReturn(10L);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/fatture - Create credit note")
    void testCreateCreditNote() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/CN001";
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.NOTA_CREDITO;
        createRequest.importo = new BigDecimal("-500.00");
        createRequest.dataEmissione = LocalDate.now();

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        when(fatturaDAO.save(any(Fattura.class))).thenReturn(11L);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    // ==================== PDF EXPORT TESTS ====================

    @Test
    @DisplayName("GET /api/v1/fatture/{id}/pdf - PDF not found")
    void testExportPdfNotFound() throws Exception {
        when(fatturaDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/fatture/999/pdf"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/fatture/{id}/pdf - PDF generation error")
    void testExportPdfGenerationError() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        when(pdfService.generateFatturaPdf(mockFattura)).thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(get("/api/v1/fatture/1/pdf"))
            .andExpect(status().isInternalServerError());
    }

    // ==================== PAYMENT STATUS TESTS ====================

    @Test
    @DisplayName("PUT /api/v1/fatture/{id}/stato - Update to PAGATA")
    void testUpdateStatoToPagata() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        doNothing().when(fatturaDAO).update(any(Fattura.class));

        mockMvc.perform(put("/api/v1/fatture/1/stato")
                .param("stato", "PAGATA")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/fatture/{id}/stato - Update to PARZIALMENTE_PAGATA")
    void testUpdateStatoToParzialmentePagata() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);
        doNothing().when(fatturaDAO).update(any(Fattura.class));

        mockMvc.perform(put("/api/v1/fatture/1/stato")
                .param("stato", "PARZIALMENTE_PAGATA")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/fatture/{id}/stato - Invalid status")
    void testUpdateStatoInvalid() throws Exception {
        when(fatturaDAO.findById(1L)).thenReturn(mockFattura);

        mockMvc.perform(put("/api/v1/fatture/1/stato")
                .param("stato", "INVALID_STATUS")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    // ==================== LARGE DATA TESTS ====================

    @Test
    @DisplayName("POST /api/v1/fatture - Very large amount")
    void testCreateFatturaWithLargeAmount() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/LARGE";
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.importo = new BigDecimal("999999999.99");
        createRequest.dataEmissione = LocalDate.now();

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        when(fatturaDAO.save(any(Fattura.class))).thenReturn(12L);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/fatture - Very small amount")
    void testCreateFatturaWithSmallAmount() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/SMALL";
        createRequest.clienteId = 1L;
        createRequest.tipo = Fattura.TipoFattura.REALE;
        createRequest.importo = new BigDecimal("0.01");
        createRequest.dataEmissione = LocalDate.now();

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        when(fatturaDAO.save(any(Fattura.class))).thenReturn(13L);

        mockMvc.perform(post("/api/v1/fatture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @DisplayName("POST /api/v1/fatture - Missing content type")
    void testCreateWithoutContentType() throws Exception {
        FattureController.FatturaDTO createRequest = new FattureController.FatturaDTO();
        createRequest.numero = "2026/002";
        createRequest.clienteId = 1L;

        mockMvc.perform(post("/api/v1/fatture")
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("GET /api/v1/fatture - Invalid page parameter")
    void testListWithInvalidPage() throws Exception {
        mockMvc.perform(get("/api/v1/fatture")
                .param("page", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

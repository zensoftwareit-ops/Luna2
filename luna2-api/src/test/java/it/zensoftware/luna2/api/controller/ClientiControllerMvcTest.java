package it.zensoftware.luna2.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests per ClientiController con MockMvc
 * Testa HTTP layer (request/response, validation, status codes)
 */
@WebMvcTest(ClientiController.class)
@DisplayName("ClientiController MockMvc Tests")
public class ClientiControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClienteDAO clienteDAO;

    private Cliente mockCliente;
    private ClientiController.ClienteDTO mockClienteDTO;

    @BeforeEach
    void setUp() {
        // Setup mock Cliente
        mockCliente = new Cliente();
        mockCliente.setId(1L);
        mockCliente.setRagioneSociale("Test SPA");
        mockCliente.setEmail("test@company.it");
        mockCliente.setPartitaIva("12345678901");
        mockCliente.setPaese("Italia");

        // Setup mock DTO
        mockClienteDTO = new ClientiController.ClienteDTO(
            1L,
            "Test SPA",
            "test@company.it",
            "12345678901",
            "Italia"
        );
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("GET /api/v1/clienti - List with paging")
    void testListClienti() throws Exception {
        List<Cliente> clienti = new ArrayList<>();
        clienti.add(mockCliente);

        when(clienteDAO.findAllActive()).thenReturn(clienti);

        mockMvc.perform(get("/api/v1/clienti")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].nome", equalTo("Test SPA")));
    }

    @Test
    @DisplayName("GET /api/v1/clienti - Empty list")
    void testListClientiEmpty() throws Exception {
        when(clienteDAO.findAllActive()).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/clienti")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/clienti/{id} - Get by ID")
    void testGetClienteById() throws Exception {
        when(clienteDAO.findById(1L)).thenReturn(mockCliente);

        mockMvc.perform(get("/api/v1/clienti/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome", equalTo("Test SPA")))
            .andExpect(jsonPath("$.email", equalTo("test@company.it")));
    }

    @Test
    @DisplayName("GET /api/v1/clienti/{id} - Not Found")
    void testGetClienteByIdNotFound() throws Exception {
        when(clienteDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/clienti/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/clienti - Create cliente")
    void testCreateCliente() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "New Company SPA";
        createRequest.email = "new@company.it";
        createRequest.partitaIva = "98765432101";
        createRequest.paese = "Italia";

        when(clienteDAO.save(any(Cliente.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/clienti - Validation error (missing nome)")
    void testCreateClienteValidationError() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = ""; // Empty nome - violates @NotBlank
        createRequest.email = "test@company.it";
        createRequest.partitaIva = "12345678901";

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", equalTo(400)));
    }

    @Test
    @DisplayName("PUT /api/v1/clienti/{id} - Update cliente")
    void testUpdateCliente() throws Exception {
        ClientiController.ClienteDTO updateRequest = new ClientiController.ClienteDTO();
        updateRequest.nome = "Updated SPA";
        updateRequest.email = "updated@company.it";
        updateRequest.partitaIva = "12345678901";

        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        doNothing().when(clienteDAO).update(any(Cliente.class));

        mockMvc.perform(put("/api/v1/clienti/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome", notNullValue()));
    }

    @Test
    @DisplayName("PUT /api/v1/clienti/{id} - Update non-existent")
    void testUpdateClienteNotFound() throws Exception {
        ClientiController.ClienteDTO updateRequest = new ClientiController.ClienteDTO();
        updateRequest.nome = "Updated SPA";

        when(clienteDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/clienti/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/clienti/{id} - Delete cliente")
    void testDeleteCliente() throws Exception {
        when(clienteDAO.findById(1L)).thenReturn(mockCliente);
        doNothing().when(clienteDAO).delete(mockCliente);

        mockMvc.perform(delete("/api/v1/clienti/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/clienti/{id} - Delete non-existent")
    void testDeleteClienteNotFound() throws Exception {
        when(clienteDAO.findById(999L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/clienti/999"))
            .andExpect(status().isNotFound());
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("GET /api/v1/clienti/search - Search by nome")
    void testSearchClienteByNome() throws Exception {
        List<Cliente> results = new ArrayList<>();
        results.add(mockCliente);

        when(clienteDAO.searchByName("Test")).thenReturn(results);

        mockMvc.perform(get("/api/v1/clienti/search")
                .param("nome", "Test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nome", equalTo("Test SPA")));
    }

    @Test
    @DisplayName("GET /api/v1/clienti/search - Search by email")
    void testSearchClienteByEmail() throws Exception {
        List<Cliente> results = new ArrayList<>();
        results.add(mockCliente);

        when(clienteDAO.findByProperty("email", "test@company.it")).thenReturn(results);

        mockMvc.perform(get("/api/v1/clienti/search")
                .param("email", "test@company.it")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/clienti/search - Search by partita IVA")
    void testSearchClienteByPartitaIva() throws Exception {
        when(clienteDAO.findByPartitaIva("12345678901")).thenReturn(mockCliente);

        mockMvc.perform(get("/api/v1/clienti/search")
                .param("partitaIva", "12345678901")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].partitaIva", equalTo("12345678901")));
    }

    @Test
    @DisplayName("GET /api/v1/clienti/search - Search no results")
    void testSearchClienteNoResults() throws Exception {
        when(clienteDAO.findByProperty("email", "nonexistent@company.it")).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/clienti/search")
                .param("email", "nonexistent@company.it")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("POST /api/v1/clienti - Invalid email format")
    void testCreateClienteInvalidEmail() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "Test Company";
        createRequest.email = "invalid-email"; // Invalid email format
        createRequest.partitaIva = "12345678901";

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/clienti - Invalid partita IVA")
    void testCreateClienteInvalidPartitaIva() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "Test Company";
        createRequest.email = "test@company.it";
        createRequest.partitaIva = "1234567890"; // Too short

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isBadRequest());
    }

    // ==================== INPUT SANITIZATION TESTS ====================

    @Test
    @DisplayName("POST /api/v1/clienti - Create with special characters")
    void testCreateClienteWithSpecialCharacters() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "Test & Company <Script>";
        createRequest.email = "test@company.it";
        createRequest.partitaIva = "12345678901";

        when(clienteDAO.save(any(Cliente.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/clienti - Create with HTML tags")
    void testCreateClienteWithHtmlTags() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "<script>alert('xss')</script>";
        createRequest.email = "test@company.it";
        createRequest.partitaIva = "12345678901";

        when(clienteDAO.save(any(Cliente.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("POST /api/v1/clienti - Very long company name")
    void testCreateClienteWithVeryLongName() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "A".repeat(255);
        createRequest.email = "test@company.it";
        createRequest.partitaIva = "12345678901";

        when(clienteDAO.save(any(Cliente.class))).thenReturn(2L);

        mockMvc.perform(post("/api/v1/clienti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/clienti - Large page size")
    void testListClientiWithLargePageSize() throws Exception {
        List<Cliente> clienti = new ArrayList<>();
        when(clienteDAO.findAllActive()).thenReturn(clienti);

        mockMvc.perform(get("/api/v1/clienti")
                .param("page", "0")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/clienti/search - Multiple criteria (first match wins)")
    void testSearchClienteMultipleCriteria() throws Exception {
        when(clienteDAO.findByPartitaIva("12345678901")).thenReturn(mockCliente);

        mockMvc.perform(get("/api/v1/clienti/search")
                .param("partitaIva", "12345678901")
                .param("email", "other@company.it")
                .param("nome", "Other")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/clienti - Missing content type")
    void testCreateWithoutContentType() throws Exception {
        ClientiController.ClienteDTO createRequest = new ClientiController.ClienteDTO();
        createRequest.nome = "Test";

        mockMvc.perform(post("/api/v1/clienti")
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnsupportedMediaType());
    }
}

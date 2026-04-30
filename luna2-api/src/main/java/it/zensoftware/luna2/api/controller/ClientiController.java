package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.api.validator.ValidPartitaIva;
import it.zensoftware.luna2.api.validator.ValidEmail;
import it.zensoftware.luna2.api.validator.ValidItalianPhoneNumber;
import it.zensoftware.luna2.api.security.InputSanitizer;
import it.zensoftware.luna2.api.security.Auditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST API per Clienti.
 * 
 * Endpoint:
 * GET    /api/v1/clienti            - Lista con paginazione
 * GET    /api/v1/clienti/{id}       - Dettaglio cliente
 * POST   /api/v1/clienti            - Crea cliente
 * PUT    /api/v1/clienti/{id}       - Aggiorna cliente
 * DELETE /api/v1/clienti/{id}       - Cancella cliente
 * GET    /api/v1/clienti/search     - Ricerca avanzata
 */
@RestController
@RequestMapping("/api/v1/clienti")
public class ClientiController {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    /**
     * Lista tutti i clienti con paginazione.
     * ?page=0&size=20&sort=id,desc
     */
    @GetMapping
    public ResponseEntity<Page<ClienteDTO>> list(Pageable pageable) {
        List<Cliente> results = clienteDAO.findAllActive();
        List<ClienteDTO> dtos = mapClientiToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    /**
     * Ricerca clienti per nome, email, partita IVA.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ClienteDTO>> search(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String partitaIva) {
        List<Cliente> risultati = new ArrayList<>();

        if (partitaIva != null && !partitaIva.isEmpty()) {
            Cliente cliente = clienteDAO.findByPartitaIva(partitaIva);
            if (cliente != null) {
                risultati.add(cliente);
            }
        } else if (email != null && !email.isEmpty()) {
            risultati = clienteDAO.findByProperty("email", email);
        } else if (nome != null && !nome.isEmpty()) {
            risultati = clienteDAO.searchByName(nome);
        } else {
            risultati = clienteDAO.findAllActive();
        }

        return ResponseEntity.ok(mapClientiToDto(risultati));
    }

    /**
     * Dettaglio cliente by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> getById(@PathVariable Long id) {
        Cliente cliente = clienteDAO.findById(id);
        if (cliente == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(cliente));
    }

    /**
     * Crea nuovo cliente.
     */
    @Auditable(action = "CREATE", entityType = "Cliente")
    @PostMapping
    public ResponseEntity<ClienteDTO> create(@Valid @RequestBody ClienteDTO dto) {
        Cliente cliente = fromDto(dto, null);
        if (cliente.getCodiceCliente() == null || cliente.getCodiceCliente().isEmpty()) {
            cliente.setCodiceCliente(clienteDAO.generateNextCodiceCliente());
        }
        clienteDAO.save(cliente);
        return ResponseEntity.status(201).body(toDto(cliente));
    }

    /**
     * Aggiorna cliente.
     */
    @Auditable(action = "UPDATE", entityType = "Cliente")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> update(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        Cliente existing = clienteDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Cliente updated = fromDto(dto, existing);
        updated.setId(id);
        clienteDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    /**
     * Cancella cliente.
     */
    @Auditable(action = "DELETE", entityType = "Cliente")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Cliente existing = clienteDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        clienteDAO.delete(existing);
        return ResponseEntity.noContent().build();
    }

    private ClienteDTO toDto(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return new ClienteDTO(
            cliente.getId(),
            cliente.getRagioneSociale(),
            cliente.getEmail(),
            cliente.getPartitaIva(),
            cliente.getPaese()
        );
    }

    private List<ClienteDTO> mapClientiToDto(List<Cliente> clienti) {
        if (clienti == null || clienti.isEmpty()) {
            return Collections.emptyList();
        }
        List<ClienteDTO> dtos = new ArrayList<>();
        for (Cliente cliente : clienti) {
            dtos.add(toDto(cliente));
        }
        return dtos;
    }

    private Cliente fromDto(ClienteDTO dto, Cliente existing) {
        Cliente cliente = existing != null ? existing : new Cliente();
        if (dto == null) {
            return cliente;
        }
        if (dto.nome != null) {
            cliente.setRagioneSociale(dto.nome);
        }
        cliente.setEmail(dto.email);
        cliente.setPartitaIva(dto.partitaIva);
        cliente.setPaese(dto.paese != null ? dto.paese : "Italia");
        return cliente;
    }

    private Page<ClienteDTO> paginate(List<ClienteDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<ClienteDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    public static class ClienteDTO {
        public Long id;

        @javax.validation.constraints.NotBlank(message = "Nome cliente non può essere vuoto")
        public String nome;

        @ValidEmail(message = "Email non valida")
        public String email;

        @ValidPartitaIva(message = "Partita IVA non valida")
        public String partitaIva;

        public String paese;

        public ClienteDTO() {}

        public ClienteDTO(Long id, String nome, String email, String partitaIva, String paese) {
            this.id = id;
            this.nome = InputSanitizer.sanitize(nome);
            this.email = InputSanitizer.sanitizeEmail(email);
            this.partitaIva = InputSanitizer.sanitizeNumeric(partitaIva);
            this.paese = InputSanitizer.sanitize(paese);
        }
    }
}

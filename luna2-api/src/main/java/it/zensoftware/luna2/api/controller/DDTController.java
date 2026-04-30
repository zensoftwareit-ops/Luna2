package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.DdtDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.util.HibernateUtil;
import it.zensoftware.luna2.api.service.PdfService;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * REST API per DDT (Documenti di Trasporto).
 *
 * Endpoint:
 * GET    /api/v1/ddt              - Lista con paginazione
 * GET    /api/v1/ddt/{id}         - Dettaglio DDT
 * POST   /api/v1/ddt              - Crea DDT
 * PUT    /api/v1/ddt/{id}         - Aggiorna DDT
 * DELETE /api/v1/ddt/{id}         - Cancella DDT
 * GET    /api/v1/ddt/{id}/pdf     - Esporta PDF
 * GET    /api/v1/ddt/search       - Ricerca avanzata
 */
@RestController
@RequestMapping("/api/v1/ddt")
public class DDTController {

    private final DdtDAO ddtDAO = new DdtDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping
    public ResponseEntity<Page<DDTDto>> list(
            @RequestParam(required = false) Integer anno,
            Pageable pageable) {
        List<Map<String, Object>> results = anno != null ?
            ddtDAO.findByAnno(anno) :
            ddtDAO.findAll();
        List<DDTDto> dtos = mapDDTToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DDTDto>> search(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cliente) {
        List<DDTDto> risultati = new ArrayList<>();

        if (numero != null && !numero.isEmpty()) {
            List<Map<String, Object>> dati = ddtDAO.findByProperty("numero", numero);
            risultati = mapDDTToDto(dati);
        } else if (cliente != null && !cliente.isEmpty()) {
            List<Map<String, Object>> dati = ddtDAO.findByClienteNome(cliente);
            risultati = mapDDTToDto(dati);
        } else {
            List<Map<String, Object>> dati = ddtDAO.findAll();
            risultati = mapDDTToDto(dati);
        }

        return ResponseEntity.ok(risultati);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DDTDto> getById(@PathVariable Long id) {
        Map<String, Object> ddt = ddtDAO.findById(id);
        if (ddt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(ddt));
    }

    @PostMapping
    public ResponseEntity<DDTDto> create(@Valid @RequestBody DDTDto dto) {
        Map<String, Object> ddt = fromDto(dto);
        Long id = ddtDAO.save(ddt);
        ddt.put("id", id);
        return ResponseEntity.status(201).body(toDto(ddt));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DDTDto> update(@PathVariable Long id, @Valid @RequestBody DDTDto dto) {
        Map<String, Object> existing = ddtDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> updated = fromDto(dto);
        updated.put("id", id);
        ddtDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        Map<String, Object> ddt = ddtDAO.findById(id);
        if (ddt == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdfBytes = new PdfService().generateDdtPdf(
                id,
                (String) ddt.get("numero"),
                (Date) ddt.get("data_ddt"),
                null // TODO: clienteDAO.findById(cliente_id)
            );
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"ddt-" + ddt.get("numero") + ".pdf\"")
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Map<String, Object> ddt = ddtDAO.findById(id);
        if (ddt == null) {
            return ResponseEntity.notFound().build();
        }
        ddtDAO.delete(id);
        return ResponseEntity.noContent().build();
    }

    private DDTDto toDto(Map<String, Object> ddt) {
        if (ddt == null) {
            return null;
        }
        return new DDTDto(
            ((Number) ddt.get("id")).longValue(),
            (String) ddt.get("numero"),
            ((Number) ddt.getOrDefault("cliente_id", 0)).longValue(),
            (String) ddt.get("cliente_nome"),
            (Date) ddt.get("data_ddt"),
            (String) ddt.get("causale_trasporto"),
            ((Number) ddt.getOrDefault("numero_colli", 0)).intValue(),
            (String) ddt.get("trasportatore")
        );
    }

    private List<DDTDto> mapDDTToDto(List<Map<String, Object>> ddts) {
        if (ddts == null || ddts.isEmpty()) {
            return Collections.emptyList();
        }
        List<DDTDto> dtos = new ArrayList<>();
        for (Map<String, Object> ddt : ddts) {
            dtos.add(toDto(ddt));
        }
        return dtos;
    }

    private Page<DDTDto> paginate(List<DDTDto> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<DDTDto> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Map<String, Object> fromDto(DDTDto dto) {
        Map<String, Object> ddt = new java.util.LinkedHashMap<>();
        ddt.put("numero", dto.numero);
        ddt.put("cliente_id", dto.clienteId);
        ddt.put("data_ddt", dto.dataDdt);
        ddt.put("causale_trasporto", dto.causaleTrasporto);
        ddt.put("numero_colli", dto.numeroColli);
        ddt.put("trasportatore", dto.trasportatore);
        return ddt;
    }

    public static class DDTDto {
        public Long id;

        @javax.validation.constraints.NotBlank(message = "Numero DDT non può essere vuoto")
        public String numero;

        @javax.validation.constraints.NotNull(message = "Cliente ID è obbligatorio")
        public Long clienteId;

        public String clienteNome;

        @javax.validation.constraints.NotNull(message = "Data DDT è obbligatoria")
        public Date dataDdt;

        @javax.validation.constraints.NotBlank(message = "Causale trasporto non può essere vuota")
        public String causaleTrasporto;

        @javax.validation.constraints.NotNull(message = "Numero colli è obbligatorio")
        @javax.validation.constraints.Min(value = 1, message = "Numero colli deve essere >= 1")
        public Integer numeroColli;

        public String trasportatore;

        public DDTDto() {}

        public DDTDto(Long id, String numero, Long clienteId, String clienteNome,
                     Date dataDdt, String causaleTrasporto, Integer numeroColli, String trasportatore) {
            this.id = id;
            this.numero = numero;
            this.clienteId = clienteId;
            this.clienteNome = clienteNome;
            this.dataDdt = dataDdt;
            this.causaleTrasporto = causaleTrasporto;
            this.numeroColli = numeroColli;
            this.trasportatore = trasportatore;
        }
    }
}

package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.PreventivoDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * REST API per Preventivi (Quotazioni).
 *
 * Endpoint:
 * GET    /api/v1/preventivi              - Lista con paginazione
 * GET    /api/v1/preventivi/{id}         - Dettaglio preventivo
 * POST   /api/v1/preventivi              - Crea preventivo
 * PUT    /api/v1/preventivi/{id}         - Aggiorna preventivo
 * DELETE /api/v1/preventivi/{id}         - Cancella preventivo
 * GET    /api/v1/preventivi/{id}/pdf     - Esporta PDF
 * PUT    /api/v1/preventivi/{id}/stato   - Cambia stato
 * GET    /api/v1/preventivi/search       - Ricerca avanzata
 */
@RestController
@RequestMapping("/api/v1/preventivi")
public class PreventiviController {

    private final PreventivoDAO preventivoDAO = new PreventivoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping
    public ResponseEntity<Page<PreventivoDTO>> list(
            @RequestParam(required = false) Integer anno,
            Pageable pageable) {
        List<Preventivo> results = anno != null ?
            preventivoDAO.findByAnno(anno) :
            preventivoDAO.findAll();
        List<PreventivoDTO> dtos = mapPreventiviToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PreventivoDTO>> search(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String stato) {
        List<Preventivo> risultati = new ArrayList<>();

        if (numero != null && !numero.isEmpty()) {
            risultati = preventivoDAO.findByProperty("numero", numero);
        } else if (stato != null && !stato.isEmpty()) {
            try {
                Preventivo.StatoPreventivo statoPrev = Preventivo.StatoPreventivo.valueOf(stato);
                risultati = preventivoDAO.findByStato(statoPrev);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (cliente != null && !cliente.isEmpty()) {
            risultati = findByClienteNome(cliente);
        } else {
            risultati = preventivoDAO.findAll();
        }

        return ResponseEntity.ok(mapPreventiviToDto(risultati));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreventivoDTO> getById(@PathVariable Long id) {
        Preventivo preventivo = preventivoDAO.findById(id);
        if (preventivo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(preventivo));
    }

    @PostMapping
    public ResponseEntity<PreventivoDTO> create(@RequestBody PreventivoDTO dto) {
        Preventivo preventivo = fromDto(dto, null);
        if (preventivo.getNumero() == null || preventivo.getNumero().isEmpty()) {
            int anno = LocalDateTime.now().getYear();
            preventivo.setNumero(preventivoDAO.generaNuovoNumero(anno));
            preventivo.setAnno(anno);
        }
        preventivoDAO.save(preventivo);
        return ResponseEntity.status(201).body(toDto(preventivo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PreventivoDTO> update(@PathVariable Long id, @RequestBody PreventivoDTO dto) {
        Preventivo existing = preventivoDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Preventivo updated = fromDto(dto, existing);
        updated.setId(id);
        preventivoDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @PutMapping("/{id}/stato")
    public ResponseEntity<PreventivoDTO> updateStato(
            @PathVariable Long id,
            @RequestParam String stato) {
        Preventivo preventivo = preventivoDAO.findById(id);
        if (preventivo == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            preventivo.setStato(Preventivo.StatoPreventivo.valueOf(stato));
            preventivoDAO.update(preventivo);
            return ResponseEntity.ok(toDto(preventivo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        // TODO: Implementare generazione PDF
        return ResponseEntity.status(501).body(new byte[0]);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Preventivo preventivo = preventivoDAO.findById(id);
        if (preventivo == null) {
            return ResponseEntity.notFound().build();
        }
        preventivoDAO.delete(preventivo);
        return ResponseEntity.noContent().build();
    }

    private PreventivoDTO toDto(Preventivo preventivo) {
        if (preventivo == null) {
            return null;
        }
        Long clienteId = preventivo.getCliente() != null ? preventivo.getCliente().getId() : null;
        String clienteNome = preventivo.getCliente() != null ? preventivo.getCliente().getRagioneSociale() : null;
        return new PreventivoDTO(
            preventivo.getId(),
            preventivo.getNumero(),
            clienteId,
            clienteNome,
            preventivo.getStato(),
            preventivo.getImporteTotale(),
            toLocalDate(preventivo.getDataCreazione()),
            toLocalDate(preventivo.getDataScadenza())
        );
    }

    private List<PreventivoDTO> mapPreventiviToDto(List<Preventivo> preventivi) {
        if (preventivi == null || preventivi.isEmpty()) {
            return Collections.emptyList();
        }
        List<PreventivoDTO> dtos = new ArrayList<>();
        for (Preventivo preventivo : preventivi) {
            dtos.add(toDto(preventivo));
        }
        return dtos;
    }

    private Page<PreventivoDTO> paginate(List<PreventivoDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<PreventivoDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Preventivo fromDto(PreventivoDTO dto, Preventivo existing) {
        Preventivo preventivo = existing != null ? existing : new Preventivo();
        if (dto == null) {
            return preventivo;
        }

        if (dto.numero != null && !dto.numero.isEmpty()) {
            preventivo.setNumero(dto.numero);
        }

        if (dto.clienteId != null) {
            Cliente cliente = clienteDAO.findById(dto.clienteId);
            preventivo.setCliente(cliente);
        }

        if (dto.stato != null) {
            preventivo.setStato(dto.stato);
        }

        if (dto.importo != null) {
            preventivo.setImporteTotale(dto.importo);
        }

        if (dto.dataCreazione != null) {
            preventivo.setDataCreazione(toDate(dto.dataCreazione));
        } else if (preventivo.getDataCreazione() == null) {
            preventivo.setDataCreazione(new Date());
        }

        if (dto.dataScadenza != null) {
            preventivo.setDataScadenza(toDate(dto.dataScadenza));
        }

        return preventivo;
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private List<Preventivo> findByClienteNome(String clienteNome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo p WHERE LOWER(p.cliente.ragioneSociale) LIKE LOWER(:term) ORDER BY p.dataCreazione DESC",
                Preventivo.class
            );
            query.setParameter("term", "%" + clienteNome + "%");
            return query.getResultList();
        }
    }

    public static class PreventivoDTO {
        public Long id;
        public String numero;
        public Long clienteId;
        public String clienteNome;
        public Preventivo.StatoPreventivo stato;
        public BigDecimal importo;
        public LocalDate dataCreazione;
        public LocalDate dataScadenza;

        public PreventivoDTO() {}

        public PreventivoDTO(Long id, String numero, Long clienteId, String clienteNome,
                           Preventivo.StatoPreventivo stato, BigDecimal importo,
                           LocalDate dataCreazione, LocalDate dataScadenza) {
            this.id = id;
            this.numero = numero;
            this.clienteId = clienteId;
            this.clienteNome = clienteNome;
            this.stato = stato;
            this.importo = importo;
            this.dataCreazione = dataCreazione;
            this.dataScadenza = dataScadenza;
        }
    }
}

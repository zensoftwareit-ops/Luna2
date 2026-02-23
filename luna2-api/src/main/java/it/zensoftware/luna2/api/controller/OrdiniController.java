package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * REST API per Ordini.
 * 
 * GET    /api/v1/ordini              - Lista con paginazione
 * GET    /api/v1/ordini/{id}         - Dettaglio ordine
 * POST   /api/v1/ordini              - Crea ordine
 * PUT    /api/v1/ordini/{id}         - Aggiorna ordine
 * DELETE /api/v1/ordini/{id}         - Cancella ordine
 * PUT    /api/v1/ordini/{id}/stato   - Cambia stato
 * GET    /api/v1/ordini/search       - Ricerca avanzata
 */
@RestController
@RequestMapping("/api/v1/ordini")
public class OrdiniController {

    private final OrdineDAO ordineDAO = new OrdineDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping
    public ResponseEntity<Page<OrdineDTO>> list(
            @RequestParam(required = false) Integer anno,
            Pageable pageable) {
        List<Ordine> results = ordineDAO.findByAnno(anno);
        List<OrdineDTO> dtos = mapOrdiniToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<OrdineDTO>> search(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String stato) {
        List<Ordine> risultati = new ArrayList<>();

        if (numero != null && !numero.isEmpty()) {
            risultati = ordineDAO.findByProperty("numero", numero);
        } else if (stato != null && !stato.isEmpty()) {
            risultati = ordineDAO.findByStato(Ordine.Stato.valueOf(stato));
        } else if (cliente != null && !cliente.isEmpty()) {
            risultati = findByClienteNome(cliente);
        } else {
            risultati = ordineDAO.findAll();
        }

        return ResponseEntity.ok(mapOrdiniToDto(risultati));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdineDTO> getById(@PathVariable Long id) {
        Ordine ordine = ordineDAO.findById(id);
        if (ordine == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(ordine));
    }

    @PostMapping
    public ResponseEntity<OrdineDTO> create(@RequestBody OrdineDTO dto) {
        Ordine ordine = fromDto(dto, null);
        ordineDAO.save(ordine);
        return ResponseEntity.status(201).body(toDto(ordine));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdineDTO> update(@PathVariable Long id, @RequestBody OrdineDTO dto) {
        Ordine existing = ordineDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Ordine updated = fromDto(dto, existing);
        updated.setId(id);
        ordineDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @PutMapping("/{id}/stato")
    public ResponseEntity<OrdineDTO> updateStato(
            @PathVariable Long id,
            @RequestParam String stato) {
        Ordine ordine = ordineDAO.findById(id);
        if (ordine == null) {
            return ResponseEntity.notFound().build();
        }
        ordine.setStato(Ordine.Stato.valueOf(stato));
        ordineDAO.update(ordine);
        return ResponseEntity.ok(toDto(ordine));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Ordine ordine = ordineDAO.findById(id);
        if (ordine == null) {
            return ResponseEntity.notFound().build();
        }
        ordineDAO.delete(ordine);
        return ResponseEntity.noContent().build();
    }

    private OrdineDTO toDto(Ordine ordine) {
        if (ordine == null) {
            return null;
        }
        Long clienteId = ordine.getCliente() != null ? ordine.getCliente().getId() : null;
        String clienteNome = ordine.getCliente() != null ? ordine.getCliente().getRagioneSociale() : null;
        return new OrdineDTO(
            ordine.getId(),
            ordine.getNumero(),
            clienteId,
            clienteNome,
            ordine.getStato(),
            ordine.getTotale(),
            toLocalDateTime(ordine.getDataCreazione()),
            toLocalDateTime(ordine.getDataConsegnaPrevista())
        );
    }

    private List<OrdineDTO> mapOrdiniToDto(List<Ordine> ordini) {
        if (ordini == null || ordini.isEmpty()) {
            return Collections.emptyList();
        }
        List<OrdineDTO> dtos = new ArrayList<>();
        for (Ordine ordine : ordini) {
            dtos.add(toDto(ordine));
        }
        return dtos;
    }

    private Page<OrdineDTO> paginate(List<OrdineDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<OrdineDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Ordine fromDto(OrdineDTO dto, Ordine existing) {
        Ordine ordine = existing != null ? existing : new Ordine();
        if (dto == null) {
            return ordine;
        }

        if (dto.numero != null && !dto.numero.isEmpty()) {
            ordine.setNumero(dto.numero);
        } else if (ordine.getNumero() == null || ordine.getNumero().isEmpty()) {
            int anno = LocalDateTime.now().getYear();
            ordine.setNumero(String.valueOf(ordineDAO.getNextNumero(anno)));
            ordine.setAnno(anno);
        }

        if (dto.clienteId != null) {
            Cliente cliente = clienteDAO.findById(dto.clienteId);
            ordine.setCliente(cliente);
        }

        if (dto.stato != null) {
            ordine.setStato(dto.stato);
        }

        if (dto.importo != null) {
            ordine.setTotale(dto.importo);
        }

        if (dto.dataCreazione != null) {
            ordine.setDataOrdine(toDate(dto.dataCreazione));
        } else if (ordine.getDataOrdine() == null) {
            ordine.setDataOrdine(new Date());
        }

        if (dto.dataConsegna != null) {
            ordine.setDataConsegnaPrevista(toDate(dto.dataConsegna));
        }

        return ordine;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private List<Ordine> findByClienteNome(String clienteNome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Ordine> query = session.createQuery(
                "FROM Ordine o WHERE LOWER(o.cliente.ragioneSociale) LIKE LOWER(:term) ORDER BY o.dataCreazione DESC",
                Ordine.class
            );
            query.setParameter("term", "%" + clienteNome + "%");
            return query.getResultList();
        }
    }

    
    
    public static class OrdineDTO {
        public Long id;
        public String numero;
        public Long clienteId;
        public String clienteNome;
        public Ordine.Stato stato;
        public BigDecimal importo;
        public LocalDateTime dataCreazione;
        public LocalDateTime dataConsegna;

        public OrdineDTO() {}

        public OrdineDTO(Long id, String numero, Long clienteId, String clienteNome, Ordine.Stato stato,
                         BigDecimal importo, LocalDateTime dataCreazione, LocalDateTime dataConsegna) {
            this.id = id;
            this.numero = numero;
            this.clienteId = clienteId;
            this.clienteNome = clienteNome;
            this.stato = stato;
            this.importo = importo;
            this.dataCreazione = dataCreazione;
            this.dataConsegna = dataConsegna;
        }
    }
}

package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.util.HibernateUtil;
import it.zensoftware.luna2.api.service.PdfService;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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
 * REST API per Fatture.
 * 
 * GET    /api/v1/fatture              - Lista con paginazione
 * GET    /api/v1/fatture/{id}         - Dettaglio fattura
 * POST   /api/v1/fatture              - Crea fattura
 * PUT    /api/v1/fatture/{id}         - Aggiorna fattura
 * DELETE /api/v1/fatture/{id}         - Cancella fattura
 * GET    /api/v1/fatture/{id}/pdf     - Esporta PDF
 * PUT    /api/v1/fatture/{id}/stato   - Cambia stato
 */
@RestController
@RequestMapping("/api/v1/fatture")
public class FattureController {

    private final FatturaDAO fatturaDAO = new FatturaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    @GetMapping
    public ResponseEntity<Page<FatturaDTO>> list(
            @RequestParam(required = false) Integer anno,
            Pageable pageable) {
        List<Fattura> results = fatturaDAO.findByAnno(anno);
        List<FatturaDTO> dtos = mapFattureToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FatturaDTO>> search(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String stato) {
        List<Fattura> risultati = new ArrayList<>();

        if (numero != null && !numero.isEmpty()) {
            risultati = fatturaDAO.findByProperty("numero", numero);
        } else if (stato != null && !stato.isEmpty()) {
            risultati = fatturaDAO.findByProperty("statoPagamento", Fattura.StatoPagamento.valueOf(stato));
        } else if (cliente != null && !cliente.isEmpty()) {
            risultati = findByClienteNome(cliente);
        } else {
            risultati = fatturaDAO.findAll();
        }

        return ResponseEntity.ok(mapFattureToDto(risultati));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FatturaDTO> getById(@PathVariable Long id) {
        Fattura fattura = fatturaDAO.findById(id);
        if (fattura == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(fattura));
    }

    @PostMapping
    public ResponseEntity<FatturaDTO> create(@RequestBody FatturaDTO dto) {
        Fattura fattura = fromDto(dto, null);
        fatturaDAO.save(fattura);
        return ResponseEntity.status(201).body(toDto(fattura));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FatturaDTO> update(@PathVariable Long id, @RequestBody FatturaDTO dto) {
        Fattura existing = fatturaDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Fattura updated = fromDto(dto, existing);
        updated.setId(id);
        fatturaDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @PutMapping("/{id}/stato")
    public ResponseEntity<FatturaDTO> updateStato(
            @PathVariable Long id,
            @RequestParam String stato) {
        Fattura fattura = fatturaDAO.findById(id);
        if (fattura == null) {
            return ResponseEntity.notFound().build();
        }
        fattura.setStato(Fattura.StatoPagamento.valueOf(stato));
        fatturaDAO.update(fattura);
        return ResponseEntity.ok(toDto(fattura));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        Fattura fattura = fatturaDAO.findById(id);
        if (fattura == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdfBytes = new PdfService().generateFatturaPdf(fattura);
            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"fattura-" + fattura.getNumero() + ".pdf\"")
                .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Fattura fattura = fatturaDAO.findById(id);
        if (fattura == null) {
            return ResponseEntity.notFound().build();
        }
        fatturaDAO.delete(fattura);
        return ResponseEntity.noContent().build();
    }

    private FatturaDTO toDto(Fattura fattura) {
        if (fattura == null) {
            return null;
        }
        Long clienteId = fattura.getCliente() != null ? fattura.getCliente().getId() : null;
        String clienteNome = fattura.getCliente() != null ? fattura.getCliente().getRagioneSociale() : null;
        return new FatturaDTO(
            fattura.getId(),
            fattura.getNumero(),
            clienteId,
            clienteNome,
            fattura.getTipoFattura(),
            fattura.getStatoPagamento(),
            fattura.getTotale(),
            toLocalDate(fattura.getDataFattura()),
            toLocalDate(fattura.getDataPagamento())
        );
    }

    private List<FatturaDTO> mapFattureToDto(List<Fattura> fatture) {
        if (fatture == null || fatture.isEmpty()) {
            return Collections.emptyList();
        }
        List<FatturaDTO> dtos = new ArrayList<>();
        for (Fattura fattura : fatture) {
            dtos.add(toDto(fattura));
        }
        return dtos;
    }

    private Page<FatturaDTO> paginate(List<FatturaDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<FatturaDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Fattura fromDto(FatturaDTO dto, Fattura existing) {
        Fattura fattura = existing != null ? existing : new Fattura();
        if (dto == null) {
            return fattura;
        }

        if (dto.numero != null && !dto.numero.isEmpty()) {
            fattura.setNumero(dto.numero);
        } else if (fattura.getNumero() == null || fattura.getNumero().isEmpty()) {
            int anno = LocalDateTime.now().getYear();
            fattura.setNumero(fatturaDAO.generaNuovoNumero(anno));
            fattura.setAnno(anno);
        }

        if (dto.clienteId != null) {
            Cliente cliente = clienteDAO.findById(dto.clienteId);
            fattura.setCliente(cliente);
        }

        if (dto.tipo != null) {
            fattura.setTipoFattura(dto.tipo);
        }

        if (dto.stato != null) {
            fattura.setStato(dto.stato);
        }

        if (dto.importo != null) {
            fattura.setTotale(dto.importo);
        }

        if (dto.dataEmissione != null) {
            fattura.setDataFattura(toDate(dto.dataEmissione));
        } else if (fattura.getDataFattura() == null) {
            fattura.setDataFattura(new Date());
        }

        if (dto.dataPagamento != null) {
            fattura.setDataPagamento(toDate(dto.dataPagamento));
        }

        return fattura;
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

    private List<Fattura> findByClienteNome(String clienteNome) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Fattura> query = session.createQuery(
                "FROM Fattura f WHERE LOWER(f.cliente.ragioneSociale) LIKE LOWER(:term) ORDER BY f.dataCreazione DESC",
                Fattura.class
            );
            query.setParameter("term", "%" + clienteNome + "%");
            return query.getResultList();
        }
    }

    
    
    public static class FatturaDTO {
        public Long id;
        public String numero;
        public Long clienteId;
        public String clienteNome;
        public Fattura.TipoFattura tipo;
        public Fattura.StatoPagamento stato;
        public BigDecimal importo;
        public LocalDate dataEmissione;
        public LocalDate dataPagamento;

        public FatturaDTO() {}

        public FatturaDTO(Long id, String numero, Long clienteId, String clienteNome,
                          Fattura.TipoFattura tipo, Fattura.StatoPagamento stato,
                          BigDecimal importo, LocalDate dataEmissione, LocalDate dataPagamento) {
            this.id = id;
            this.numero = numero;
            this.clienteId = clienteId;
            this.clienteNome = clienteNome;
            this.tipo = tipo;
            this.stato = stato;
            this.importo = importo;
            this.dataEmissione = dataEmissione;
            this.dataPagamento = dataPagamento;
        }
    }
}

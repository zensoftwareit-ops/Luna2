package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.PagamentoDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Pagamento;
import it.zensoftware.luna2.api.security.RateLimited;
import it.zensoftware.luna2.api.security.Auditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * REST API per Pagamenti.
 *
 * Endpoint:
 * GET    /api/v1/pagamenti              - Lista con paginazione
 * GET    /api/v1/pagamenti/{id}         - Dettaglio pagamento
 * POST   /api/v1/pagamenti              - Registra pagamento
 * PUT    /api/v1/pagamenti/{id}         - Aggiorna pagamento
 * DELETE /api/v1/pagamenti/{id}         - Annulla pagamento
 * GET    /api/v1/pagamenti/search       - Ricerca avanzata
 * PUT    /api/v1/pagamenti/{id}/riconcilia - Marca come riconciliato
 */
@RestController
@RequestMapping("/api/v1/pagamenti")
public class PagamentiController {

    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private final FatturaDAO fatturaDAO = new FatturaDAO();

    @GetMapping
    public ResponseEntity<Page<PagamentoDTO>> list(Pageable pageable) {
        List<Pagamento> results = pagamentoDAO.findAll();
        List<PagamentoDTO> dtos = mapPagamentiToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PagamentoDTO>> search(
            @RequestParam(required = false) String numeroFattura,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String riferimento) {
        List<Pagamento> risultati = new ArrayList<>();

        if (numeroFattura != null && !numeroFattura.isEmpty()) {
            risultati = pagamentoDAO.findByNumeroFattura(numeroFattura);
        } else if (metodo != null && !metodo.isEmpty()) {
            try {
                Pagamento.MetodoPagamento metodoPag = Pagamento.MetodoPagamento.valueOf(metodo);
                risultati = pagamentoDAO.findByMetodo(metodoPag);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (riferimento != null && !riferimento.isEmpty()) {
            risultati = pagamentoDAO.findByRiferimento(riferimento);
        } else {
            risultati = pagamentoDAO.findAll();
        }

        return ResponseEntity.ok(mapPagamentiToDto(risultati));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoDTO> getById(@PathVariable Long id) {
        Pagamento pagamento = pagamentoDAO.findById(id);
        if (pagamento == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(pagamento));
    }

    @RateLimited(maxRequests = 200, windowMinutes = 5)
    @Auditable(action = "CREATE", entityType = "Pagamento")
    @PostMapping
    public ResponseEntity<PagamentoDTO> create(@Valid @RequestBody PagamentoDTO dto) {
        Pagamento pagamento = fromDto(dto, null);
        pagamentoDAO.save(pagamento);

        // Aggiorna stato fattura se pagamento completo
        if (pagamento.getFattura() != null) {
            updateStatoFattura(pagamento.getFattura());
        }

        return ResponseEntity.status(201).body(toDto(pagamento));
    }

    @Auditable(action = "UPDATE", entityType = "Pagamento")
    @PutMapping("/{id}")
    public ResponseEntity<PagamentoDTO> update(@PathVariable Long id, @Valid @RequestBody PagamentoDTO dto) {
        Pagamento existing = pagamentoDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Pagamento updated = fromDto(dto, existing);
        updated.setId(id);
        pagamentoDAO.update(updated);

        // Aggiorna stato fattura
        if (updated.getFattura() != null) {
            updateStatoFattura(updated.getFattura());
        }

        return ResponseEntity.ok(toDto(updated));
    }

    @Auditable(action = "RICONCILIA", entityType = "Pagamento")
    @PutMapping("/{id}/riconcilia")
    public ResponseEntity<PagamentoDTO> riconcilia(@PathVariable Long id) {
        Pagamento pagamento = pagamentoDAO.findById(id);
        if (pagamento == null) {
            return ResponseEntity.notFound().build();
        }
        pagamento.setRiconciliato(true);
        pagamento.setDataRiconciliazione(new Date());
        pagamentoDAO.update(pagamento);
        return ResponseEntity.ok(toDto(pagamento));
    }

    @Auditable(action = "DELETE", entityType = "Pagamento")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Pagamento pagamento = pagamentoDAO.findById(id);
        if (pagamento == null) {
            return ResponseEntity.notFound().build();
        }
        Fattura fattura = pagamento.getFattura();
        pagamentoDAO.delete(pagamento);

        // Aggiorna stato fattura dopo cancellazione
        if (fattura != null) {
            updateStatoFattura(fattura);
        }

        return ResponseEntity.noContent().build();
    }

    private void updateStatoFattura(Fattura fattura) {
        List<Pagamento> pagamenti = pagamentoDAO.findByFattura(fattura);
        BigDecimal totalePagato = BigDecimal.ZERO;
        for (Pagamento p : pagamenti) {
            totalePagato = totalePagato.add(p.getImporto());
        }

        BigDecimal totale = fattura.getTotale();
        if (totalePagato.compareTo(totale) >= 0) {
            fattura.setStatoPagamento(Fattura.StatoPagamento.PAGATA);
        } else if (totalePagato.compareTo(BigDecimal.ZERO) > 0) {
            fattura.setStatoPagamento(Fattura.StatoPagamento.PARZIALMENTE_PAGATA);
        } else {
            fattura.setStatoPagamento(Fattura.StatoPagamento.DA_PAGARE);
        }
        fatturaDAO.update(fattura);
    }

    private PagamentoDTO toDto(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }
        String numeroFattura = pagamento.getFattura() != null ? pagamento.getFattura().getNumero() : null;
        return new PagamentoDTO(
            pagamento.getId(),
            numeroFattura,
            pagamento.getImporto(),
            toLocalDate(pagamento.getDataPagamento()),
            pagamento.getMetodoPagamento(),
            pagamento.getRiferimento(),
            pagamento.getRiconciliato(),
            toLocalDate(pagamento.getDataRiconciliazione())
        );
    }

    private List<PagamentoDTO> mapPagamentiToDto(List<Pagamento> pagamenti) {
        if (pagamenti == null || pagamenti.isEmpty()) {
            return Collections.emptyList();
        }
        List<PagamentoDTO> dtos = new ArrayList<>();
        for (Pagamento pagamento : pagamenti) {
            dtos.add(toDto(pagamento));
        }
        return dtos;
    }

    private Page<PagamentoDTO> paginate(List<PagamentoDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<PagamentoDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Pagamento fromDto(PagamentoDTO dto, Pagamento existing) {
        Pagamento pagamento = existing != null ? existing : new Pagamento();
        if (dto == null) {
            return pagamento;
        }

        if (dto.numeroFattura != null && !dto.numeroFattura.isEmpty()) {
            Fattura fattura = fatturaDAO.findByProperty("numero", dto.numeroFattura)
                .stream().findFirst().orElse(null);
            if (fattura != null) {
                pagamento.setFattura(fattura);
            }
        }

        if (dto.importo != null) {
            pagamento.setImporto(dto.importo);
        }

        if (dto.dataPagamento != null) {
            pagamento.setDataPagamento(toDate(dto.dataPagamento));
        }

        if (dto.metodo != null) {
            pagamento.setMetodoPagamento(dto.metodo);
        }

        if (dto.riferimento != null) {
            pagamento.setRiferimento(dto.riferimento);
        }

        return pagamento;
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

    public static class PagamentoDTO {
        public Long id;

        @javax.validation.constraints.NotBlank(message = "Numero fattura non può essere vuoto")
        public String numeroFattura;

        @javax.validation.constraints.NotNull(message = "Importo è obbligatorio")
        @javax.validation.constraints.DecimalMin(value = "0.01", message = "Importo deve essere > 0")
        public BigDecimal importo;

        @javax.validation.constraints.NotNull(message = "Data pagamento è obbligatoria")
        public LocalDate dataPagamento;

        @javax.validation.constraints.NotNull(message = "Metodo pagamento è obbligatorio")
        public Pagamento.MetodoPagamento metodo;

        public String riferimento;
        public Boolean riconciliato;
        public LocalDate dataRiconciliazione;

        public PagamentoDTO() {}

        public PagamentoDTO(Long id, String numeroFattura, BigDecimal importo, LocalDate dataPagamento,
                           Pagamento.MetodoPagamento metodo, String riferimento, Boolean riconciliato,
                           LocalDate dataRiconciliazione) {
            this.id = id;
            this.numeroFattura = numeroFattura;
            this.importo = importo;
            this.dataPagamento = dataPagamento;
            this.metodo = metodo;
            this.riferimento = riferimento;
            this.riconciliato = riconciliato;
            this.dataRiconciliazione = dataRiconciliazione;
        }
    }
}

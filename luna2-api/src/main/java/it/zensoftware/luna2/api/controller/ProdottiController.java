package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Prodotto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST API per Prodotti.
 *
 * Endpoint:
 * GET    /api/v1/prodotti              - Lista con paginazione
 * GET    /api/v1/prodotti/{id}         - Dettaglio prodotto
 * POST   /api/v1/prodotti              - Crea prodotto
 * PUT    /api/v1/prodotti/{id}         - Aggiorna prodotto
 * DELETE /api/v1/prodotti/{id}         - Cancella prodotto
 * GET    /api/v1/prodotti/search       - Ricerca avanzata
 */
@RestController
@RequestMapping("/api/v1/prodotti")
public class ProdottiController {

    private final ProdottoDAO prodottoDAO = new ProdottoDAO();

    @GetMapping
    public ResponseEntity<Page<ProdottoDTO>> list(Pageable pageable) {
        List<Prodotto> results = prodottoDAO.findAll();
        List<ProdottoDTO> dtos = mapProdottiToDto(results);
        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProdottoDTO>> search(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String categoria) {
        List<Prodotto> risultati = new ArrayList<>();

        if (nome != null && !nome.isEmpty()) {
            risultati = searchByNome(nome);
        } else if (sku != null && !sku.isEmpty()) {
            Prodotto prodotto = prodottoDAO.findBySku(sku);
            if (prodotto != null) {
                risultati.add(prodotto);
            }
        } else if (categoria != null && !categoria.isEmpty()) {
            risultati = prodottoDAO.findByProperty("categoria", categoria);
        } else {
            risultati = prodottoDAO.findAll();
        }

        return ResponseEntity.ok(mapProdottiToDto(risultati));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdottoDTO> getById(@PathVariable Long id) {
        Prodotto prodotto = prodottoDAO.findById(id);
        if (prodotto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(prodotto));
    }

    @PostMapping
    public ResponseEntity<ProdottoDTO> create(@RequestBody ProdottoDTO dto) {
        Prodotto prodotto = fromDto(dto, null);
        if (prodotto.getSku() == null || prodotto.getSku().isEmpty()) {
            prodotto.setSku("SKU-" + System.currentTimeMillis());
        }
        prodottoDAO.save(prodotto);
        return ResponseEntity.status(201).body(toDto(prodotto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdottoDTO> update(@PathVariable Long id, @RequestBody ProdottoDTO dto) {
        Prodotto existing = prodottoDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Prodotto updated = fromDto(dto, existing);
        updated.setId(id);
        prodottoDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ProdottoDTO> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantita) {
        Prodotto prodotto = prodottoDAO.findById(id);
        if (prodotto == null) {
            return ResponseEntity.notFound().build();
        }
        prodotto.setGiacenza(quantita);
        prodottoDAO.update(prodotto);
        return ResponseEntity.ok(toDto(prodotto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Prodotto prodotto = prodottoDAO.findById(id);
        if (prodotto == null) {
            return ResponseEntity.notFound().build();
        }
        prodottoDAO.delete(prodotto);
        return ResponseEntity.noContent().build();
    }

    private ProdottoDTO toDto(Prodotto prodotto) {
        if (prodotto == null) {
            return null;
        }
        return new ProdottoDTO(
            prodotto.getId(),
            prodotto.getNome(),
            prodotto.getDescrizione(),
            prodotto.getPrezzoUnitario(),
            prodotto.getGiacenza(),
            prodotto.getSku(),
            prodotto.getCategoria()
        );
    }

    private List<ProdottoDTO> mapProdottiToDto(List<Prodotto> prodotti) {
        if (prodotti == null || prodotti.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProdottoDTO> dtos = new ArrayList<>();
        for (Prodotto prodotto : prodotti) {
            dtos.add(toDto(prodotto));
        }
        return dtos;
    }

    private Page<ProdottoDTO> paginate(List<ProdottoDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<ProdottoDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }

    private Prodotto fromDto(ProdottoDTO dto, Prodotto existing) {
        Prodotto prodotto = existing != null ? existing : new Prodotto();
        if (dto == null) {
            return prodotto;
        }

        if (dto.nome != null) {
            prodotto.setNome(dto.nome);
        }
        if (dto.descrizione != null) {
            prodotto.setDescrizione(dto.descrizione);
        }
        if (dto.prezzoUnitario != null) {
            prodotto.setPrezzoUnitario(dto.prezzoUnitario);
        }
        if (dto.giacenza != null) {
            prodotto.setGiacenza(dto.giacenza);
        }
        if (dto.sku != null) {
            prodotto.setSku(dto.sku);
        }
        if (dto.categoria != null) {
            prodotto.setCategoria(dto.categoria);
        }

        return prodotto;
    }

    private List<Prodotto> searchByNome(String nome) {
        List<Prodotto> all = prodottoDAO.findAll();
        List<Prodotto> result = new ArrayList<>();
        for (Prodotto p : all) {
            if (p.getNome() != null && p.getNome().toLowerCase().contains(nome.toLowerCase())) {
                result.add(p);
            }
        }
        return result;
    }

    public static class ProdottoDTO {
        public Long id;
        public String nome;
        public String descrizione;
        public BigDecimal prezzoUnitario;
        public Integer giacenza;
        public String sku;
        public String categoria;

        public ProdottoDTO() {}

        public ProdottoDTO(Long id, String nome, String descrizione, BigDecimal prezzoUnitario,
                          Integer giacenza, String sku, String categoria) {
            this.id = id;
            this.nome = nome;
            this.descrizione = descrizione;
            this.prezzoUnitario = prezzoUnitario;
            this.giacenza = giacenza;
            this.sku = sku;
            this.categoria = categoria;
        }
    }
}

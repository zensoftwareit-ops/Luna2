package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.PreventivoRiga;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;

public class PreventivoDAO extends GenericDAOImpl<Preventivo, Long> {
    
    private static final Logger logger = LogManager.getLogger(PreventivoDAO.class);

    private PreventivoRigaDAO preventivoRigaDAO;

    public PreventivoDAO() {
        super(Preventivo.class);
        this.preventivoRigaDAO = new PreventivoRigaDAO();
    }

    public List<Preventivo> findByClienteId(Long clienteId) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE cliente.id = :clienteId ORDER BY dataPreventivo DESC", 
                Preventivo.class);
            query.setParameter("clienteId", clienteId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by cliente", e);
            throw new RuntimeException(e);
        }
    }

    public List<Preventivo> findByStato(Preventivo.Stato stato) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE stato = :stato ORDER BY dataPreventivo DESC", 
                Preventivo.class);
            query.setParameter("stato", stato);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by stato", e);
            throw new RuntimeException(e);
        }
    }

    public List<Preventivo> findByAnno(Integer anno) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE anno = :anno ORDER BY numero DESC", 
                Preventivo.class);
            query.setParameter("anno", anno);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by anno", e);
            throw new RuntimeException(e);
        }
    }

    public String getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT MAX(CAST(numero AS long)) FROM Preventivo WHERE anno = :anno", 
                Long.class);
            query.setParameter("anno", anno);
            Long maxNumero = query.uniqueResult();
            long nextNumero = (maxNumero != null) ? maxNumero + 1 : 1;
            return String.format("%d/%04d", anno, nextNumero);
        } catch (Exception e) {
            logger.error("Error getting next numero", e);
            return anno + "/0001";
        }
    }

    /**
     * Find preventivo with righe loaded (JOIN FETCH)
     */
    public Preventivo findWithRighe(Long id) {
        try {
            Preventivo preventivo = findById(id);
            if (preventivo != null) {
                // Load righe
                List<PreventivoRiga> righe = preventivoRigaDAO.findByPreventivoId(id);
                preventivo.setRighe(righe);
            }
            return preventivo;
        } catch (Exception e) {
            logger.error("Error finding preventivo with righe", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ricalcola totali del preventivo basandosi sulle righe
     */
    public void ricalcolaTotali(Preventivo preventivo) {
        if (preventivo == null || preventivo.getId() == null) {
            return;
        }
        
        try {
            List<PreventivoRiga> righe = preventivoRigaDAO.findByPreventivoId(preventivo.getId());
            
            BigDecimal imponibile = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            
            for (PreventivoRiga riga : righe) {
                if (riga.getTipoRiga() == PreventivoRiga.TipoRiga.PRODOTTO) {
                    if (riga.getImponibileRiga() != null) {
                        imponibile = imponibile.add(riga.getImponibileRiga());
                    }
                    if (riga.getIvaImporto() != null) {
                        iva = iva.add(riga.getIvaImporto());
                    }
                }
            }
            
            // Applica sconto globale se presente
            if (preventivo.getScontoPercentuale() != null && preventivo.getScontoPercentuale().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal sconto = imponibile.multiply(preventivo.getScontoPercentuale())
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                preventivo.setScontoImporto(sconto);
                imponibile = imponibile.subtract(sconto);
            } else if (preventivo.getScontoImporto() != null && preventivo.getScontoImporto().compareTo(BigDecimal.ZERO) > 0) {
                imponibile = imponibile.subtract(preventivo.getScontoImporto());
            }
            
            // Aggiungi spese trasporto
            if (preventivo.getSpeseTrasporto() != null && preventivo.getSpeseTrasporto().compareTo(BigDecimal.ZERO) > 0) {
                imponibile = imponibile.add(preventivo.getSpeseTrasporto());
            }
            
            BigDecimal totale = imponibile.add(iva);
            
            preventivo.setImponibile(imponibile);
            preventivo.setIva(iva);
            preventivo.setTotale(totale);
            
            // Persisti le modifiche
            update(preventivo);
            
        } catch (Exception e) {
            logger.error("Error ricalculating totali", e);
            throw new RuntimeException(e);
        }
    }
}

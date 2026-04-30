package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Pagamento;
import it.zensoftware.luna2.model.Fattura;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * PagamentoDAO - Data Access Object per la gestione dei pagamenti
 *
 * Fornisce metodi per operare sulla tabella 'pagamenti'
 */
public class PagamentoDAO extends GenericDAOImpl<Pagamento, Long> {

    public PagamentoDAO() {
        super(Pagamento.class);
    }

    /**
     * Ottiene tutti i pagamenti per una fattura specifica
     */
    public List<Pagamento> findByFattura(Fattura fattura) {
        if (fattura == null || fattura.getId() == null) {
            return List.of();
        }
        return findByFatturId(fattura.getId());
    }

    /**
     * Ottiene tutti i pagamenti per una fattura by ID
     */
    public List<Pagamento> findByFatturId(Long fatturaId) {
        if (fatturaId == null) {
            return List.of();
        }
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE p.fattura.id = :fatturaId ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            query.setParameter("fatturaId", fatturaId);
            return query.getResultList();
        }
    }

    /**
     * Ottiene tutti i pagamenti in un intervallo di date
     */
    public List<Pagamento> findByDataRange(Date dataInizio, Date dataFine) {
        if (dataInizio == null || dataFine == null) {
            return findAll();
        }
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE p.dataPagamento BETWEEN :inizio AND :fine ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            query.setParameter("inizio", dataInizio);
            query.setParameter("fine", dataFine);
            return query.getResultList();
        }
    }

    /**
     * Calcola l'importo totale pagato per una fattura
     */
    public BigDecimal getTotalePagatoByFattura(Long fatturaId) {
        if (fatturaId == null) {
            return BigDecimal.ZERO;
        }
        try (Session session = getSession()) {
            Query<BigDecimal> query = session.createQuery(
                    "SELECT COALESCE(SUM(p.importo), 0) FROM Pagamento p WHERE p.fattura.id = :fatturaId",
                    BigDecimal.class);
            query.setParameter("fatturaId", fatturaId);
            BigDecimal result = query.uniqueResult();
            return result != null ? result : BigDecimal.ZERO;
        }
    }

    /**
     * Ottiene tutti i pagamenti non riconciliati
     */
    public List<Pagamento> findNonRiconciliati() {
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE p.riconciliato = false ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            return query.getResultList();
        }
    }

    /**
     * Ottiene i pagamenti per metodo di pagamento
     */
    public List<Pagamento> findByMetodo(Pagamento.MetodoPagamento metodo) {
        if (metodo == null) {
            return List.of();
        }
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE p.metodoPagamento = :metodo ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            query.setParameter("metodo", metodo);
            return query.getResultList();
        }
    }

    /**
     * Ricerca pagamenti per numero di fattura
     */
    public List<Pagamento> findByNumeroFattura(String numeroFattura) {
        if (numeroFattura == null || numeroFattura.isEmpty()) {
            return List.of();
        }
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE p.fattura.numero LIKE :numero ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            query.setParameter("numero", "%" + numeroFattura + "%");
            return query.getResultList();
        }
    }

    /**
     * Ricerca pagamenti per riferimento (numero transazione, ecc)
     */
    public List<Pagamento> findByRiferimento(String riferimento) {
        if (riferimento == null || riferimento.isEmpty()) {
            return List.of();
        }
        try (Session session = getSession()) {
            Query<Pagamento> query = session.createQuery(
                    "FROM Pagamento p WHERE LOWER(p.riferimento) LIKE LOWER(:rif) ORDER BY p.dataPagamento DESC",
                    Pagamento.class);
            query.setParameter("rif", "%" + riferimento + "%");
            return query.getResultList();
        }
    }

    /**
     * Ottiene le statistiche di pagamento per un cliente
     */
    public class PagamentoStatistiche {
        public Integer numberOfPayments;
        public BigDecimal totalAmount;
        public BigDecimal averageAmount;

        public PagamentoStatistiche(Integer numberOfPayments, BigDecimal totalAmount, BigDecimal averageAmount) {
            this.numberOfPayments = numberOfPayments;
            this.totalAmount = totalAmount;
            this.averageAmount = averageAmount;
        }
    }

    /**
     * Statistiche pagamenti di un cliente
     */
    public PagamentoStatistiche getStatisticheByCliente(Long clienteId) {
        if (clienteId == null) {
            return new PagamentoStatistiche(0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        try (Session session = getSession()) {
            Query<?> query = session.createQuery(
                    "SELECT COUNT(p.id), COALESCE(SUM(p.importo), 0), COALESCE(AVG(p.importo), 0) " +
                    "FROM Pagamento p WHERE p.fattura.cliente.id = :clienteId",
                    Object[].class);
            query.setParameter("clienteId", clienteId);

            Object[] result = (Object[]) query.uniqueResult();
            if (result != null) {
                Integer count = ((Number) result[0]).intValue();
                BigDecimal total = (BigDecimal) result[1];
                BigDecimal avg = (BigDecimal) result[2];
                return new PagamentoStatistiche(count, total, avg);
            }
        }
        return new PagamentoStatistiche(0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

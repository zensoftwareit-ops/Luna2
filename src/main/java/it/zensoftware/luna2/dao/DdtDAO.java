package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class DdtDAO {

    private static final Logger logger = LogManager.getLogger(DdtDAO.class);

    public List<DdtListItem> findAll(Integer anno, String searchTerm) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sql = new StringBuilder(
                "SELECT d.id, d.numero, d.anno, d.data_ddt, c.ragione_sociale, d.causale_trasporto, d.trasportatore, d.numero_colli, d.totale " +
                "FROM ddt d " +
                "JOIN clienti c ON c.id = d.cliente_id " +
                "WHERE 1=1 "
            );

            if (anno != null) {
                sql.append(" AND d.anno = :anno ");
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql.append(" AND (LOWER(d.numero) LIKE :term OR LOWER(c.ragione_sociale) LIKE :term) ");
            }
            sql.append(" ORDER BY d.data_ddt DESC, d.id DESC ");

            NativeQuery<Object[]> query = session.createNativeQuery(sql.toString(), Object[].class);
            if (anno != null) {
                query.setParameter("anno", anno);
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                query.setParameter("term", "%" + searchTerm.trim().toLowerCase() + "%");
            }

            List<Object[]> rows = query.list();
            List<DdtListItem> items = new ArrayList<>();
            for (Object[] r : rows) {
                DdtListItem item = new DdtListItem();
                item.setId(((Number) r[0]).longValue());
                item.setNumero((String) r[1]);
                item.setAnno(r[2] != null ? ((Number) r[2]).intValue() : null);
                item.setDataDdt((Date) r[3]);
                item.setClienteRagioneSociale((String) r[4]);
                item.setCausaleTrasporto((String) r[5]);
                item.setTrasportatore((String) r[6]);
                item.setNumeroColli(r[7] != null ? ((Number) r[7]).intValue() : null);
                item.setTotale(r[8] != null ? ((Number) r[8]).doubleValue() : 0d);
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            logger.error("Errore caricamento lista DDT", e);
            throw new RuntimeException("Errore caricamento DDT", e);
        }
    }

    public String generateNumero(int anno) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String prefix = "DDT-" + anno + "-";
            String sql = "SELECT numero FROM ddt WHERE numero LIKE :prefix ORDER BY id DESC LIMIT 1";
            String lastNumero = (String) session.createNativeQuery(sql)
                .setParameter("prefix", prefix + "%")
                .uniqueResult();

            int next = 1;
            if (lastNumero != null && lastNumero.startsWith(prefix)) {
                try {
                    next = Integer.parseInt(lastNumero.substring(prefix.length())) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
            return String.format("%s%04d", prefix, next);
        } catch (Exception e) {
            logger.error("Errore generazione numero DDT", e);
            throw new RuntimeException("Errore generazione numero DDT", e);
        }
    }

    public void insert(
        String numero,
        int anno,
        Date dataDdt,
        Long clienteId,
        String causaleTrasporto,
        String aspettoBeni,
        Integer numeroColli,
        String trasportatore,
        String indirizzoDestinazione,
        String note,
        Long createdBy
    ) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String sql = "INSERT INTO ddt (numero, anno, data_ddt, cliente_id, causale_trasporto, aspetto_beni, numero_colli, trasportatore, indirizzo_destinazione, note, imponibile, iva, totale, created_by) " +
                "VALUES (:numero, :anno, :dataDdt, :clienteId, :causale, :aspetto, :colli, :trasportatore, :indirizzo, :note, 0, 0, 0, :createdBy)";

            session.createNativeQuery(sql)
                .setParameter("numero", numero)
                .setParameter("anno", anno)
                .setParameter("dataDdt", dataDdt)
                .setParameter("clienteId", clienteId)
                .setParameter("causale", causaleTrasporto)
                .setParameter("aspetto", aspettoBeni)
                .setParameter("colli", numeroColli)
                .setParameter("trasportatore", trasportatore)
                .setParameter("indirizzo", indirizzoDestinazione)
                .setParameter("note", note)
                .setParameter("createdBy", createdBy)
                .executeUpdate();

            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore inserimento DDT", e);
            throw new RuntimeException("Errore inserimento DDT", e);
        }
    }

    public void delete(Long id) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createNativeQuery("DELETE FROM ddt WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore eliminazione DDT", e);
            throw new RuntimeException("Errore eliminazione DDT", e);
        }
    }

    public static class DdtListItem {
        private Long id;
        private String numero;
        private Integer anno;
        private Date dataDdt;
        private String clienteRagioneSociale;
        private String causaleTrasporto;
        private String trasportatore;
        private Integer numeroColli;
        private Double totale;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public Integer getAnno() { return anno; }
        public void setAnno(Integer anno) { this.anno = anno; }
        public Date getDataDdt() { return dataDdt; }
        public void setDataDdt(Date dataDdt) { this.dataDdt = dataDdt; }
        public String getClienteRagioneSociale() { return clienteRagioneSociale; }
        public void setClienteRagioneSociale(String clienteRagioneSociale) { this.clienteRagioneSociale = clienteRagioneSociale; }
        public String getCausaleTrasporto() { return causaleTrasporto; }
        public void setCausaleTrasporto(String causaleTrasporto) { this.causaleTrasporto = causaleTrasporto; }
        public String getTrasportatore() { return trasportatore; }
        public void setTrasportatore(String trasportatore) { this.trasportatore = trasportatore; }
        public Integer getNumeroColli() { return numeroColli; }
        public void setNumeroColli(Integer numeroColli) { this.numeroColli = numeroColli; }
        public Double getTotale() { return totale; }
        public void setTotale(Double totale) { this.totale = totale; }
    }
}

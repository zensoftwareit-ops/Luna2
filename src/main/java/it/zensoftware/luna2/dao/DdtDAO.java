package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.util.HibernateUtil;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaRiga;
import it.zensoftware.luna2.model.Prodotto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class DdtDAO {

    private static final Logger logger = LogManager.getLogger(DdtDAO.class);
    private static volatile boolean schemaChecked = false;

    private void ensureSchema(Session session) {
        if (schemaChecked) {
            return;
        }

        synchronized (DdtDAO.class) {
            if (schemaChecked) {
                return;
            }

            session.doWork(connection -> {
                ensureDdtTable(connection);
                ensureDdtRigheTable(connection);
            });

            schemaChecked = true;
            logger.info("Schema DDT verificato/inizializzato correttamente");
        }
    }

    private void ensureDdtTable(Connection connection) throws java.sql.SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        boolean hasDdt = hasTable(metaData, "ddt");

        try (java.sql.Statement st = connection.createStatement()) {
            if (!hasDdt) {
                st.executeUpdate(
                    "CREATE TABLE ddt (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "numero VARCHAR(50) NOT NULL UNIQUE," +
                        "anno INT NOT NULL," +
                        "data_ddt DATE NOT NULL," +
                        "cliente_id BIGINT NOT NULL," +
                        "ordine_id BIGINT," +
                        "causale_trasporto VARCHAR(100)," +
                        "aspetto_beni VARCHAR(100)," +
                        "numero_colli INT," +
                        "peso DECIMAL(10,2)," +
                        "trasportatore VARCHAR(255)," +
                        "indirizzo_destinazione TEXT," +
                        "note TEXT," +
                        "imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                        "iva DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                        "totale DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                        "fattura_id BIGINT," +
                        "data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "created_by BIGINT" +
                    ")"
                );
            }

            ensureColumn(connection, st, "ddt", "causale_trasporto", "ALTER TABLE ddt ADD COLUMN causale_trasporto VARCHAR(100)");
            ensureColumn(connection, st, "ddt", "aspetto_beni", "ALTER TABLE ddt ADD COLUMN aspetto_beni VARCHAR(100)");
            ensureColumn(connection, st, "ddt", "numero_colli", "ALTER TABLE ddt ADD COLUMN numero_colli INT");
            ensureColumn(connection, st, "ddt", "trasportatore", "ALTER TABLE ddt ADD COLUMN trasportatore VARCHAR(255)");
            ensureColumn(connection, st, "ddt", "indirizzo_destinazione", "ALTER TABLE ddt ADD COLUMN indirizzo_destinazione TEXT");
            ensureColumn(connection, st, "ddt", "note", "ALTER TABLE ddt ADD COLUMN note TEXT");
            ensureColumn(connection, st, "ddt", "imponibile", "ALTER TABLE ddt ADD COLUMN imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00");
            ensureColumn(connection, st, "ddt", "iva", "ALTER TABLE ddt ADD COLUMN iva DECIMAL(15,2) NOT NULL DEFAULT 0.00");
            ensureColumn(connection, st, "ddt", "totale", "ALTER TABLE ddt ADD COLUMN totale DECIMAL(15,2) NOT NULL DEFAULT 0.00");
            ensureColumn(connection, st, "ddt", "fattura_id", "ALTER TABLE ddt ADD COLUMN fattura_id BIGINT");
            ensureColumn(connection, st, "ddt", "created_by", "ALTER TABLE ddt ADD COLUMN created_by BIGINT");
        }
    }

    private void ensureDdtRigheTable(Connection connection) throws java.sql.SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        boolean hasDdtRighe = hasTable(metaData, "ddt_righe");

        try (java.sql.Statement st = connection.createStatement()) {
            if (!hasDdtRighe) {
                st.executeUpdate(
                    "CREATE TABLE ddt_righe (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "ddt_id BIGINT NOT NULL," +
                        "riga_numero INT NOT NULL," +
                        "tipo_riga VARCHAR(20) NOT NULL DEFAULT 'PRODOTTO'," +
                        "prodotto_id BIGINT," +
                        "descrizione TEXT NOT NULL," +
                        "quantita DECIMAL(15,3) DEFAULT 1.000," +
                        "prezzo_unitario DECIMAL(15,2) DEFAULT 0.00," +
                        "imponibile_riga DECIMAL(15,2) DEFAULT 0.00," +
                        "iva_percentuale DECIMAL(5,2) DEFAULT 22.00" +
                    ")"
                );
            }

            ensureColumn(connection, st, "ddt_righe", "riga_numero", "ALTER TABLE ddt_righe ADD COLUMN riga_numero INT NOT NULL DEFAULT 1");
            ensureColumn(connection, st, "ddt_righe", "tipo_riga", "ALTER TABLE ddt_righe ADD COLUMN tipo_riga VARCHAR(20) NOT NULL DEFAULT 'PRODOTTO'");
            ensureColumn(connection, st, "ddt_righe", "prezzo_unitario", "ALTER TABLE ddt_righe ADD COLUMN prezzo_unitario DECIMAL(15,2) DEFAULT 0.00");
            ensureColumn(connection, st, "ddt_righe", "imponibile_riga", "ALTER TABLE ddt_righe ADD COLUMN imponibile_riga DECIMAL(15,2) DEFAULT 0.00");
            ensureColumn(connection, st, "ddt_righe", "iva_percentuale", "ALTER TABLE ddt_righe ADD COLUMN iva_percentuale DECIMAL(5,2) DEFAULT 22.00");
        }
    }

    private boolean hasTable(DatabaseMetaData metaData, String tableName) throws java.sql.SQLException {
        try (java.sql.ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        }
        try (java.sql.ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws java.sql.SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (java.sql.ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) {
                return true;
            }
        }
        try (java.sql.ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return rs.next();
        }
    }

    private void ensureColumn(Connection connection, java.sql.Statement st, String tableName, String columnName, String alterSql) throws java.sql.SQLException {
        if (!hasColumn(connection, tableName, columnName)) {
            st.executeUpdate(alterSql);
        }
    }

    public List<DdtListItem> findAll(Integer anno, String searchTerm) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            ensureSchema(session);
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
            logger.error("Errore caricamento lista DDT: {}", e.getMessage(), e);
            return new ArrayList<>();
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
            ensureSchema(session);
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
            ensureSchema(session);

            Number linkedFattura = (Number) session.createNativeQuery("SELECT fattura_id FROM ddt WHERE id = :id")
                .setParameter("id", id)
                .uniqueResult();
            if (linkedFattura != null) {
                throw new RuntimeException("DDT gia convertito in fattura: cancellazione non consentita");
            }

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

    public DdtHeader findHeaderById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            ensureSchema(session);
            String sql = "SELECT d.id, d.numero, d.anno, d.data_ddt, d.cliente_id, c.ragione_sociale, d.causale_trasporto, d.aspetto_beni, d.numero_colli, d.trasportatore, d.indirizzo_destinazione, d.note, d.imponibile, d.iva, d.totale, d.fattura_id " +
                "FROM ddt d JOIN clienti c ON c.id = d.cliente_id WHERE d.id = :id";
            Object[] r = (Object[]) session.createNativeQuery(sql)
                .setParameter("id", id)
                .uniqueResult();

            if (r == null) {
                return null;
            }

            DdtHeader d = new DdtHeader();
            d.setId(((Number) r[0]).longValue());
            d.setNumero((String) r[1]);
            d.setAnno(r[2] != null ? ((Number) r[2]).intValue() : null);
            d.setDataDdt((Date) r[3]);
            d.setClienteId(r[4] != null ? ((Number) r[4]).longValue() : null);
            d.setClienteRagioneSociale((String) r[5]);
            d.setCausaleTrasporto((String) r[6]);
            d.setAspettoBeni((String) r[7]);
            d.setNumeroColli(r[8] != null ? ((Number) r[8]).intValue() : null);
            d.setTrasportatore((String) r[9]);
            d.setIndirizzoDestinazione((String) r[10]);
            d.setNote((String) r[11]);
            d.setImponibile(toBigDecimal(r[12]));
            d.setIva(toBigDecimal(r[13]));
            d.setTotale(toBigDecimal(r[14]));
            d.setFatturaId(r[15] != null ? ((Number) r[15]).longValue() : null);
            return d;
        } catch (Exception e) {
            logger.error("Errore lettura testata DDT", e);
            throw new RuntimeException("Errore lettura DDT", e);
        }
    }

    public List<DdtRigaItem> findRigheByDdtId(Long ddtId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            ensureSchema(session);
            String sql = "SELECT r.id, r.riga_numero, r.prodotto_id, p.nome, r.descrizione, r.quantita, r.prezzo_unitario, r.imponibile_riga, r.iva_percentuale " +
                "FROM ddt_righe r LEFT JOIN prodotti p ON p.id = r.prodotto_id WHERE r.ddt_id = :ddtId ORDER BY r.riga_numero ASC";
            List<Object[]> rows = session.createNativeQuery(sql, Object[].class)
                .setParameter("ddtId", ddtId)
                .list();

            List<DdtRigaItem> items = new ArrayList<>();
            for (Object[] r : rows) {
                DdtRigaItem item = new DdtRigaItem();
                item.setId(((Number) r[0]).longValue());
                item.setRigaNumero(r[1] != null ? ((Number) r[1]).intValue() : null);
                item.setProdottoId(r[2] != null ? ((Number) r[2]).longValue() : null);
                item.setProdottoNome((String) r[3]);
                item.setDescrizione((String) r[4]);
                item.setQuantita(toBigDecimal(r[5], 3));
                item.setPrezzoUnitario(toBigDecimal(r[6]));
                item.setImponibileRiga(toBigDecimal(r[7]));
                item.setIvaPercentuale(toBigDecimal(r[8], 2));
                items.add(item);
            }
            return items;
        } catch (Exception e) {
            logger.error("Errore lettura righe DDT", e);
            throw new RuntimeException("Errore lettura righe DDT", e);
        }
    }

    public void addRiga(Long ddtId, Long prodottoId, String descrizione, BigDecimal quantita, BigDecimal prezzoUnitario, BigDecimal ivaPercentuale) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ensureSchema(session);

            Integer nextRiga = ((Number) session.createNativeQuery("SELECT COALESCE(MAX(riga_numero),0)+1 FROM ddt_righe WHERE ddt_id = :ddtId")
                .setParameter("ddtId", ddtId)
                .uniqueResult()).intValue();

            BigDecimal imponibile = quantita.multiply(prezzoUnitario).setScale(2, RoundingMode.HALF_UP);

            String sql = "INSERT INTO ddt_righe (ddt_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale) " +
                "VALUES (:ddtId, :rigaNumero, 'PRODOTTO', :prodottoId, :descrizione, :quantita, :prezzoUnitario, :imponibile, :ivaPercentuale)";

            session.createNativeQuery(sql)
                .setParameter("ddtId", ddtId)
                .setParameter("rigaNumero", nextRiga)
                .setParameter("prodottoId", prodottoId)
                .setParameter("descrizione", descrizione)
                .setParameter("quantita", quantita)
                .setParameter("prezzoUnitario", prezzoUnitario)
                .setParameter("imponibile", imponibile)
                .setParameter("ivaPercentuale", ivaPercentuale)
                .executeUpdate();

            recalculateTotals(session, ddtId);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore inserimento riga DDT", e);
            throw new RuntimeException("Errore inserimento riga DDT", e);
        }
    }

    public void deleteRiga(Long rigaId, Long ddtId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ensureSchema(session);
            session.createNativeQuery("DELETE FROM ddt_righe WHERE id = :id")
                .setParameter("id", rigaId)
                .executeUpdate();
            recalculateTotals(session, ddtId);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore eliminazione riga DDT", e);
            throw new RuntimeException("Errore eliminazione riga DDT", e);
        }
    }

    public Long convertToFattura(Long ddtId, Long currentUserId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            ensureSchema(session);

            DdtHeader header = findHeaderById(ddtId);
            if (header == null) {
                throw new RuntimeException("DDT non trovato");
            }
            if (header.getFatturaId() != null) {
                return header.getFatturaId();
            }

            List<DdtRigaItem> righe = findRigheByDdtId(ddtId);
            if (righe.isEmpty()) {
                throw new RuntimeException("Impossibile convertire un DDT senza righe");
            }

            FatturaDAO fatturaDAO = new FatturaDAO();
            int anno = header.getAnno() != null ? header.getAnno() : Calendar.getInstance().get(Calendar.YEAR);
            String numeroFattura = fatturaDAO.generaNuovoNumero(anno);

            Cliente cliente = session.get(Cliente.class, header.getClienteId());

            Fattura fattura = new Fattura();
            fattura.setNumero(numeroFattura);
            fattura.setAnno(anno);
            fattura.setDataFattura(new java.util.Date());
            fattura.setTipoFattura(Fattura.TipoFattura.ORDINARIA);
            fattura.setCliente(cliente);
            fattura.setOggetto("Da DDT " + header.getNumero());
            fattura.setImponibile(header.getImponibile());
            fattura.setIva(header.getIva());
            fattura.setTotale(header.getTotale());
            fattura.setTotaleNetto(header.getTotale());
            fattura.setStato(Fattura.StatoFattura.EMESSA);
            if (currentUserId != null) {
                fattura.setCreatedBy(session.get(it.zensoftware.luna2.model.User.class, currentUserId));
            }

            session.save(fattura);

            int n = 1;
            for (DdtRigaItem r : righe) {
                FatturaRiga fr = new FatturaRiga();
                fr.setFattura(fattura);
                fr.setRigaNumero(n++);
                if (r.getProdottoId() != null) {
                    fr.setProdotto(session.get(Prodotto.class, r.getProdottoId()));
                }
                fr.setDescrizione(r.getDescrizione());
                fr.setQuantita(r.getQuantita());
                fr.setPrezzoUnitario(r.getPrezzoUnitario());
                fr.setImponibileRiga(r.getImponibileRiga());
                fr.setIvaPercentuale(r.getIvaPercentuale());

                BigDecimal ivaRiga = r.getImponibileRiga()
                    .multiply(r.getIvaPercentuale())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                fr.setTotaleRiga(r.getImponibileRiga().add(ivaRiga));
                session.save(fr);
            }

            session.createNativeQuery("UPDATE ddt SET fattura_id = :fatturaId WHERE id = :ddtId")
                .setParameter("fatturaId", fattura.getId())
                .setParameter("ddtId", ddtId)
                .executeUpdate();

            tx.commit();
            return fattura.getId();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore conversione DDT in fattura", e);
            throw new RuntimeException("Errore conversione DDT in fattura", e);
        }
    }

    private void recalculateTotals(Session session, Long ddtId) {
        String sql = "SELECT COALESCE(SUM(imponibile_riga),0), COALESCE(SUM(imponibile_riga * iva_percentuale / 100),0) FROM ddt_righe WHERE ddt_id = :ddtId";
        Object[] sums = (Object[]) session.createNativeQuery(sql)
            .setParameter("ddtId", ddtId)
            .uniqueResult();

        BigDecimal imponibile = toBigDecimal(sums[0]);
        BigDecimal iva = toBigDecimal(sums[1]);
        BigDecimal totale = imponibile.add(iva).setScale(2, RoundingMode.HALF_UP);

        session.createNativeQuery("UPDATE ddt SET imponibile = :imp, iva = :iva, totale = :tot WHERE id = :ddtId")
            .setParameter("imp", imponibile)
            .setParameter("iva", iva)
            .setParameter("tot", totale)
            .setParameter("ddtId", ddtId)
            .executeUpdate();
    }

    private BigDecimal toBigDecimal(Object value) {
        return toBigDecimal(value, 2);
    }

    private BigDecimal toBigDecimal(Object value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).setScale(scale, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(scale, RoundingMode.HALF_UP);
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

    public static class DdtHeader {
        private Long id;
        private String numero;
        private Integer anno;
        private Date dataDdt;
        private Long clienteId;
        private String clienteRagioneSociale;
        private String causaleTrasporto;
        private String aspettoBeni;
        private Integer numeroColli;
        private String trasportatore;
        private String indirizzoDestinazione;
        private String note;
        private BigDecimal imponibile;
        private BigDecimal iva;
        private BigDecimal totale;
        private Long fatturaId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public Integer getAnno() { return anno; }
        public void setAnno(Integer anno) { this.anno = anno; }
        public Date getDataDdt() { return dataDdt; }
        public void setDataDdt(Date dataDdt) { this.dataDdt = dataDdt; }
        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        public String getClienteRagioneSociale() { return clienteRagioneSociale; }
        public void setClienteRagioneSociale(String clienteRagioneSociale) { this.clienteRagioneSociale = clienteRagioneSociale; }
        public String getCausaleTrasporto() { return causaleTrasporto; }
        public void setCausaleTrasporto(String causaleTrasporto) { this.causaleTrasporto = causaleTrasporto; }
        public String getAspettoBeni() { return aspettoBeni; }
        public void setAspettoBeni(String aspettoBeni) { this.aspettoBeni = aspettoBeni; }
        public Integer getNumeroColli() { return numeroColli; }
        public void setNumeroColli(Integer numeroColli) { this.numeroColli = numeroColli; }
        public String getTrasportatore() { return trasportatore; }
        public void setTrasportatore(String trasportatore) { this.trasportatore = trasportatore; }
        public String getIndirizzoDestinazione() { return indirizzoDestinazione; }
        public void setIndirizzoDestinazione(String indirizzoDestinazione) { this.indirizzoDestinazione = indirizzoDestinazione; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public BigDecimal getImponibile() { return imponibile; }
        public void setImponibile(BigDecimal imponibile) { this.imponibile = imponibile; }
        public BigDecimal getIva() { return iva; }
        public void setIva(BigDecimal iva) { this.iva = iva; }
        public BigDecimal getTotale() { return totale; }
        public void setTotale(BigDecimal totale) { this.totale = totale; }
        public Long getFatturaId() { return fatturaId; }
        public void setFatturaId(Long fatturaId) { this.fatturaId = fatturaId; }
    }

    public static class DdtRigaItem {
        private Long id;
        private Integer rigaNumero;
        private Long prodottoId;
        private String prodottoNome;
        private String descrizione;
        private BigDecimal quantita;
        private BigDecimal prezzoUnitario;
        private BigDecimal imponibileRiga;
        private BigDecimal ivaPercentuale;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getRigaNumero() { return rigaNumero; }
        public void setRigaNumero(Integer rigaNumero) { this.rigaNumero = rigaNumero; }
        public Long getProdottoId() { return prodottoId; }
        public void setProdottoId(Long prodottoId) { this.prodottoId = prodottoId; }
        public String getProdottoNome() { return prodottoNome; }
        public void setProdottoNome(String prodottoNome) { this.prodottoNome = prodottoNome; }
        public String getDescrizione() { return descrizione; }
        public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
        public BigDecimal getQuantita() { return quantita; }
        public void setQuantita(BigDecimal quantita) { this.quantita = quantita; }
        public BigDecimal getPrezzoUnitario() { return prezzoUnitario; }
        public void setPrezzoUnitario(BigDecimal prezzoUnitario) { this.prezzoUnitario = prezzoUnitario; }
        public BigDecimal getImponibileRiga() { return imponibileRiga; }
        public void setImponibileRiga(BigDecimal imponibileRiga) { this.imponibileRiga = imponibileRiga; }
        public BigDecimal getIvaPercentuale() { return ivaPercentuale; }
        public void setIvaPercentuale(BigDecimal ivaPercentuale) { this.ivaPercentuale = ivaPercentuale; }
    }
}

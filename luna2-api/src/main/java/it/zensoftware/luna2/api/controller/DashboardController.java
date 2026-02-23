package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.LeadDAO;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.dao.TaskDAO;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * REST API per Dashboard.
 * 
 * GET /api/v1/dashboard/stats - Statistiche globali
 * GET /api/v1/dashboard/ricavi - Ricavi mensili
 * GET /api/v1/dashboard/clienti-top - Top 5 clienti
 * GET /api/v1/dashboard/ordini-pending - Ordini in sospeso
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final OrdineDAO ordineDAO = new OrdineDAO();
    private final FatturaDAO fatturaDAO = new FatturaDAO();
    private final LeadDAO leadDAO = new LeadDAO();
    private final TaskDAO taskDAO = new TaskDAO();

    /**
     * Statistiche globali per il dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        int clientiAttivi = clienteDAO.findAllActive().size();
        int ordiniMese = countOrdiniMese();
        int fattureNonPagate = sumFattureNonPagate();
        int ricaviAnno = sumRicaviAnno();
        int leadAperti = (int) leadDAO.count();
        int taskScadute = (int) taskDAO.count();

        DashboardStats stats = new DashboardStats(
            clientiAttivi,
            ordiniMese,
            fattureNonPagate,
            ricaviAnno,
            leadAperti,
            taskScadute,
            LocalDateTime.now()
        );
        return ResponseEntity.ok(stats);
    }

    /**
     * Ricavi ultimi 12 mesi.
     */
    @GetMapping("/ricavi")
    public ResponseEntity<RicaviMensile[]> getRicavi() {
        List<RicaviMensile> ricaviList = buildRicaviMensili();
        RicaviMensile[] ricavi = ricaviList.toArray(new RicaviMensile[0]);
        return ResponseEntity.ok(ricavi);
    }

    private int countOrdiniMese() {
        YearMonth ym = YearMonth.now();
        Date start = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(o.id) FROM Ordine o WHERE o.dataOrdine BETWEEN :start AND :end",
                Long.class
            );
            query.setParameter("start", start);
            query.setParameter("end", end);
            Long count = query.uniqueResult();
            return count != null ? count.intValue() : 0;
        }
    }

    private int sumFattureNonPagate() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT COALESCE(SUM(f.totale), 0) FROM Fattura f WHERE f.statoPagamento <> :stato",
                BigDecimal.class
            );
            query.setParameter("stato", Fattura.StatoPagamento.PAGATA);
            BigDecimal total = query.uniqueResult();
            return total != null ? total.intValue() : 0;
        }
    }

    private int sumRicaviAnno() {
        int year = LocalDate.now().getYear();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT COALESCE(SUM(f.totale), 0) FROM Fattura f WHERE f.anno = :anno",
                BigDecimal.class
            );
            query.setParameter("anno", year);
            BigDecimal total = query.uniqueResult();
            return total != null ? total.intValue() : 0;
        }
    }

    private List<RicaviMensile> buildRicaviMensili() {
        List<RicaviMensile> list = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            BigDecimal total = sumFattureForMonth(ym);
            list.add(new RicaviMensile(ym.getMonth().name(), total));
        }
        return list;
    }

    private BigDecimal sumFattureForMonth(YearMonth ym) {
        Date start = Date.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT COALESCE(SUM(f.totale), 0) FROM Fattura f WHERE f.dataFattura BETWEEN :start AND :end",
                BigDecimal.class
            );
            query.setParameter("start", start);
            query.setParameter("end", end);
            return query.uniqueResult();
        }
    }

    public static class DashboardStats {
        public Integer clientiAttivi;
        public Integer ordiniMese;
        public Integer fattureNonPagate;
        public Integer ricaviAnno;
        public Integer leadAperti;
        public Integer taskScadute;
        public LocalDateTime lastUpdate;

        public DashboardStats() {}

        public DashboardStats(Integer clientiAttivi, Integer ordiniMese, Integer fattureNonPagate,
                             Integer ricaviAnno, Integer leadAperti, Integer taskScadute,
                             LocalDateTime lastUpdate) {
            this.clientiAttivi = clientiAttivi;
            this.ordiniMese = ordiniMese;
            this.fattureNonPagate = fattureNonPagate;
            this.ricaviAnno = ricaviAnno;
            this.leadAperti = leadAperti;
            this.taskScadute = taskScadute;
            this.lastUpdate = lastUpdate;
        }
    }

    public static class RicaviMensile {
        public String mese;
        public BigDecimal importo;

        public RicaviMensile() {}

        public RicaviMensile(String mese, BigDecimal importo) {
            this.mese = mese;
            this.importo = importo;
        }
    }
}

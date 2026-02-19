package it.zensoftware.luna2.service;

import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaPassiva;
import org.apache.commons.lang3.StringUtils;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Servizio per l'esportazione di fatture in formato Assosoftware
 */
public class FattureExportService {

    private static final String DELIMITER = "\t";
    private static final String CHARSET = "UTF-8";

    /**
     * Esporta una lista di fatture attive in formato Assosoftware
     * @param fatture lista di fatture da esportare
     * @return byte array del file esportato
     */
    public byte[] esportaFattureAssosoftware(List<Fattura> fatture) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header
        writer.println("Numero\tData\tCliente\tPartita IVA\tImponibile\tIVA\tTotale\tStato");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Dati fatture
        for (Fattura fattura : fatture) {
            try {
                String numero = fattura.getNumero() != null ? fattura.getNumero() : "";
                String data = fattura.getDataFattura() != null ? sdf.format(fattura.getDataFattura()) : "";
                String cliente = fattura.getCliente() != null ? normalizzaStringa(fattura.getCliente().getRagioneSociale()) : "";
                String partitaIva = fattura.getCliente() != null ? fattura.getCliente().getPartitaIva() : "";
                String imponibile = formatImporto(fattura.getImponibile());
                String iva = formatImporto(fattura.getIva());
                String totale = formatImporto(fattura.getTotale());
                String stato = fattura.getStatoPagamento() != null ? fattura.getStatoPagamento().toString() : "";

                writer.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                        numero, data, cliente, partitaIva, imponibile, iva, totale, stato));
            } catch (Exception e) {
                // Log and continue with next record
                System.err.println("Errore nell'esportazione fattura: " + e.getMessage());
            }
        }

        writer.flush();
        writer.close();

        try {
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore nella creazione del file di esportazione", e);
        }
    }

    /**
     * Esporta una lista di fatture passive in formato Assosoftware
     * @param fatturePassive lista di fatture passive da esportare
     * @return byte array del file esportato
     */
    public byte[] esportaFatturePassiveAssosoftware(List<FatturaPassiva> fatturePassive) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Header
        writer.println("Numero\tData\tFornitore\tPartita IVA\tImponibile\tIVA\tTotale\tScadenza\tStato Pagamento");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Dati fatture passive
        for (FatturaPassiva fattura : fatturePassive) {
            try {
                String numero = fattura.getNumero() != null ? fattura.getNumero() : "";
                String data = fattura.getDataFattura() != null ? sdf.format(fattura.getDataFattura()) : "";
                String fornitore = fattura.getFornitore() != null ? 
                        normalizzaStringa(fattura.getFornitore().getRagioneSociale()) : 
                        normalizzaStringa(fattura.getFornitoreNome());
                String partitaIva = fattura.getFornitore() != null ? 
                        fattura.getFornitore().getPartitaIva() : 
                        fattura.getFornitorePiva();
                String imponibile = formatImporto(fattura.getImponibile());
                String iva = formatImporto(fattura.getIva());
                String totale = formatImporto(fattura.getTotale());
                String scadenza = fattura.getDataScadenza() != null ? sdf.format(fattura.getDataScadenza()) : "";
                String statoPagamento = fattura.getStatoPagamento() != null ? fattura.getStatoPagamento().toString() : "";

                writer.println(String.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                        numero, data, fornitore, partitaIva, imponibile, iva, totale, scadenza, statoPagamento));
            } catch (Exception e) {
                // Log and continue with next record
                System.err.println("Errore nell'esportazione fattura passiva: " + e.getMessage());
            }
        }

        writer.flush();
        writer.close();

        try {
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore nella creazione del file di esportazione", e);
        }
    }

    /**
     * Esporta una singola fattura attiva in formato Assosoftware
     */
    public byte[] esportaSingolaFatturaAssosoftware(Fattura fattura) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Intestazione
        writer.println("=== FATTURA ===");
        writer.println("Numero Fattura: " + (fattura.getNumero() != null ? fattura.getNumero() : ""));
        writer.println("Data: " + (fattura.getDataFattura() != null ? sdf.format(fattura.getDataFattura()) : ""));
        writer.println();

        // Cliente
        writer.println("=== CLIENTE ===");
        if (fattura.getCliente() != null) {
            writer.println("Ragione Sociale: " + normalizzaStringa(fattura.getCliente().getRagioneSociale()));
            writer.println("Partita IVA: " + (fattura.getCliente().getPartitaIva() != null ? fattura.getCliente().getPartitaIva() : ""));
            writer.println("Indirizzo: " + (fattura.getCliente().getIndirizzo() != null ? fattura.getCliente().getIndirizzo() : ""));
            writer.println("CAP: " + (fattura.getCliente().getCap() != null ? fattura.getCliente().getCap() : ""));
            writer.println("Città: " + (fattura.getCliente().getCitta() != null ? fattura.getCliente().getCitta() : ""));
        }
        writer.println();

        // Importi
        writer.println("=== IMPORTI ===");
        writer.println("Imponibile: " + formatImportoDue(fattura.getImponibile()));
        writer.println("IVA: " + formatImportoDue(fattura.getIva()));
        writer.println("Totale: " + formatImportoDue(fattura.getTotale()));
        writer.println();

        // Stato
        writer.println("=== STATO ===");
        writer.println("Stato Pagamento: " + (fattura.getStatoPagamento() != null ? fattura.getStatoPagamento().toString() : ""));
        if (fattura.getDataPagamento() != null) {
            writer.println("Data Pagamento: " + sdf.format(fattura.getDataPagamento()));
        }

        writer.flush();
        writer.close();

        try {
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore nella creazione del file di esportazione", e);
        }
    }

    /**
     * Esporta una singola fattura passiva in formato Assosoftware
     */
    public byte[] esportaSingolaFatturaPassivaAssosoftware(FatturaPassiva fattura) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        // Intestazione
        writer.println("=== FATTURA PASSIVA ===");
        writer.println("Numero Fattura: " + (fattura.getNumero() != null ? fattura.getNumero() : ""));
        writer.println("Data: " + (fattura.getDataFattura() != null ? sdf.format(fattura.getDataFattura()) : ""));
        writer.println();

        // Fornitore
        writer.println("=== FORNITORE ===");
        if (fattura.getFornitore() != null) {
            writer.println("Ragione Sociale: " + normalizzaStringa(fattura.getFornitore().getRagioneSociale()));
            writer.println("Partita IVA: " + (fattura.getFornitore().getPartitaIva() != null ? fattura.getFornitore().getPartitaIva() : ""));
            writer.println("Indirizzo: " + (fattura.getFornitore().getIndirizzo() != null ? fattura.getFornitore().getIndirizzo() : ""));
            writer.println("CAP: " + (fattura.getFornitore().getCap() != null ? fattura.getFornitore().getCap() : ""));
            writer.println("Città: " + (fattura.getFornitore().getCitta() != null ? fattura.getFornitore().getCitta() : ""));
        } else {
            writer.println("Ragione Sociale: " + (fattura.getFornitoreNome() != null ? fattura.getFornitoreNome() : ""));
            writer.println("Partita IVA: " + (fattura.getFornitorePiva() != null ? fattura.getFornitorePiva() : ""));
        }
        writer.println();

        // Importi
        writer.println("=== IMPORTI ===");
        writer.println("Imponibile: " + formatImportoDue(fattura.getImponibile()));
        writer.println("IVA: " + formatImportoDue(fattura.getIva()));
        writer.println("Totale: " + formatImportoDue(fattura.getTotale()));
        writer.println();

        // Scadenze e pagamenti
        writer.println("=== DATE ===");
        if (fattura.getDataScadenza() != null) {
            writer.println("Data Scadenza: " + sdf.format(fattura.getDataScadenza()));
        }
        if (fattura.getDataPagamento() != null) {
            writer.println("Data Pagamento: " + sdf.format(fattura.getDataPagamento()));
        }
        writer.println();

        // Stato
        writer.println("=== STATO ===");
        writer.println("Stato Pagamento: " + (fattura.getStatoPagamento() != null ? fattura.getStatoPagamento().toString() : ""));
        writer.println("Stato Ricezione: " + (fattura.getStatoRicezione() != null ? fattura.getStatoRicezione() : ""));

        writer.flush();
        writer.close();

        try {
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Errore nella creazione del file di esportazione", e);
        }
    }

    /**
     * Normalizza una stringa per la rimozione di caratteri speciali
     */
    private String normalizzaStringa(String str) {
        if (StringUtils.isEmpty(str)) {
            return "";
        }
        return str.replaceAll("[\\t\\n\\r]", " ").trim();
    }

    /**
     * Formatta un importo in formato italiano (con virgola)
     */
    private String formatImporto(BigDecimal importo) {
        if (importo == null) {
            return "0,00";
        }
        return String.format("%.2f", importo).replace(".", ",");
    }

    /**
     * Formatta un importo in formato leggibile (con € e virgola)
     */
    private String formatImportoDue(BigDecimal importo) {
        if (importo == null) {
            return "€ 0,00";
        }
        return "€ " + String.format("%.2f", importo).replace(".", ",");
    }

    /**
     * Genera il nome file per l'esportazione
     */
    public String generateFileName(String tipoFattura, Integer anno) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new java.util.Date());
        return String.format("fatture_%s_%s_%s.txt", tipoFattura, anno, timestamp);
    }
}

package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import it.zensoftware.luna2.dao.AccountingAccountDAO;
import it.zensoftware.luna2.dao.AccountingAssetDAO;
import it.zensoftware.luna2.dao.AccountingEntryDAO;
import it.zensoftware.luna2.dao.AccountingPostingConfigDAO;
import it.zensoftware.luna2.dao.AccountingProfileDAO;
import it.zensoftware.luna2.dao.AccountingReportPresetDAO;
import it.zensoftware.luna2.dao.FatturaDAO;
import it.zensoftware.luna2.dao.FatturaPassivaDAO;
import it.zensoftware.luna2.dao.TaxDeadlineDAO;
import it.zensoftware.luna2.dto.AccountingBalanceRow;
import it.zensoftware.luna2.model.AccountingAccount;
import it.zensoftware.luna2.model.AccountingAsset;
import it.zensoftware.luna2.model.AccountingEntry;
import it.zensoftware.luna2.model.AccountingEntryLine;
import it.zensoftware.luna2.model.AccountingPostingConfig;
import it.zensoftware.luna2.model.AccountingProfile;
import it.zensoftware.luna2.model.AccountingReportPreset;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.FatturaPassiva;
import it.zensoftware.luna2.model.TaxDeadline;
import it.zensoftware.luna2.model.User;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContabilitaAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(ContabilitaAction.class);
    private static final String KEY_AR_CLIENTI = "AR_CLIENTI";
    private static final String KEY_AP_FORNITORI = "AP_FORNITORI";
    private static final String KEY_REVENUE_MAIN = "REVENUE_MAIN";
    private static final String KEY_COST_MAIN = "COST_MAIN";
    private static final String KEY_IVA = "IVA";
    private static final String KEY_RATEI_ATTIVI = "RATEI_ATTIVI";
    private static final String KEY_RATEI_PASSIVI = "RATEI_PASSIVI";
    private static final String KEY_RISCONTI_ATTIVI = "RISCONTI_ATTIVI";
    private static final String KEY_RISCONTI_PASSIVI = "RISCONTI_PASSIVI";
    private static final String KEY_CONTO_ECONOMICO = "CONTO_ECONOMICO";
    private static final String KEY_TAX_EXPENSE = "TAX_EXPENSE";
    private static final String KEY_CONTRIBUTION_EXPENSE = "CONTRIBUTION_EXPENSE";
    private static final String KEY_TAX_PAYABLE = "TAX_PAYABLE";
    private static final String KEY_CONTRIBUTION_PAYABLE = "CONTRIBUTION_PAYABLE";
    private static final String KEY_ASSET_ACCOUNT = "ASSET_ACCOUNT";
    private static final String KEY_ACCUM_DEPRECIATION_ACCOUNT = "ACCUM_DEPRECIATION_ACCOUNT";
    private static final String KEY_DEPRECIATION_EXPENSE_ACCOUNT = "DEPRECIATION_EXPENSE_ACCOUNT";
    private static final String REPORT_BILANCIO_VERIFICA = "BILANCIO_VERIFICA";
    private static final String REPORT_CONTO_ECONOMICO = "CONTO_ECONOMICO";
    private static final String REPORT_STATO_PATRIMONIALE = "STATO_PATRIMONIALE";
    private static final String REPORT_LIBRO_GIORNALE = "LIBRO_GIORNALE";
    private static final String REPORT_PRIMA_NOTA = "PRIMA_NOTA";
    private static final String REPORT_SCADENZARIO_FISCALE = "SCADENZARIO_FISCALE";
    private static final String REPORT_REGISTRO_IVA = "REGISTRO_IVA";
    private static final String REPORT_PARTITARIO_CLIENTI = "PARTITARIO_CLIENTI";
    private static final String REPORT_PARTITARIO_FORNITORI = "PARTITARIO_FORNITORI";
    private static final String REPORT_LIQUIDITA = "LIQUIDITA";
    private static final String REPORT_ASS_TEST_CHIUSURE = "ASSESTAMENTI_E_CHIUSURE";
    private static final String REPORT_CONSOLIDATO_MENSILE = "CONSOLIDATO_MENSILE";
    private static final String REPORT_CESPITI = "CESPITI";

    private final AccountingProfileDAO profileDAO = new AccountingProfileDAO();
    private final AccountingAssetDAO assetDAO = new AccountingAssetDAO();
    private final AccountingAccountDAO accountDAO = new AccountingAccountDAO();
    private final AccountingEntryDAO entryDAO = new AccountingEntryDAO();
    private final AccountingPostingConfigDAO postingConfigDAO = new AccountingPostingConfigDAO();
    private final AccountingReportPresetDAO reportPresetDAO = new AccountingReportPresetDAO();
    private final TaxDeadlineDAO deadlineDAO = new TaxDeadlineDAO();
    private final FatturaDAO fatturaDAO = new FatturaDAO();
    private final FatturaPassivaDAO fatturaPassivaDAO = new FatturaPassivaDAO();

    private AccountingProfile profilo;
    private AccountingAccount conto;
    private AccountingEntry registrazione;
    private TaxDeadline scadenza;
    private AccountingAsset cespite;

    private List<AccountingAccount> conti;
    private List<AccountingEntry> registrazioni;
    private List<TaxDeadline> scadenze;
    private List<AccountingAsset> cespiti;
    private List<AccountingBalanceRow> bilancioRows;
    private List<AccountingEntryLine> mastrinoLines;
    private AccountingAccount mastrinoConto;
    private List<AccountingPostingConfig> postingConfigs;

    private Long id;
    private Long accountId;
    private Long[] lineAccountIds;
    private String[] lineTypes;
    private String[] lineDescriptions;
    private String[] lineAmounts;
    private String registrazioneEntryDate;
    private String registrazioneCompetenceDate;
    private String scadenzaDate;
    private String cespitePurchaseDate;
    private Integer annoEsercizio;

    private String[] configKeys;
    private String[] configRegimes;
    private String[] configForms;
    private Long[] configAccountIds;
    private String reportType;
    private String reportDateFrom;
    private String reportDateTo;
    private String reportStatus;
    private Long reportAccountId;
    private String reportDocumentType;
    private String preview;
    private List<ReportDefinition> reportDefinitions;
    private List<String> previewHeaders;
    private List<List<String>> previewRows;
    private String previewTitle;
    private List<AccountingAccount> reportAccounts;
    private List<String> trendLabels;
    private List<BigDecimal> trendRicavi;
    private List<BigDecimal> trendCosti;
    private List<BigDecimal> trendRisultato;
    private List<AccountingReportPreset> reportPresets;

    private Integer previewPage;
    private Integer previewPageSize;
    private Integer previewTotalPages;
    private Long previewTotalRows;
    private String selectedPreset;
    private String presetName;

    private BigDecimal totalDare = BigDecimal.ZERO;
    private BigDecimal totalAvere = BigDecimal.ZERO;
    private Long postedEntries = 0L;
    private Long openDeadlines = 0L;
    private Long overdueDeadlines = 0L;
    private BigDecimal bilancioTotaleDare = BigDecimal.ZERO;
    private BigDecimal bilancioTotaleAvere = BigDecimal.ZERO;
    private BigDecimal bilancioDelta = BigDecimal.ZERO;
    private BigDecimal mastrinoSaldo = BigDecimal.ZERO;
    private Long syncedActive = 0L;
    private Long syncedPassive = 0L;
    private Long generatedRateiRisconti = 0L;
    private Long generatedChiusure = 0L;
    private Long generatedAperture = 0L;
    private Long generatedAmmortamenti = 0L;
    private InputStream inputStream;
    private String contentDisposition;

    public String dashboard() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        profilo = profileDAO.getDefaultProfile();
        registrazioni = entryDAO.findRecent(8);
        scadenze = deadlineDAO.findUpcoming(8);
        conti = accountDAO.findEnabled();

        totalDare = safe(entryDAO.sumByLineType("DEBIT"));
        totalAvere = safe(entryDAO.sumByLineType("CREDIT"));
        postedEntries = safe(entryDAO.countPosted());
        openDeadlines = deadlineDAO.countOpen();
        overdueDeadlines = deadlineDAO.countOverdue(new Date());
        return SUCCESS;
    }

    public String profilo() {
        ensureSeedData();
        profilo = profileDAO.getDefaultProfile();
        if (profilo == null) {
            profilo = new AccountingProfile();
        }
        return SUCCESS;
    }

    public String saveProfilo() {
        try {
            if (profilo == null) {
                addActionError("Profilo contabile non valido");
                return ERROR;
            }
            if (profilo.getCompanyName() == null || profilo.getCompanyName().trim().isEmpty()) {
                addActionError("Ragione sociale obbligatoria");
                return ERROR;
            }

            AccountingProfile existing = profileDAO.getDefaultProfile();
            if (existing != null && profilo.getId() == null) {
                profilo.setId(existing.getId());
            }

            if (profilo.getId() == null) {
                profileDAO.save(profilo);
            } else {
                profileDAO.update(profilo);
            }

            addActionMessage("Profilo contabile salvato");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio profilo contabile", e);
            addActionError("Errore salvataggio profilo: " + e.getMessage());
            return ERROR;
        }
    }

    public String conti() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        conti = accountDAO.findAllOrdered();
        if (id != null) {
            conto = accountDAO.findById(id);
        }
        if (conto == null) {
            conto = new AccountingAccount();
        }
        return SUCCESS;
    }

    public String saveConto() {
        try {
            conti = accountDAO.findAllOrdered();
            if (conto == null) {
                addActionError("Conto non valido");
                return ERROR;
            }
            if (conto.getCode() == null || conto.getCode().trim().isEmpty()) {
                addActionError("Codice conto obbligatorio");
                return ERROR;
            }
            if (conto.getName() == null || conto.getName().trim().isEmpty()) {
                addActionError("Nome conto obbligatorio");
                return ERROR;
            }

            AccountingAccount existingByCode = accountDAO.findByCode(conto.getCode().trim());
            if (existingByCode != null && (conto.getId() == null || !existingByCode.getId().equals(conto.getId()))) {
                addActionError("Esiste gia' un conto con questo codice");
                return ERROR;
            }

            conto.setCode(conto.getCode().trim().toUpperCase());
            if (conto.getEnabled() == null) {
                conto.setEnabled(true);
            }
            if (conto.getSystemAccount() == null) {
                conto.setSystemAccount(false);
            }

            if (conto.getId() == null) {
                accountDAO.save(conto);
            } else {
                accountDAO.update(conto);
            }

            addActionMessage("Conto salvato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio conto", e);
            addActionError("Errore salvataggio conto: " + e.getMessage());
            return ERROR;
        }
    }

    public String registrazioni() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        registrazioni = entryDAO.findRecent(100);
        return SUCCESS;
    }

    public String cespiti() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        cespiti = assetDAO.findAllOrdered();

        if (id != null) {
            cespite = assetDAO.findById(id);
        }
        if (cespite == null) {
            cespite = new AccountingAsset();
            cespite.setPurchaseDate(new Date());
            cespite.setStatus(AccountingAsset.AssetStatus.ACTIVE);
        }
        cespitePurchaseDate = formatDate(cespite.getPurchaseDate());
        return SUCCESS;
    }

    public String saveCespite() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            cespiti = assetDAO.findAllOrdered();

            if (cespite == null) {
                addActionError("Cespite non valido");
                return ERROR;
            }
            if (cespite.getCode() == null || cespite.getCode().trim().isEmpty()) {
                addActionError("Codice cespite obbligatorio");
                return ERROR;
            }
            if (cespite.getDescription() == null || cespite.getDescription().trim().isEmpty()) {
                addActionError("Descrizione cespite obbligatoria");
                return ERROR;
            }
            if (cespitePurchaseDate != null && !cespitePurchaseDate.trim().isEmpty()) {
                cespite.setPurchaseDate(parseDate(cespitePurchaseDate));
            }
            if (cespite.getPurchaseDate() == null) {
                addActionError("Data acquisto obbligatoria");
                return ERROR;
            }
            if (safe(cespite.getPurchaseAmount()).compareTo(BigDecimal.ZERO) <= 0) {
                addActionError("Costo storico deve essere maggiore di zero");
                return ERROR;
            }
            if (safe(cespite.getResidualValue()).compareTo(BigDecimal.ZERO) < 0) {
                addActionError("Valore residuo non puo' essere negativo");
                return ERROR;
            }
            if (safe(cespite.getResidualValue()).compareTo(safe(cespite.getPurchaseAmount())) > 0) {
                addActionError("Valore residuo non puo' superare il costo storico");
                return ERROR;
            }
            if ((cespite.getUsefulLifeYears() == null || cespite.getUsefulLifeYears() <= 0)
                    && (cespite.getDepreciationRate() == null || cespite.getDepreciationRate().compareTo(BigDecimal.ZERO) <= 0)) {
                addActionError("Specificare anni vita utile o aliquota ammortamento");
                return ERROR;
            }

            String code = cespite.getCode().trim().toUpperCase();
            AccountingAsset existing = assetDAO.findByCode(code);
            if (existing != null && (cespite.getId() == null || !existing.getId().equals(cespite.getId()))) {
                addActionError("Esiste gia' un cespite con questo codice");
                return ERROR;
            }

            cespite.setCode(code);
            if (cespite.getStatus() == null) {
                cespite.setStatus(AccountingAsset.AssetStatus.ACTIVE);
            }
            if (cespite.getAccumulatedDepreciation() == null) {
                cespite.setAccumulatedDepreciation(BigDecimal.ZERO);
            }

            if (cespite.getId() == null) {
                assetDAO.save(cespite);
            } else {
                assetDAO.update(cespite);
            }

            addActionMessage("Cespite salvato con successo");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio cespite", e);
            addActionError("Errore salvataggio cespite: " + e.getMessage());
            return ERROR;
        }
    }

    public String generaAmmortamentiAuto() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();

            AccountingEntry existing = entryDAO.findBySource("AMMORTAMENTO_AUTO", (long) year);
            if (existing != null) {
                generatedAmmortamenti = 0L;
                addActionMessage("Ammortamenti automatici gia' generati per l'esercizio " + year);
                return SUCCESS;
            }

            AccountingAccount fondoAmmortamento = resolveConfiguredAccount(KEY_ACCUM_DEPRECIATION_ACCOUNT);
            AccountingAccount costoAmmortamento = resolveConfiguredAccount(KEY_DEPRECIATION_EXPENSE_ACCOUNT);

            AccountingEntry entry = new AccountingEntry();
            entry.setProtocolNumber(entryDAO.getNextProtocolNumber());
            entry.setEntryDate(parseDate(year + "-12-31"));
            entry.setCompetenceDate(entry.getEntryDate());
            entry.setType(AccountingEntry.EntryType.ASSESTAMENTO);
            entry.setStatus(AccountingEntry.EntryStatus.POSTED);
            entry.setDescription("Ammortamenti automatici esercizio " + year);
            entry.setSourceType("AMMORTAMENTO_AUTO");
            entry.setSourceId((long) year);
            entry.setCreatedBy(getCurrentUser());

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            long processedAssets = 0L;

            for (AccountingAsset asset : assetDAO.findActive()) {
                if (asset.getPurchaseDate() == null || yearOf(asset.getPurchaseDate()) > year) {
                    continue;
                }

                BigDecimal annualQuota = computeAnnualDepreciation(asset);
                if (annualQuota.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal maxDepreciable = safe(asset.getPurchaseAmount()).subtract(safe(asset.getResidualValue()));
                BigDecimal remaining = maxDepreciable.subtract(safe(asset.getAccumulatedDepreciation()));
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    asset.setStatus(AccountingAsset.AssetStatus.FULLY_DEPRECIATED);
                    assetDAO.update(asset);
                    continue;
                }

                BigDecimal quota = annualQuota.min(remaining).setScale(2, RoundingMode.HALF_UP);
                if (quota.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                entry.addLine(buildLine(costoAmmortamento, AccountingEntryLine.LineType.DEBIT, quota,
                        "Quota ammortamento " + asset.getCode()));
                entry.addLine(buildLine(fondoAmmortamento, AccountingEntryLine.LineType.CREDIT, quota,
                        "Fondo ammortamento " + asset.getCode()));

                totalDebit = totalDebit.add(quota);
                totalCredit = totalCredit.add(quota);

                asset.setAccumulatedDepreciation(safe(asset.getAccumulatedDepreciation()).add(quota));
                BigDecimal updatedRemaining = maxDepreciable.subtract(safe(asset.getAccumulatedDepreciation()));
                if (updatedRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    asset.setStatus(AccountingAsset.AssetStatus.FULLY_DEPRECIATED);
                } else if (asset.getStatus() == null) {
                    asset.setStatus(AccountingAsset.AssetStatus.ACTIVE);
                }
                assetDAO.update(asset);
                processedAssets++;
            }

            if (processedAssets == 0L) {
                generatedAmmortamenti = 0L;
                addActionMessage("Nessuna quota ammortamento da generare per l'esercizio " + year);
                return SUCCESS;
            }

            entry.setTotalDebit(totalDebit);
            entry.setTotalCredit(totalCredit);
            entryDAO.saveOrUpdateEntry(entry);
            generatedAmmortamenti = processedAssets;
            addActionMessage("Ammortamenti automatici generati per " + processedAssets + " cespiti");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore generazione ammortamenti automatica", e);
            addActionError("Errore ammortamenti automatici: " + e.getMessage());
            return ERROR;
        }
    }

    public String nuovaRegistrazione() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        conti = accountDAO.findEnabled();
        registrazione = new AccountingEntry();
        registrazione.setEntryDate(new Date());
        registrazione.setCompetenceDate(new Date());
        registrazione.setProtocolNumber(entryDAO.getNextProtocolNumber());
        registrazioneEntryDate = formatDate(registrazione.getEntryDate());
        registrazioneCompetenceDate = formatDate(registrazione.getCompetenceDate());
        return SUCCESS;
    }

    public String saveRegistrazione() {
        try {
            ensureSeedData();
            conti = accountDAO.findEnabled();
            applyRegistrazioneDateInputs();

            if (registrazione == null) {
                addActionError("Registrazione non valida");
                return ERROR;
            }
            if (registrazione.getDescription() == null || registrazione.getDescription().trim().isEmpty()) {
                addActionError("Descrizione registrazione obbligatoria");
                return ERROR;
            }
            if (registrazione.getEntryDate() == null) {
                addActionError("Data registrazione obbligatoria");
                return ERROR;
            }
            if (lineAccountIds == null || lineAccountIds.length < 2) {
                addActionError("Servono almeno due righe contabili");
                return ERROR;
            }

            AccountingEntry entryToSave = registrazione;
            entryToSave.setLines(new ArrayList<>());
            entryToSave.setCreatedBy(getCurrentUser());
            if (entryToSave.getProtocolNumber() == null || entryToSave.getProtocolNumber().trim().isEmpty()) {
                entryToSave.setProtocolNumber(entryDAO.getNextProtocolNumber());
            }

            BigDecimal dare = BigDecimal.ZERO;
            BigDecimal avere = BigDecimal.ZERO;

            for (int index = 0; index < lineAccountIds.length; index++) {
                if (lineAccountIds[index] == null) {
                    continue;
                }
                String amountValue = lineAmounts != null && lineAmounts.length > index ? lineAmounts[index] : null;
                if (amountValue == null || amountValue.trim().isEmpty()) {
                    continue;
                }

                BigDecimal amount = new BigDecimal(amountValue.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                AccountingAccount account = accountDAO.findById(lineAccountIds[index]);
                if (account == null) {
                    addActionError("Conto non trovato per una delle righe");
                    return ERROR;
                }

                AccountingEntryLine line = new AccountingEntryLine();
                line.setAccount(account);
                line.setAmount(amount);
                line.setDescription(lineDescriptions != null && lineDescriptions.length > index ? lineDescriptions[index] : null);

                AccountingEntryLine.LineType lineType = AccountingEntryLine.LineType.valueOf(
                        (lineTypes != null && lineTypes.length > index && lineTypes[index] != null ? lineTypes[index] : "DEBIT").toUpperCase());
                line.setLineType(lineType);

                if (lineType == AccountingEntryLine.LineType.DEBIT) {
                    dare = dare.add(amount);
                } else {
                    avere = avere.add(amount);
                }

                entryToSave.addLine(line);
            }

            if (entryToSave.getLines().size() < 2) {
                addActionError("La registrazione deve contenere almeno due righe valorizzate");
                return ERROR;
            }
            if (dare.compareTo(avere) != 0) {
                addActionError("Registrazione sbilanciata: dare " + dare + " / avere " + avere);
                return ERROR;
            }

            entryToSave.setTotalDebit(dare);
            entryToSave.setTotalCredit(avere);
            if (entryToSave.getStatus() == null) {
                entryToSave.setStatus(AccountingEntry.EntryStatus.DRAFT);
            }

            entryDAO.saveOrUpdateEntry(entryToSave);
            addActionMessage("Registrazione contabile salvata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio registrazione", e);
            conti = accountDAO.findEnabled();
            addActionError("Errore salvataggio registrazione: " + e.getMessage());
            return ERROR;
        }
    }

    public String postaRegistrazione() {
        try {
            AccountingEntry entry = entryDAO.findWithLines(id);
            if (entry == null) {
                addActionError("Registrazione non trovata");
                registrazioni = entryDAO.findRecent(100);
                return ERROR;
            }
            entry.setStatus(AccountingEntry.EntryStatus.POSTED);
            entryDAO.saveOrUpdateEntry(entry);
            addActionMessage("Registrazione contabilizzata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore contabilizzazione registrazione", e);
            registrazioni = entryDAO.findRecent(100);
            addActionError("Errore contabilizzazione: " + e.getMessage());
            return ERROR;
        }
    }

    public String bilancio() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        bilancioRows = entryDAO.computeTrialBalance();
        bilancioTotaleDare = BigDecimal.ZERO;
        bilancioTotaleAvere = BigDecimal.ZERO;
        for (AccountingBalanceRow row : bilancioRows) {
            bilancioTotaleDare = bilancioTotaleDare.add(safe(row.getTotalDebit()));
            bilancioTotaleAvere = bilancioTotaleAvere.add(safe(row.getTotalCredit()));
        }
        bilancioDelta = bilancioTotaleDare.subtract(bilancioTotaleAvere);
        return SUCCESS;
    }

    public String mastrini() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        conti = accountDAO.findEnabled();
        if (accountId == null && !conti.isEmpty()) {
            accountId = conti.get(0).getId();
        }
        if (accountId != null) {
            mastrinoConto = accountDAO.findById(accountId);
            mastrinoLines = entryDAO.findLedgerLines(accountId);
            mastrinoSaldo = BigDecimal.ZERO;
            for (AccountingEntryLine line : mastrinoLines) {
                if (line.getLineType() == AccountingEntryLine.LineType.DEBIT) {
                    mastrinoSaldo = mastrinoSaldo.add(safe(line.getAmount()));
                } else {
                    mastrinoSaldo = mastrinoSaldo.subtract(safe(line.getAmount()));
                }
            }
        } else {
            mastrinoLines = new ArrayList<>();
            mastrinoSaldo = BigDecimal.ZERO;
        }
        return SUCCESS;
    }

    public String libroGiornale() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        registrazioni = entryDAO.findAllOrdered();
        return SUCCESS;
    }

    public String stampaMastrinoPdf() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            mastrini();

            Document document = new Document(PageSize.A4, 24, 24, 24, 24);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Mastrino - " + (mastrinoConto == null ? "Conto" : mastrinoConto.getCode() + " " + mastrinoConto.getName())));
            document.add(new Paragraph("Saldo: " + mastrinoSaldo));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2f, 2f, 4f, 2f, 2f});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Data");
            addHeaderCell(table, "Protocollo");
            addHeaderCell(table, "Descrizione");
            addHeaderCell(table, "Tipo");
            addHeaderCell(table, "Importo");

            for (AccountingEntryLine line : mastrinoLines) {
                table.addCell(new PhraseSafe(line.getEntry() != null && line.getEntry().getEntryDate() != null ? new SimpleDateFormat("dd/MM/yyyy").format(line.getEntry().getEntryDate()) : "").value);
                table.addCell(new PhraseSafe(line.getEntry() != null ? line.getEntry().getProtocolNumber() : "").value);
                table.addCell(new PhraseSafe(line.getDescription()).value);
                table.addCell(new PhraseSafe(line.getLineType() == null ? "" : line.getLineType().name()).value);
                table.addCell(new PhraseSafe(safe(line.getAmount()).toPlainString()).value);
            }

            document.add(table);
            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment;filename=mastrino_" + (mastrinoConto != null ? mastrinoConto.getCode() : "conto") + ".pdf";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore stampa mastrino PDF", e);
            addActionError("Errore stampa mastrino PDF: " + e.getMessage());
            return ERROR;
        }
    }

    public String exportMastrinoExcel() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            mastrini();

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Mastrino");

            int rowNum = 0;
            Row header = sheet.createRow(rowNum++);
            header.createCell(0).setCellValue("Data");
            header.createCell(1).setCellValue("Protocollo");
            header.createCell(2).setCellValue("Descrizione");
            header.createCell(3).setCellValue("Tipo");
            header.createCell(4).setCellValue("Importo");

            for (AccountingEntryLine line : mastrinoLines) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(line.getEntry() != null && line.getEntry().getEntryDate() != null ? new SimpleDateFormat("dd/MM/yyyy").format(line.getEntry().getEntryDate()) : "");
                row.createCell(1).setCellValue(line.getEntry() != null ? line.getEntry().getProtocolNumber() : "");
                row.createCell(2).setCellValue(line.getDescription() == null ? "" : line.getDescription());
                row.createCell(3).setCellValue(line.getLineType() == null ? "" : line.getLineType().name());
                row.createCell(4).setCellValue(safe(line.getAmount()).doubleValue());
            }

            for (int i = 0; i < 5; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment;filename=mastrino_" + (mastrinoConto != null ? mastrinoConto.getCode() : "conto") + ".xlsx";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore export mastrino Excel", e);
            addActionError("Errore export mastrino Excel: " + e.getMessage());
            return ERROR;
        }
    }

    public String stampaLibroGiornale() {
        try {
            List<AccountingEntry> orderedEntries = entryDAO.findAllOrdered();

            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font textFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            document.add(new Paragraph("Libro Giornale", titleFont));
            document.add(new Paragraph("Generato il " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), textFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2.0f, 1.5f, 4.0f, 1.8f, 1.8f, 1.6f});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Protocollo");
            addHeaderCell(table, "Data");
            addHeaderCell(table, "Descrizione");
            addHeaderCell(table, "Dare");
            addHeaderCell(table, "Avere");
            addHeaderCell(table, "Stato");

            for (AccountingEntry entry : orderedEntries) {
                table.addCell(new PhraseSafe(entry.getProtocolNumber()).value);
                table.addCell(new PhraseSafe(entry.getEntryDate() == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(entry.getEntryDate())).value);
                table.addCell(new PhraseSafe(entry.getDescription()).value);
                table.addCell(new PhraseSafe(safe(entry.getTotalDebit()).toPlainString()).value);
                table.addCell(new PhraseSafe(safe(entry.getTotalCredit()).toPlainString()).value);
                table.addCell(new PhraseSafe(entry.getStatus() == null ? "" : entry.getStatus().name()).value);
            }

            document.add(table);
            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment;filename=libro_giornale_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore stampa libro giornale", e);
            addActionError("Errore stampa libro giornale: " + e.getMessage());
            return ERROR;
        }
    }

    public String reports() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        reportDefinitions = buildReportDefinitions();
        reportAccounts = accountDAO.findEnabled();
        loadReportPresets();
        if (annoEsercizio == null) {
            annoEsercizio = Calendar.getInstance().get(Calendar.YEAR);
        }
        if (reportType == null || reportType.trim().isEmpty()) {
            reportType = REPORT_BILANCIO_VERIFICA;
        }
        if (previewPage == null || previewPage < 1) {
            previewPage = 1;
        }
        if (previewPageSize == null || previewPageSize < 10 || previewPageSize > 200) {
            previewPageSize = 25;
        }

        if (isPreviewRequested()) {
            ReportFilter filter = buildReportFilter();
            ReportTable table = buildReportTable(resolveReportType(), resolveAnnoEsercizio(), filter);
            previewTitle = table.title;
            previewHeaders = table.headers;
            previewTotalRows = (long) table.rows.size();
            previewTotalPages = Math.max(1, (int) Math.ceil((double) previewTotalRows / previewPageSize));
            if (previewPage > previewTotalPages) {
                previewPage = previewTotalPages;
            }
            previewRows = paginateRows(table.rows, previewPage, previewPageSize);
        }
        return SUCCESS;
    }

    public String saveReportPreset() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                addActionError("Utente non autenticato");
                return reports();
            }
            if (presetName == null || presetName.trim().isEmpty()) {
                addActionError("Nome preset obbligatorio");
                return reports();
            }

            AccountingReportPreset preset = reportPresetDAO.findByUserAndName(currentUser.getId(), presetName);
            if (preset == null) {
                preset = new AccountingReportPreset();
                preset.setUser(currentUser);
                preset.setName(presetName.trim());
            }
            preset.setReportType(resolveReportType());
            preset.setAnnoEsercizio(annoEsercizio);
            preset.setDateFrom(reportDateFrom);
            preset.setDateTo(reportDateTo);
            preset.setEntryStatus(reportStatus);
            preset.setAccountId(reportAccountId);
            preset.setDocumentType(reportDocumentType);
            reportPresetDAO.saveOrUpdate(preset);

            selectedPreset = preset.getName();
            addActionMessage("Preset filtri salvato: " + preset.getName());
            return reports();
        } catch (Exception e) {
            logger.error("Errore salvataggio preset report", e);
            addActionError("Errore salvataggio preset: " + e.getMessage());
            return reports();
        }
    }

    public String applyReportPreset() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                addActionError("Utente non autenticato");
                return reports();
            }
            if (selectedPreset == null || selectedPreset.trim().isEmpty()) {
                addActionError("Seleziona un preset");
                return reports();
            }

            AccountingReportPreset preset = reportPresetDAO.findByUserAndName(currentUser.getId(), selectedPreset);
            if (preset == null) {
                addActionError("Preset non trovato");
                return reports();
            }

            reportType = preset.getReportType();
            annoEsercizio = preset.getAnnoEsercizio();
            reportDateFrom = preset.getDateFrom();
            reportDateTo = preset.getDateTo();
            reportStatus = preset.getEntryStatus();
            reportAccountId = preset.getAccountId();
            reportDocumentType = preset.getDocumentType();
            preview = "1";
            previewPage = 1;

            addActionMessage("Preset applicato: " + preset.getName());
            return reports();
        } catch (Exception e) {
            logger.error("Errore applicazione preset report", e);
            addActionError("Errore applicazione preset: " + e.getMessage());
            return reports();
        }
    }

    public String deleteReportPreset() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                addActionError("Utente non autenticato");
                return reports();
            }
            if (selectedPreset == null || selectedPreset.trim().isEmpty()) {
                addActionError("Seleziona un preset da eliminare");
                return reports();
            }

            AccountingReportPreset preset = reportPresetDAO.findByUserAndName(currentUser.getId(), selectedPreset);
            if (preset == null) {
                addActionError("Preset non trovato");
                return reports();
            }
            reportPresetDAO.delete(preset);

            addActionMessage("Preset eliminato: " + selectedPreset);
            selectedPreset = null;
            return reports();
        } catch (Exception e) {
            logger.error("Errore eliminazione preset report", e);
            addActionError("Errore eliminazione preset: " + e.getMessage());
            return reports();
        }
    }

    public String exportReportPdf() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();
            ReportFilter filter = buildReportFilter();
            ReportTable table = buildReportTable(resolveReportType(), year, filter);

            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font textFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            document.add(new Paragraph(table.title, titleFont));
            document.add(new Paragraph("Esercizio: " + year, textFont));
            document.add(new Paragraph("Generato il " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), textFont));
            document.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(table.headers.size());
            pdfTable.setWidthPercentage(100);
            for (String header : table.headers) {
                addHeaderCell(pdfTable, header);
            }
            for (List<String> row : table.rows) {
                for (String cell : row) {
                    pdfTable.addCell(new PhraseSafe(cell).value);
                }
            }
            document.add(pdfTable);
            document.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment;filename=" + table.fileBaseName + "_" + year + ".pdf";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore export report PDF", e);
            addActionError("Errore export report PDF: " + e.getMessage());
            reportDefinitions = buildReportDefinitions();
            return ERROR;
        }
    }

    public String exportReportExcel() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();
            ReportFilter filter = buildReportFilter();
            ReportTable table = buildReportTable(resolveReportType(), year, filter);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");

            int rowNum = 0;
            Row title = sheet.createRow(rowNum++);
            title.createCell(0).setCellValue(table.title + " - Esercizio " + year);

            Row generated = sheet.createRow(rowNum++);
            generated.createCell(0).setCellValue("Generato il " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));

            rowNum++;

            Row header = sheet.createRow(rowNum++);
            for (int i = 0; i < table.headers.size(); i++) {
                header.createCell(i).setCellValue(table.headers.get(i));
            }

            for (List<String> values : table.rows) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < values.size(); i++) {
                    row.createCell(i).setCellValue(values.get(i));
                }
            }

            for (int i = 0; i < table.headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            inputStream = new ByteArrayInputStream(baos.toByteArray());
            contentDisposition = "attachment;filename=" + table.fileBaseName + "_" + year + ".xlsx";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore export report Excel", e);
            addActionError("Errore export report Excel: " + e.getMessage());
            reportDefinitions = buildReportDefinitions();
            return ERROR;
        }
    }

    public String exportReportCsv() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();
            ReportFilter filter = buildReportFilter();
            ReportTable table = buildReportTable(resolveReportType(), year, filter);

            StringBuilder csv = new StringBuilder();
            csv.append(escapeCsv(table.title)).append("\n");
            csv.append("Esercizio;").append(year).append("\n\n");
            csv.append(String.join(";", table.headers)).append("\n");
            for (List<String> row : table.rows) {
                List<String> cells = new ArrayList<>();
                for (String cell : row) {
                    cells.add(escapeCsv(cell));
                }
                csv.append(String.join(";", cells)).append("\n");
            }

            inputStream = new ByteArrayInputStream(csv.toString().getBytes("UTF-8"));
            contentDisposition = "attachment;filename=" + table.fileBaseName + "_" + year + ".csv";
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore export report CSV", e);
            addActionError("Errore export report CSV: " + e.getMessage());
            reportDefinitions = buildReportDefinitions();
            return ERROR;
        }
    }

    private String resolveReportType() {
        if (reportType == null || reportType.trim().isEmpty()) {
            return REPORT_BILANCIO_VERIFICA;
        }
        return reportType.trim().toUpperCase();
    }

    private List<ReportDefinition> buildReportDefinitions() {
        List<ReportDefinition> definitions = new ArrayList<>();
        definitions.add(new ReportDefinition(REPORT_BILANCIO_VERIFICA, "Bilancio di Verifica", "Saldi dare/avere per conto"));
        definitions.add(new ReportDefinition(REPORT_CONTO_ECONOMICO, "Conto Economico", "Risultato economico per classi di costo/ricavo"));
        definitions.add(new ReportDefinition(REPORT_STATO_PATRIMONIALE, "Stato Patrimoniale", "Attivo/Passivo/Patrimonio netto"));
        definitions.add(new ReportDefinition(REPORT_LIBRO_GIORNALE, "Libro Giornale", "Movimenti cronologici delle registrazioni"));
        definitions.add(new ReportDefinition(REPORT_PRIMA_NOTA, "Prima Nota", "Registrazioni con dettaglio operativo"));
        definitions.add(new ReportDefinition(REPORT_SCADENZARIO_FISCALE, "Scadenzario Fiscale", "Scadenze tributarie e previdenziali"));
        definitions.add(new ReportDefinition(REPORT_REGISTRO_IVA, "Registro IVA", "Fatture attive/passive con imponibile e IVA"));
        definitions.add(new ReportDefinition(REPORT_PARTITARIO_CLIENTI, "Partitario Clienti", "Movimenti da fatture attive"));
        definitions.add(new ReportDefinition(REPORT_PARTITARIO_FORNITORI, "Partitario Fornitori", "Movimenti da fatture passive"));
        definitions.add(new ReportDefinition(REPORT_LIQUIDITA, "Liquidita' Cassa/Banca", "Movimenti su conti di cassa e banca"));
        definitions.add(new ReportDefinition(REPORT_ASS_TEST_CHIUSURE, "Assestamenti e Chiusure", "Ratei/risconti, chiusure e aperture automatiche"));
        definitions.add(new ReportDefinition(REPORT_CONSOLIDATO_MENSILE, "Consolidato Mensile", "Trend mensile ricavi, costi e risultato"));
        definitions.add(new ReportDefinition(REPORT_CESPITI, "Registro Cespiti", "Anagrafica cespiti e stato ammortamenti"));
        return definitions;
    }

    private ReportTable buildReportTable(String key, int year, ReportFilter filter) {
        switch (key) {
            case REPORT_BILANCIO_VERIFICA:
                return buildBilancioVerificaReport(year, filter);
            case REPORT_CONTO_ECONOMICO:
                return buildContoEconomicoReport(year, filter);
            case REPORT_STATO_PATRIMONIALE:
                return buildStatoPatrimonialeReport(year, filter);
            case REPORT_LIBRO_GIORNALE:
                return buildLibroGiornaleReport(year, filter);
            case REPORT_PRIMA_NOTA:
                return buildPrimaNotaReport(year, filter);
            case REPORT_SCADENZARIO_FISCALE:
                return buildScadenzarioFiscaleReport(year, filter);
            case REPORT_REGISTRO_IVA:
                return buildRegistroIvaReport(year, filter);
            case REPORT_PARTITARIO_CLIENTI:
                return buildPartitarioClientiReport(year, filter);
            case REPORT_PARTITARIO_FORNITORI:
                return buildPartitarioFornitoriReport(year, filter);
            case REPORT_LIQUIDITA:
                return buildLiquiditaReport(year, filter);
            case REPORT_ASS_TEST_CHIUSURE:
                return buildAssestamentiChiusureReport(year, filter);
            case REPORT_CONSOLIDATO_MENSILE:
                return buildConsolidatoMensileReport(year, filter);
            case REPORT_CESPITI:
                return buildCespitiReport(year);
            default:
                return buildBilancioVerificaReport(year, filter);
        }
    }

    private ReportTable buildBilancioVerificaReport(int year, ReportFilter filter) {
        Map<Long, BigDecimal> debitByAccount = new LinkedHashMap<>();
        Map<Long, BigDecimal> creditByAccount = new LinkedHashMap<>();
        for (AccountingEntry entry : entryDAO.findAllOrdered()) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            AccountingEntry full = entryDAO.findWithLines(entry.getId());
            if (full == null || full.getLines() == null) {
                continue;
            }
            for (AccountingEntryLine line : full.getLines()) {
                if (line.getAccount() == null || line.getAccount().getId() == null) {
                    continue;
                }
                if (line.getLineType() == AccountingEntryLine.LineType.DEBIT) {
                    debitByAccount.put(line.getAccount().getId(), safe(debitByAccount.get(line.getAccount().getId())).add(safe(line.getAmount())));
                } else {
                    creditByAccount.put(line.getAccount().getId(), safe(creditByAccount.get(line.getAccount().getId())).add(safe(line.getAmount())));
                }
            }
        }

        ReportTable table = new ReportTable("Bilancio di Verifica", "bilancio_verifica");
        table.headers.add("Codice");
        table.headers.add("Conto");
        table.headers.add("Totale Dare");
        table.headers.add("Totale Avere");
        table.headers.add("Saldo");

        for (AccountingAccount account : accountDAO.findEnabled()) {
            BigDecimal debit = safe(debitByAccount.get(account.getId()));
            BigDecimal credit = safe(creditByAccount.get(account.getId()));
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            table.rows.add(rowOf(
                    account.getCode(),
                    account.getName(),
                    debit.toPlainString(),
                    credit.toPlainString(),
                    debit.subtract(credit).toPlainString()));
        }
        return table;
    }

    private ReportTable buildContoEconomicoReport(int year, ReportFilter filter) {
        ReportTable base = buildBilancioVerificaReport(year, filter);
        ReportTable table = new ReportTable("Conto Economico", "conto_economico");
        table.headers.add("Codice");
        table.headers.add("Conto");
        table.headers.add("Categoria");
        table.headers.add("Saldo");

        BigDecimal totale = BigDecimal.ZERO;
        for (List<String> row : base.rows) {
            AccountingAccount account = accountDAO.findByCode(row.get(0));
            if (account == null || account.getCategory() == null) {
                continue;
            }
            if (account.getCategory() != AccountingAccount.Category.COSTI
                    && account.getCategory() != AccountingAccount.Category.RICAVI
                    && account.getCategory() != AccountingAccount.Category.TRIBUTI) {
                continue;
            }
            BigDecimal saldo = new BigDecimal(row.get(4));
            totale = totale.add(saldo);
            table.rows.add(rowOf(account.getCode(), account.getName(), account.getCategory().name(), saldo.toPlainString()));
        }
        table.rows.add(rowOf("", "", "RISULTATO", totale.toPlainString()));
        return table;
    }

    private ReportTable buildStatoPatrimonialeReport(int year, ReportFilter filter) {
        ReportTable base = buildBilancioVerificaReport(year, filter);
        ReportTable table = new ReportTable("Stato Patrimoniale", "stato_patrimoniale");
        table.headers.add("Codice");
        table.headers.add("Conto");
        table.headers.add("Categoria");
        table.headers.add("Saldo");
        for (List<String> row : base.rows) {
            AccountingAccount account = accountDAO.findByCode(row.get(0));
            if (account == null || account.getCategory() == null) {
                continue;
            }
            if (account.getCategory() == AccountingAccount.Category.COSTI || account.getCategory() == AccountingAccount.Category.RICAVI) {
                continue;
            }
            BigDecimal saldo = new BigDecimal(row.get(4));
            if (saldo.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            table.rows.add(rowOf(account.getCode(), account.getName(), account.getCategory().name(), saldo.toPlainString()));
        }
        return table;
    }

    private ReportTable buildLibroGiornaleReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Libro Giornale", "libro_giornale");
        table.headers.add("Protocollo");
        table.headers.add("Data");
        table.headers.add("Descrizione");
        table.headers.add("Dare");
        table.headers.add("Avere");
        table.headers.add("Stato");
        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            table.rows.add(rowOf(
                    entry.getProtocolNumber(),
                    formatDateIt(entry.getEntryDate()),
                    entry.getDescription(),
                    safe(entry.getTotalDebit()).toPlainString(),
                    safe(entry.getTotalCredit()).toPlainString(),
                    entry.getStatus() == null ? "" : entry.getStatus().name()));
        }
        return table;
    }

    private ReportTable buildPrimaNotaReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Prima Nota", "prima_nota");
        table.headers.add("Protocollo");
        table.headers.add("Data");
        table.headers.add("Tipo");
        table.headers.add("Descrizione");
        table.headers.add("Documento");
        table.headers.add("Fonte");
        table.headers.add("Stato");
        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            table.rows.add(rowOf(
                    entry.getProtocolNumber(),
                    formatDateIt(entry.getEntryDate()),
                    entry.getType() == null ? "" : entry.getType().name(),
                    entry.getDescription(),
                    entry.getDocumentNumber(),
                    (entry.getSourceType() == null ? "" : entry.getSourceType()) + (entry.getSourceId() == null ? "" : " #" + entry.getSourceId()),
                    entry.getStatus() == null ? "" : entry.getStatus().name()));
        }
        return table;
    }

    private ReportTable buildScadenzarioFiscaleReport(int year, ReportFilter filter) {
        List<TaxDeadline> deadlines = deadlineDAO.findAllOrdered();
        ReportTable table = new ReportTable("Scadenzario Fiscale", "scadenzario_fiscale");
        table.headers.add("Titolo");
        table.headers.add("Tipo");
        table.headers.add("Data Scadenza");
        table.headers.add("Frequenza");
        table.headers.add("Importo");
        table.headers.add("Stato");
        for (TaxDeadline deadline : deadlines) {
            if (deadline.getDeadlineDate() == null || yearOf(deadline.getDeadlineDate()) != year) {
                continue;
            }
            if (!matchesDeadlineFilters(deadline, filter)) {
                continue;
            }
            table.rows.add(rowOf(
                    deadline.getTitle(),
                    deadline.getType() == null ? "" : deadline.getType().name(),
                    formatDateIt(deadline.getDeadlineDate()),
                    deadline.getFrequency() == null ? "" : deadline.getFrequency().name(),
                    safe(deadline.getAmountDue()).toPlainString(),
                    deadline.getStatus() == null ? "" : deadline.getStatus().name()));
        }
        return table;
    }

    private ReportTable buildRegistroIvaReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Registro IVA", "registro_iva");
        table.headers.add("Data");
        table.headers.add("Protocollo");
        table.headers.add("Tipo Registro");
        table.headers.add("Controparte");
        table.headers.add("Imponibile");
        table.headers.add("IVA");
        table.headers.add("Totale");

        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            if (!"FATTURA_ATTIVA".equals(entry.getSourceType()) && !"FATTURA_PASSIVA".equals(entry.getSourceType())) {
                continue;
            }

            AccountingEntry full = entryDAO.findWithLines(entry.getId());
            BigDecimal ivaAmount = BigDecimal.ZERO;
            if (full != null && full.getLines() != null) {
                for (AccountingEntryLine line : full.getLines()) {
                    if (line.getAccount() != null && line.getAccount().getCategory() == AccountingAccount.Category.TRIBUTI) {
                        ivaAmount = ivaAmount.add(safe(line.getAmount()));
                    }
                }
            }
            BigDecimal totale = safe(entry.getTotalDebit());
            BigDecimal imponibile = totale.subtract(ivaAmount);
            table.rows.add(rowOf(
                    formatDateIt(entry.getEntryDate()),
                    entry.getProtocolNumber(),
                    "FATTURA_ATTIVA".equals(entry.getSourceType()) ? "Vendite" : "Acquisti",
                    entry.getCounterparty(),
                    imponibile.toPlainString(),
                    ivaAmount.toPlainString(),
                    totale.toPlainString()));
        }
        return table;
    }

    private ReportTable buildPartitarioClientiReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Partitario Clienti", "partitario_clienti");
        table.headers.add("Data");
        table.headers.add("Protocollo");
        table.headers.add("Cliente");
        table.headers.add("Documento");
        table.headers.add("Importo");
        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            if (!"FATTURA_ATTIVA".equals(entry.getSourceType())) {
                continue;
            }
            table.rows.add(rowOf(
                    formatDateIt(entry.getEntryDate()),
                    entry.getProtocolNumber(),
                    entry.getCounterparty(),
                    entry.getDocumentNumber(),
                    safe(entry.getTotalDebit()).toPlainString()));
        }
        return table;
    }

    private ReportTable buildPartitarioFornitoriReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Partitario Fornitori", "partitario_fornitori");
        table.headers.add("Data");
        table.headers.add("Protocollo");
        table.headers.add("Fornitore");
        table.headers.add("Documento");
        table.headers.add("Importo");
        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            if (!"FATTURA_PASSIVA".equals(entry.getSourceType())) {
                continue;
            }
            table.rows.add(rowOf(
                    formatDateIt(entry.getEntryDate()),
                    entry.getProtocolNumber(),
                    entry.getCounterparty(),
                    entry.getDocumentNumber(),
                    safe(entry.getTotalCredit()).toPlainString()));
        }
        return table;
    }

    private ReportTable buildLiquiditaReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Liquidita' Cassa/Banca", "liquidita_cassa_banca");
        table.headers.add("Data");
        table.headers.add("Protocollo");
        table.headers.add("Conto");
        table.headers.add("Descrizione");
        table.headers.add("Entrata");
        table.headers.add("Uscita");

        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            AccountingEntry full = entryDAO.findWithLines(entry.getId());
            if (full == null || full.getLines() == null) {
                continue;
            }
            for (AccountingEntryLine line : full.getLines()) {
                AccountingAccount account = line.getAccount();
                if (account == null || account.getCode() == null) {
                    continue;
                }
                if (!"1000".equals(account.getCode()) && !"1010".equals(account.getCode())) {
                    continue;
                }
                String entrata = line.getLineType() == AccountingEntryLine.LineType.DEBIT ? safe(line.getAmount()).toPlainString() : "0";
                String uscita = line.getLineType() == AccountingEntryLine.LineType.CREDIT ? safe(line.getAmount()).toPlainString() : "0";
                table.rows.add(rowOf(
                        formatDateIt(entry.getEntryDate()),
                        entry.getProtocolNumber(),
                        account.getCode() + " - " + account.getName(),
                        line.getDescription(),
                        entrata,
                        uscita));
            }
        }
        return table;
    }

    private ReportTable buildAssestamentiChiusureReport(int year, ReportFilter filter) {
        List<AccountingEntry> entries = entryDAO.findAllOrdered();
        ReportTable table = new ReportTable("Assestamenti e Chiusure", "assestamenti_chiusure");
        table.headers.add("Data");
        table.headers.add("Protocollo");
        table.headers.add("Tipo");
        table.headers.add("Descrizione");
        table.headers.add("Fonte");
        table.headers.add("Dare");
        table.headers.add("Avere");
        for (AccountingEntry entry : entries) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            if (!"RATEI_RISCONTI_AUTO".equals(entry.getSourceType())
                    && !"CHIUSURA_AUTO".equals(entry.getSourceType())
                    && !"APERTURA_AUTO".equals(entry.getSourceType())) {
                continue;
            }
            table.rows.add(rowOf(
                    formatDateIt(entry.getEntryDate()),
                    entry.getProtocolNumber(),
                    entry.getType() == null ? "" : entry.getType().name(),
                    entry.getDescription(),
                    entry.getSourceType(),
                    safe(entry.getTotalDebit()).toPlainString(),
                    safe(entry.getTotalCredit()).toPlainString()));
        }
        return table;
    }

    private ReportTable buildConsolidatoMensileReport(int year, ReportFilter filter) {
        ReportTable table = new ReportTable("Consolidato Mensile", "consolidato_mensile");
        table.headers.add("Mese");
        table.headers.add("Ricavi");
        table.headers.add("Costi");
        table.headers.add("Risultato");

        BigDecimal[] ricavi = new BigDecimal[12];
        BigDecimal[] costi = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            ricavi[i] = BigDecimal.ZERO;
            costi[i] = BigDecimal.ZERO;
        }

        for (AccountingEntry entry : entryDAO.findAllOrdered()) {
            if (entry.getEntryDate() == null || yearOf(entry.getEntryDate()) != year) {
                continue;
            }
            if (!matchesEntryFilters(entry, filter, true)) {
                continue;
            }
            int month = monthOf(entry.getEntryDate());
            AccountingEntry full = entryDAO.findWithLines(entry.getId());
            if (full == null || full.getLines() == null) {
                continue;
            }
            for (AccountingEntryLine line : full.getLines()) {
                if (line.getAccount() == null || line.getAccount().getCategory() == null) {
                    continue;
                }
                BigDecimal amount = safe(line.getAmount());
                if (line.getAccount().getCategory() == AccountingAccount.Category.RICAVI) {
                    if (line.getLineType() == AccountingEntryLine.LineType.CREDIT) {
                        ricavi[month] = ricavi[month].add(amount);
                    } else {
                        ricavi[month] = ricavi[month].subtract(amount);
                    }
                }
                if (line.getAccount().getCategory() == AccountingAccount.Category.COSTI
                        || line.getAccount().getCategory() == AccountingAccount.Category.TRIBUTI) {
                    if (line.getLineType() == AccountingEntryLine.LineType.DEBIT) {
                        costi[month] = costi[month].add(amount);
                    } else {
                        costi[month] = costi[month].subtract(amount);
                    }
                }
            }
        }

        trendLabels = new ArrayList<>();
        trendRicavi = new ArrayList<>();
        trendCosti = new ArrayList<>();
        trendRisultato = new ArrayList<>();

        String[] months = {"Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic"};
        for (int i = 0; i < 12; i++) {
            BigDecimal risultato = ricavi[i].subtract(costi[i]);
            trendLabels.add(months[i]);
            trendRicavi.add(ricavi[i]);
            trendCosti.add(costi[i]);
            trendRisultato.add(risultato);
            table.rows.add(rowOf(months[i], ricavi[i].toPlainString(), costi[i].toPlainString(), risultato.toPlainString()));
        }

        return table;
    }

    private ReportTable buildCespitiReport(int year) {
        ReportTable table = new ReportTable("Registro Cespiti", "registro_cespiti");
        table.headers.add("Codice");
        table.headers.add("Descrizione");
        table.headers.add("Tipo");
        table.headers.add("Data acquisto");
        table.headers.add("Costo storico");
        table.headers.add("Valore residuo");
        table.headers.add("Fondo ammortamento");
        table.headers.add("Valore netto");
        table.headers.add("Stato");

        for (AccountingAsset asset : assetDAO.findAllOrdered()) {
            if (asset.getPurchaseDate() != null && yearOf(asset.getPurchaseDate()) > year) {
                continue;
            }
            BigDecimal costo = safe(asset.getPurchaseAmount());
            BigDecimal residuo = safe(asset.getResidualValue());
            BigDecimal fondo = safe(asset.getAccumulatedDepreciation());
            BigDecimal netto = costo.subtract(fondo);
            if (netto.compareTo(residuo) < 0) {
                netto = residuo;
            }

            table.rows.add(rowOf(
                    asset.getCode(),
                    asset.getDescription(),
                    asset.getAssetType() == null ? "" : asset.getAssetType().name(),
                    formatDateIt(asset.getPurchaseDate()),
                    costo.toPlainString(),
                    residuo.toPlainString(),
                    fondo.toPlainString(),
                    netto.toPlainString(),
                    asset.getStatus() == null ? "" : asset.getStatus().name()));
        }
        return table;
    }

    private ReportFilter buildReportFilter() {
        ReportFilter filter = new ReportFilter();
        if (reportDateFrom != null && !reportDateFrom.trim().isEmpty()) {
            filter.dateFrom = parseDate(reportDateFrom.trim());
        }
        if (reportDateTo != null && !reportDateTo.trim().isEmpty()) {
            filter.dateTo = parseDate(reportDateTo.trim());
        }
        if (reportStatus != null && !reportStatus.trim().isEmpty() && !"ANY".equalsIgnoreCase(reportStatus)) {
            filter.status = AccountingEntry.EntryStatus.valueOf(reportStatus.trim().toUpperCase());
        }
        if (reportAccountId != null && reportAccountId > 0) {
            filter.accountId = reportAccountId;
        }
        if (reportDocumentType != null && !reportDocumentType.trim().isEmpty() && !"ANY".equalsIgnoreCase(reportDocumentType)) {
            filter.documentType = reportDocumentType.trim().toUpperCase();
        }
        return filter;
    }

    private boolean matchesEntryFilters(AccountingEntry entry, ReportFilter filter, boolean withAccountFilter) {
        if (entry == null || filter == null) {
            return false;
        }
        if (!isDateInRange(entry.getEntryDate(), filter.dateFrom, filter.dateTo)) {
            return false;
        }
        if (filter.status != null && entry.getStatus() != filter.status) {
            return false;
        }
        if (filter.documentType != null) {
            if ("MANUALE".equals(filter.documentType)) {
                if (entry.getSourceType() != null && !entry.getSourceType().trim().isEmpty()) {
                    return false;
                }
            } else {
                String sourceType = entry.getSourceType() == null ? "" : entry.getSourceType().toUpperCase();
                String entryType = entry.getType() == null ? "" : entry.getType().name();
                if (!filter.documentType.equals(sourceType) && !filter.documentType.equals(entryType)) {
                    return false;
                }
            }
        }
        if (withAccountFilter && filter.accountId != null) {
            AccountingEntry full = entryDAO.findWithLines(entry.getId());
            if (full == null || full.getLines() == null) {
                return false;
            }
            boolean found = false;
            for (AccountingEntryLine line : full.getLines()) {
                if (line.getAccount() != null && filter.accountId.equals(line.getAccount().getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesDeadlineFilters(TaxDeadline deadline, ReportFilter filter) {
        if (deadline == null || filter == null) {
            return false;
        }
        return isDateInRange(deadline.getDeadlineDate(), filter.dateFrom, filter.dateTo);
    }

    private boolean isDateInRange(Date value, Date from, Date to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.before(from)) {
            return false;
        }
        if (to != null && value.after(to)) {
            return false;
        }
        return true;
    }

    private boolean isPreviewRequested() {
        return "1".equals(preview) || "true".equalsIgnoreCase(preview);
    }

    private List<List<String>> paginateRows(List<List<String>> rows, int page, int pageSize) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        int safePage = page < 1 ? 1 : page;
        int safePageSize = pageSize < 1 ? 25 : pageSize;
        int start = (safePage - 1) * safePageSize;
        if (start >= rows.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + safePageSize, rows.size());
        return new ArrayList<>(rows.subList(start, end));
    }

    private void loadReportPresets() {
        reportPresets = new ArrayList<>();
        User currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return;
        }
        reportPresets = reportPresetDAO.findByUser(currentUser.getId());
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private int monthOf(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH);
    }

    private List<String> rowOf(String... values) {
        List<String> row = new ArrayList<>();
        if (values == null) {
            return row;
        }
        for (String value : values) {
            row.add(value == null ? "" : value);
        }
        return row;
    }

    private String formatDateIt(Date value) {
        return value == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(value);
    }

    private static class ReportFilter {
        private Date dateFrom;
        private Date dateTo;
        private AccountingEntry.EntryStatus status;
        private Long accountId;
        private String documentType;
    }

    public String sincronizzaFatture() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            syncedActive = (long) sincronizzaFattureAttive();
            syncedPassive = (long) sincronizzaFatturePassive();
            addActionMessage("Sincronizzazione completata: " + syncedActive + " fatture attive, " + syncedPassive + " fatture passive.");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore sincronizzazione fatture", e);
            addActionError("Errore sincronizzazione fatture: " + e.getMessage());
            return ERROR;
        }
    }

    public String scadenze() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        scadenze = deadlineDAO.findAllOrdered();
        if (scadenza == null) {
            scadenza = new TaxDeadline();
            scadenza.setDeadlineDate(nextMonth());
        }
        scadenzaDate = formatDate(scadenza.getDeadlineDate());
        return SUCCESS;
    }

    public String saveScadenza() {
        try {
            scadenze = deadlineDAO.findAllOrdered();
            if (scadenza == null || scadenza.getTitle() == null || scadenza.getTitle().trim().isEmpty()) {
                addActionError("Titolo scadenza obbligatorio");
                return ERROR;
            }
            if (scadenza.getDeadlineDate() == null) {
                addActionError("Data scadenza obbligatoria");
                return ERROR;
            }
            if (scadenza.getStatus() == null) {
                scadenza.setStatus(TaxDeadline.DeadlineStatus.OPEN);
            }

            if (scadenza.getId() == null) {
                deadlineDAO.save(scadenza);
            } else {
                deadlineDAO.update(scadenza);
            }
            addActionMessage("Scadenza salvata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio scadenza", e);
            addActionError("Errore salvataggio scadenza: " + e.getMessage());
            return ERROR;
        }
    }

    public String completaScadenza() {
        try {
            TaxDeadline deadline = deadlineDAO.findById(id);
            if (deadline == null) {
                addActionError("Scadenza non trovata");
                return ERROR;
            }
            deadline.setStatus(TaxDeadline.DeadlineStatus.COMPLETED);
            deadline.setCompletedAt(new Date());
            deadlineDAO.update(deadline);
            addActionMessage("Scadenza completata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore completamento scadenza", e);
            addActionError("Errore completamento scadenza: " + e.getMessage());
            return ERROR;
        }
    }

    public String configuratore() {
        ensureSeedData();
        ensurePostingConfigSeeded();
        conti = accountDAO.findAllOrdered();
        postingConfigs = postingConfigDAO.findAllOrdered();
        return SUCCESS;
    }

    public String saveConfiguratore() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            if (configKeys == null || configAccountIds == null) {
                addActionError("Configurazione non valida");
                conti = accountDAO.findAllOrdered();
                postingConfigs = postingConfigDAO.findAllOrdered();
                return ERROR;
            }

            for (int i = 0; i < configKeys.length; i++) {
                String key = configKeys[i];
                String regime = configRegimes != null && configRegimes.length > i ? configRegimes[i] : null;
                String form = configForms != null && configForms.length > i ? configForms[i] : null;
                Long accId = configAccountIds.length > i ? configAccountIds[i] : null;
                if (key == null || key.trim().isEmpty() || accId == null) {
                    continue;
                }

                AccountingAccount account = accountDAO.findById(accId);
                if (account == null) {
                    continue;
                }

                AccountingPostingConfig config = postingConfigDAO.findConfig(key.trim(), normalize(regime), normalize(form));
                if (config == null || !equalsNullable(config.getTaxRegime(), normalize(regime)) || !equalsNullable(config.getBusinessForm(), normalize(form)) || !key.trim().equals(config.getLogicalKey())) {
                    config = new AccountingPostingConfig();
                    config.setLogicalKey(key.trim());
                    config.setTaxRegime(normalize(regime));
                    config.setBusinessForm(normalize(form));
                }
                config.setAccount(account);
                config.setActive(true);
                config.setDescription("Configurazione account logico " + key.trim());
                postingConfigDAO.saveOrUpdate(config);
            }

            addActionMessage("Configurazione conti salvata");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore salvataggio configuratore conti", e);
            addActionError("Errore salvataggio configuratore: " + e.getMessage());
            conti = accountDAO.findAllOrdered();
            postingConfigs = postingConfigDAO.findAllOrdered();
            return ERROR;
        }
    }

    public String generaRateiRiscontiAuto() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();
            AccountingEntry existing = entryDAO.findBySource("RATEI_RISCONTI_AUTO", (long) year);
            if (existing != null) {
                addActionMessage("Ratei/Risconti gia' generati per l'esercizio " + year);
                generatedRateiRisconti = 0L;
                return SUCCESS;
            }

            AccountingAccount rateiAttivi = resolveConfiguredAccount(KEY_RATEI_ATTIVI);
            AccountingAccount rateiPassivi = resolveConfiguredAccount(KEY_RATEI_PASSIVI);
            AccountingAccount riscontiAttivi = resolveConfiguredAccount(KEY_RISCONTI_ATTIVI);
            AccountingAccount riscontiPassivi = resolveConfiguredAccount(KEY_RISCONTI_PASSIVI);

            AccountingEntry assestamento = new AccountingEntry();
            assestamento.setProtocolNumber(entryDAO.getNextProtocolNumber());
            assestamento.setEntryDate(parseDate(year + "-12-31"));
            assestamento.setCompetenceDate(assestamento.getEntryDate());
            assestamento.setType(AccountingEntry.EntryType.ASSESTAMENTO);
            assestamento.setStatus(AccountingEntry.EntryStatus.POSTED);
            assestamento.setDescription("Assestamento automatico ratei/risconti " + year);
            assestamento.setSourceType("RATEI_RISCONTI_AUTO");
            assestamento.setSourceId((long) year);
            assestamento.setCreatedBy(getCurrentUser());

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int generatedLines = 0;

            List<AccountingEntry> posted = entryDAO.findPostedByYear(year);
            for (AccountingEntry entry : posted) {
                if (entry.getCompetenceDate() == null || entry.getEntryDate() == null) {
                    continue;
                }
                int entryYear = yearOf(entry.getEntryDate());
                int competenceYear = yearOf(entry.getCompetenceDate());
                if (entryYear != year) {
                    continue;
                }

                boolean toNextYear = competenceYear > year;
                boolean toPreviousYear = competenceYear < year;
                if (!toNextYear && !toPreviousYear) {
                    continue;
                }

                AccountingEntry fullEntry = entryDAO.findWithLines(entry.getId());
                if (fullEntry == null || fullEntry.getLines() == null) {
                    continue;
                }

                for (AccountingEntryLine line : fullEntry.getLines()) {
                    if (line.getAccount() == null || line.getAccount().getCategory() == null) {
                        continue;
                    }
                    AccountingAccount.Category category = line.getAccount().getCategory();
                    if (category != AccountingAccount.Category.COSTI && category != AccountingAccount.Category.RICAVI) {
                        continue;
                    }

                    BigDecimal amount = safe(line.getAmount());
                    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    if (toNextYear) {
                        if (line.getLineType() == AccountingEntryLine.LineType.DEBIT) {
                            assestamento.addLine(buildLine(riscontiAttivi, AccountingEntryLine.LineType.DEBIT, amount, "Risconto attivo automatico"));
                            assestamento.addLine(buildLine(line.getAccount(), AccountingEntryLine.LineType.CREDIT, amount, "Storno costo a risconto"));
                            totalDebit = totalDebit.add(amount);
                            totalCredit = totalCredit.add(amount);
                        } else {
                            assestamento.addLine(buildLine(line.getAccount(), AccountingEntryLine.LineType.DEBIT, amount, "Storno ricavo a risconto"));
                            assestamento.addLine(buildLine(riscontiPassivi, AccountingEntryLine.LineType.CREDIT, amount, "Risconto passivo automatico"));
                            totalDebit = totalDebit.add(amount);
                            totalCredit = totalCredit.add(amount);
                        }
                    } else if (toPreviousYear) {
                        if (line.getLineType() == AccountingEntryLine.LineType.DEBIT) {
                            assestamento.addLine(buildLine(rateiPassivi, AccountingEntryLine.LineType.DEBIT, amount, "Rateo passivo automatico"));
                            assestamento.addLine(buildLine(line.getAccount(), AccountingEntryLine.LineType.CREDIT, amount, "Storno costo da rateo"));
                            totalDebit = totalDebit.add(amount);
                            totalCredit = totalCredit.add(amount);
                        } else {
                            assestamento.addLine(buildLine(line.getAccount(), AccountingEntryLine.LineType.DEBIT, amount, "Storno ricavo da rateo"));
                            assestamento.addLine(buildLine(rateiAttivi, AccountingEntryLine.LineType.CREDIT, amount, "Rateo attivo automatico"));
                            totalDebit = totalDebit.add(amount);
                            totalCredit = totalCredit.add(amount);
                        }
                    }

                    generatedLines += 2;
                }
            }

            if (generatedLines == 0) {
                addActionMessage("Nessun rateo/risconto da generare per l'esercizio " + year);
                generatedRateiRisconti = 0L;
                return SUCCESS;
            }

            assestamento.setTotalDebit(totalDebit);
            assestamento.setTotalCredit(totalCredit);
            entryDAO.saveOrUpdateEntry(assestamento);
            generatedRateiRisconti = (long) generatedLines;
            addActionMessage("Generati ratei/risconti automatici per l'esercizio " + year + ": " + generatedLines + " righe");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore generazione ratei/risconti automatica", e);
            addActionError("Errore ratei/risconti automatici: " + e.getMessage());
            return ERROR;
        }
    }

    public String chiusuraEsercizioAuto() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();
            int year = resolveAnnoEsercizio();
            AccountingEntry existing = entryDAO.findBySource("CHIUSURA_AUTO", (long) year);
            if (existing != null) {
                generatedChiusure = 0L;
                addActionMessage("Chiusura automatica gia' presente per l'esercizio " + year);
                return SUCCESS;
            }

            AccountingAccount contoEconomico = resolveConfiguredAccount(KEY_CONTO_ECONOMICO);
            List<AccountingBalanceRow> rows = entryDAO.computeTrialBalanceByYear(year);

            AccountingEntry closing = new AccountingEntry();
            closing.setProtocolNumber(entryDAO.getNextProtocolNumber());
            closing.setEntryDate(parseDate(year + "-12-31"));
            closing.setCompetenceDate(closing.getEntryDate());
            closing.setType(AccountingEntry.EntryType.CHIUSURA);
            closing.setStatus(AccountingEntry.EntryStatus.POSTED);
            closing.setDescription("Chiusura automatica esercizio " + year);
            closing.setSourceType("CHIUSURA_AUTO");
            closing.setSourceId((long) year);
            closing.setCreatedBy(getCurrentUser());

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int generatedLines = 0;

            for (AccountingBalanceRow row : rows) {
                AccountingAccount account = accountDAO.findById(row.getAccountId());
                if (account == null || account.getCategory() == null) {
                    continue;
                }
                if (account.getCategory() != AccountingAccount.Category.RICAVI
                        && account.getCategory() != AccountingAccount.Category.COSTI
                        && account.getCategory() != AccountingAccount.Category.TRIBUTI) {
                    continue;
                }

                BigDecimal saldo = safe(row.getBalance());
                if (saldo.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal amount = saldo.abs();
                if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                    closing.addLine(buildLine(contoEconomico, AccountingEntryLine.LineType.DEBIT, amount, "Chiusura economica automatica"));
                    closing.addLine(buildLine(account, AccountingEntryLine.LineType.CREDIT, amount, "Azzeramento saldo conto economico"));
                    totalDebit = totalDebit.add(amount);
                    totalCredit = totalCredit.add(amount);
                } else {
                    closing.addLine(buildLine(account, AccountingEntryLine.LineType.DEBIT, amount, "Azzeramento saldo conto economico"));
                    closing.addLine(buildLine(contoEconomico, AccountingEntryLine.LineType.CREDIT, amount, "Chiusura economica automatica"));
                    totalDebit = totalDebit.add(amount);
                    totalCredit = totalCredit.add(amount);
                }
                generatedLines += 2;
            }

            if (generatedLines == 0) {
                generatedChiusure = 0L;
                addActionMessage("Nessuna scrittura da chiudere per l'esercizio " + year);
                return SUCCESS;
            }

            closing.setTotalDebit(totalDebit);
            closing.setTotalCredit(totalCredit);
            entryDAO.saveOrUpdateEntry(closing);
            generatedChiusure = (long) generatedLines;
            addActionMessage("Chiusura automatica esercizio " + year + " completata: " + generatedLines + " righe generate");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore chiusura automatica esercizio", e);
            addActionError("Errore chiusura automatica: " + e.getMessage());
            return ERROR;
        }
    }

    public String aperturaEsercizioAuto() {
        try {
            ensureSeedData();
            ensurePostingConfigSeeded();

            int year = resolveAnnoEsercizio();
            int nextYear = year + 1;

            AccountingEntry existing = entryDAO.findBySource("APERTURA_AUTO", (long) nextYear);
            if (existing != null) {
                generatedAperture = 0L;
                addActionMessage("Apertura automatica gia' presente per l'esercizio " + nextYear);
                return SUCCESS;
            }

            List<AccountingBalanceRow> rows = entryDAO.computeTrialBalanceByYear(year);
            AccountingEntry opening = new AccountingEntry();
            opening.setProtocolNumber(entryDAO.getNextProtocolNumber());
            opening.setEntryDate(parseDate(nextYear + "-01-01"));
            opening.setCompetenceDate(opening.getEntryDate());
            opening.setType(AccountingEntry.EntryType.APERTURA);
            opening.setStatus(AccountingEntry.EntryStatus.POSTED);
            opening.setDescription("Apertura automatica esercizio " + nextYear + " da saldi " + year);
            opening.setSourceType("APERTURA_AUTO");
            opening.setSourceId((long) nextYear);
            opening.setCreatedBy(getCurrentUser());

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;
            int generatedLines = 0;

            for (AccountingBalanceRow row : rows) {
                AccountingAccount account = accountDAO.findById(row.getAccountId());
                if (account == null || account.getCategory() == null) {
                    continue;
                }

                if (account.getCategory() == AccountingAccount.Category.COSTI
                        || account.getCategory() == AccountingAccount.Category.RICAVI) {
                    continue;
                }

                BigDecimal saldo = safe(row.getBalance());
                if (saldo.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal amount = saldo.abs();
                AccountingEntryLine.LineType lineType = saldo.compareTo(BigDecimal.ZERO) > 0
                        ? AccountingEntryLine.LineType.DEBIT
                        : AccountingEntryLine.LineType.CREDIT;

                opening.addLine(buildLine(account, lineType, amount, "Apertura saldo iniziale " + nextYear));
                if (lineType == AccountingEntryLine.LineType.DEBIT) {
                    totalDebit = totalDebit.add(amount);
                } else {
                    totalCredit = totalCredit.add(amount);
                }
                generatedLines++;
            }

            if (generatedLines == 0) {
                generatedAperture = 0L;
                addActionMessage("Nessun saldo patrimoniale da riaprire per l'esercizio " + nextYear);
                return SUCCESS;
            }

            if (totalDebit.compareTo(totalCredit) != 0) {
                throw new RuntimeException("Riapertura non quadrata (Dare=" + totalDebit + ", Avere=" + totalCredit + ")");
            }

            opening.setTotalDebit(totalDebit);
            opening.setTotalCredit(totalCredit);
            entryDAO.saveOrUpdateEntry(opening);
            generatedAperture = (long) generatedLines;
            addActionMessage("Apertura automatica esercizio " + nextYear + " completata: " + generatedLines + " righe generate");
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Errore apertura automatica esercizio", e);
            addActionError("Errore apertura automatica: " + e.getMessage());
            return ERROR;
        }
    }

    private void ensureSeedData() {
        seedAccountsIfNeeded();
        seedDeadlinesIfNeeded();
    }

    private void ensurePostingConfigSeeded() {
        if (!postingConfigDAO.findAllOrdered().isEmpty()) {
            return;
        }
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_AR_CLIENTI, "1200");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_AP_FORNITORI, "2000");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_REVENUE_MAIN, "3000");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_COST_MAIN, "4000");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_IVA, "2400");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_TAX_EXPENSE, "4300");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_CONTRIBUTION_EXPENSE, "4200");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_TAX_PAYABLE, "2700");
        seedConfigForRegime(AccountingProfile.TaxRegime.FORFETTARIO.name(), KEY_CONTRIBUTION_PAYABLE, "2710");

        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_AR_CLIENTI, "1200");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_AP_FORNITORI, "2000");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_REVENUE_MAIN, "3100");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_COST_MAIN, "4100");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_IVA, "2400");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_TAX_EXPENSE, "4300");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_CONTRIBUTION_EXPENSE, "4200");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_TAX_PAYABLE, "2700");
        seedConfigForRegime(AccountingProfile.TaxRegime.ORDINARIO.name(), KEY_CONTRIBUTION_PAYABLE, "2710");

        seedConfigGeneric(KEY_RATEI_ATTIVI, "1500");
        seedConfigGeneric(KEY_RATEI_PASSIVI, "2500");
        seedConfigGeneric(KEY_RISCONTI_ATTIVI, "1510");
        seedConfigGeneric(KEY_RISCONTI_PASSIVI, "2510");
        seedConfigGeneric(KEY_CONTO_ECONOMICO, "5000");
        seedConfigGeneric(KEY_ASSET_ACCOUNT, "1600");
        seedConfigGeneric(KEY_ACCUM_DEPRECIATION_ACCOUNT, "2605");
        seedConfigGeneric(KEY_DEPRECIATION_EXPENSE_ACCOUNT, "4400");
    }

    private void seedAccountsIfNeeded() {
        ensureSystemAccount("1000", "Cassa", AccountingAccount.Category.ATTIVO);
        ensureSystemAccount("1010", "Banca c/c", AccountingAccount.Category.ATTIVO);
        ensureSystemAccount("1200", "Crediti verso clienti", AccountingAccount.Category.ATTIVO);
        ensureSystemAccount("1500", "Ratei attivi", AccountingAccount.Category.ATTIVO);
        ensureSystemAccount("1510", "Risconti attivi", AccountingAccount.Category.ATTIVO);
        ensureSystemAccount("1600", "Cespiti", AccountingAccount.Category.ATTIVO);

        ensureSystemAccount("2000", "Debiti verso fornitori", AccountingAccount.Category.PASSIVO);
        ensureSystemAccount("2500", "Ratei passivi", AccountingAccount.Category.PASSIVO);
        ensureSystemAccount("2510", "Risconti passivi", AccountingAccount.Category.PASSIVO);
        ensureSystemAccount("2605", "Fondo ammortamento cespiti", AccountingAccount.Category.PASSIVO);
        ensureSystemAccount("2700", "Debiti tributari", AccountingAccount.Category.PASSIVO);
        ensureSystemAccount("2710", "Debiti previdenziali", AccountingAccount.Category.PASSIVO);

        ensureSystemAccount("2400", "Erario c/IVA", AccountingAccount.Category.TRIBUTI);
        ensureSystemAccount("3000", "Ricavi da prestazioni", AccountingAccount.Category.RICAVI);
        ensureSystemAccount("3100", "Ricavi da vendite", AccountingAccount.Category.RICAVI);
        ensureSystemAccount("4000", "Costi servizi", AccountingAccount.Category.COSTI);
        ensureSystemAccount("4100", "Costi merci", AccountingAccount.Category.COSTI);
        ensureSystemAccount("4200", "Contributi previdenziali", AccountingAccount.Category.TRIBUTI);
        ensureSystemAccount("4300", "Imposte sostitutive", AccountingAccount.Category.TRIBUTI);
        ensureSystemAccount("4400", "Ammortamenti", AccountingAccount.Category.COSTI);
        ensureSystemAccount("5000", "Conto economico di chiusura", AccountingAccount.Category.PATRIMONIO_NETTO);
    }

    private void ensureSystemAccount(String code, String name, AccountingAccount.Category category) {
        AccountingAccount existing = accountDAO.findByCode(code);
        if (existing != null) {
            return;
        }
        createSystemAccount(code, name, category);
    }

    private void seedDeadlinesIfNeeded() {
        if (!deadlineDAO.findAllOrdered().isEmpty()) {
            return;
        }
        TaxDeadline iva = new TaxDeadline();
        iva.setTitle("Liquidazione IVA periodica");
        iva.setType(TaxDeadline.DeadlineType.IVA);
        iva.setFrequency(TaxDeadline.Frequency.QUARTERLY);
        iva.setDeadlineDate(nextMonth());
        iva.setStatus(TaxDeadline.DeadlineStatus.OPEN);
        deadlineDAO.save(iva);

        TaxDeadline f24 = new TaxDeadline();
        f24.setTitle("Versamento F24 contributi/imposte");
        f24.setType(TaxDeadline.DeadlineType.F24);
        f24.setFrequency(TaxDeadline.Frequency.MONTHLY);
        f24.setDeadlineDate(nextMonthMid());
        f24.setStatus(TaxDeadline.DeadlineStatus.OPEN);
        deadlineDAO.save(f24);
    }

    private void createSystemAccount(String code, String name, AccountingAccount.Category category) {
        AccountingAccount account = new AccountingAccount();
        account.setCode(code);
        account.setName(name);
        account.setCategory(category);
        account.setSystemAccount(true);
        account.setEnabled(true);
        accountDAO.save(account);
    }

    private Date nextMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        return calendar.getTime();
    }

    private Date nextMonthMid() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 20);
        return calendar.getTime();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal computeAnnualDepreciation(AccountingAsset asset) {
        BigDecimal purchase = safe(asset.getPurchaseAmount());
        BigDecimal residual = safe(asset.getResidualValue());
        BigDecimal depreciable = purchase.subtract(residual);
        if (depreciable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (asset.getUsefulLifeYears() != null && asset.getUsefulLifeYears() > 0) {
            return depreciable.divide(new BigDecimal(asset.getUsefulLifeYears()), 2, RoundingMode.HALF_UP);
        }
        if (asset.getDepreciationRate() != null && asset.getDepreciationRate().compareTo(BigDecimal.ZERO) > 0) {
            return purchase.multiply(asset.getDepreciationRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private int sincronizzaFattureAttive() {
        int created = 0;
        AccountingAccount creditiClienti = resolveConfiguredAccount(KEY_AR_CLIENTI);
        AccountingAccount ricavi = resolveConfiguredAccount(KEY_REVENUE_MAIN);
        AccountingAccount iva = resolveConfiguredAccount(KEY_IVA);

        List<Fattura> fatture = fatturaDAO.findContabilizzabili();
        for (Fattura fattura : fatture) {
            if (fattura == null || fattura.getId() == null) {
                continue;
            }
            AccountingEntry existing = entryDAO.findBySource("FATTURA_ATTIVA", fattura.getId());
            if (existing != null) {
                continue;
            }

            AccountingEntry entry = new AccountingEntry();
            entry.setProtocolNumber(entryDAO.getNextProtocolNumber());
            entry.setEntryDate(fattura.getDataFattura() != null ? fattura.getDataFattura() : new Date());
            entry.setCompetenceDate(entry.getEntryDate());
            entry.setType(AccountingEntry.EntryType.FATTURA_ATTIVA);
            entry.setStatus(AccountingEntry.EntryStatus.POSTED);
            entry.setDescription("Fattura attiva " + fattura.getNumero());
            entry.setDocumentNumber(fattura.getNumero());
            entry.setCounterparty(fattura.getCliente() != null ? fattura.getCliente().getRagioneSociale() : "Cliente");
            entry.setSourceType("FATTURA_ATTIVA");
            entry.setSourceId(fattura.getId());
            entry.setCreatedBy(getCurrentUser());

            BigDecimal imponibile = safe(fattura.getImponibile());
            BigDecimal ivaAmount = safe(fattura.getIva());
            BigDecimal totale = safe(fattura.getTotale());
            if (totale.compareTo(BigDecimal.ZERO) <= 0) {
                totale = imponibile.add(ivaAmount);
            }

            entry.addLine(buildLine(creditiClienti, AccountingEntryLine.LineType.DEBIT, totale, "Credito verso cliente"));
            if (imponibile.compareTo(BigDecimal.ZERO) > 0) {
                entry.addLine(buildLine(ricavi, AccountingEntryLine.LineType.CREDIT, imponibile, "Ricavo da fattura attiva"));
            }
            if (ivaAmount.compareTo(BigDecimal.ZERO) > 0) {
                entry.addLine(buildLine(iva, AccountingEntryLine.LineType.CREDIT, ivaAmount, "IVA vendite"));
            }

            entry.setTotalDebit(totale);
            entry.setTotalCredit(imponibile.add(ivaAmount));
            if (entry.getTotalDebit().compareTo(entry.getTotalCredit()) == 0) {
                entryDAO.saveOrUpdateEntry(entry);
                created++;
            }
        }
        return created;
    }

    private int sincronizzaFatturePassive() {
        int created = 0;
        AccountingAccount debitiFornitori = resolveConfiguredAccount(KEY_AP_FORNITORI);
        AccountingAccount costi = resolveConfiguredAccount(KEY_COST_MAIN);
        AccountingAccount iva = resolveConfiguredAccount(KEY_IVA);

        List<FatturaPassiva> fatturePassive = fatturaPassivaDAO.findAllOrdered();
        for (FatturaPassiva fattura : fatturePassive) {
            if (fattura == null || fattura.getId() == null) {
                continue;
            }
            AccountingEntry existing = entryDAO.findBySource("FATTURA_PASSIVA", fattura.getId());
            if (existing != null) {
                continue;
            }

            AccountingEntry entry = new AccountingEntry();
            entry.setProtocolNumber(entryDAO.getNextProtocolNumber());
            entry.setEntryDate(fattura.getDataFattura() != null ? fattura.getDataFattura() : new Date());
            entry.setCompetenceDate(entry.getEntryDate());
            entry.setType(AccountingEntry.EntryType.FATTURA_PASSIVA);
            entry.setStatus(AccountingEntry.EntryStatus.POSTED);
            entry.setDescription("Fattura passiva " + fattura.getNumero());
            entry.setDocumentNumber(fattura.getNumero());
            entry.setCounterparty(fattura.getFornitoreNome() != null ? fattura.getFornitoreNome() : "Fornitore");
            entry.setSourceType("FATTURA_PASSIVA");
            entry.setSourceId(fattura.getId());
            entry.setCreatedBy(getCurrentUser());

            BigDecimal imponibile = safe(fattura.getImponibile());
            BigDecimal ivaAmount = safe(fattura.getIva());
            BigDecimal totale = safe(fattura.getTotale());
            if (totale.compareTo(BigDecimal.ZERO) <= 0) {
                totale = imponibile.add(ivaAmount);
            }

            if (imponibile.compareTo(BigDecimal.ZERO) > 0) {
                entry.addLine(buildLine(costi, AccountingEntryLine.LineType.DEBIT, imponibile, "Costo da fattura passiva"));
            }
            if (ivaAmount.compareTo(BigDecimal.ZERO) > 0) {
                entry.addLine(buildLine(iva, AccountingEntryLine.LineType.DEBIT, ivaAmount, "IVA acquisti"));
            }
            entry.addLine(buildLine(debitiFornitori, AccountingEntryLine.LineType.CREDIT, totale, "Debito verso fornitore"));

            entry.setTotalDebit(imponibile.add(ivaAmount));
            entry.setTotalCredit(totale);
            if (entry.getTotalDebit().compareTo(entry.getTotalCredit()) == 0) {
                entryDAO.saveOrUpdateEntry(entry);
                created++;
            }
        }
        return created;
    }

    private AccountingAccount resolveConfiguredAccount(String logicalKey) {
        AccountingProfile activeProfile = profileDAO.getDefaultProfile();
        String regime = activeProfile != null && activeProfile.getTaxRegime() != null ? activeProfile.getTaxRegime().name() : null;
        String form = activeProfile != null && activeProfile.getBusinessForm() != null ? activeProfile.getBusinessForm().name() : null;

        AccountingPostingConfig config = postingConfigDAO.findConfig(logicalKey, regime, form);
        if (config == null || config.getAccount() == null) {
            throw new RuntimeException("Configurazione conto mancante per chiave logica: " + logicalKey);
        }
        return accountDAO.findById(config.getAccount().getId());
    }

    private void seedConfigForRegime(String regime, String logicalKey, String accountCode) {
        AccountingAccount account = accountDAO.findByCode(accountCode);
        if (account == null) {
            return;
        }
        AccountingPostingConfig config = postingConfigDAO.findConfig(logicalKey, regime, null);
        if (config == null || !logicalKey.equals(config.getLogicalKey()) || !equalsNullable(config.getTaxRegime(), regime)) {
            config = new AccountingPostingConfig();
            config.setLogicalKey(logicalKey);
            config.setTaxRegime(regime);
            config.setBusinessForm(null);
        }
        config.setAccount(account);
        config.setActive(true);
        config.setDescription("Config automatica regime " + regime + " per " + logicalKey);
        postingConfigDAO.saveOrUpdate(config);
    }

    private void seedConfigGeneric(String logicalKey, String accountCode) {
        AccountingAccount account = accountDAO.findByCode(accountCode);
        if (account == null) {
            return;
        }
        AccountingPostingConfig config = postingConfigDAO.findConfig(logicalKey, null, null);
        if (config == null || !logicalKey.equals(config.getLogicalKey())) {
            config = new AccountingPostingConfig();
            config.setLogicalKey(logicalKey);
            config.setTaxRegime(null);
            config.setBusinessForm(null);
        }
        config.setAccount(account);
        config.setActive(true);
        config.setDescription("Config generica per " + logicalKey);
        postingConfigDAO.saveOrUpdate(config);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty() || "ANY".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private boolean equalsNullable(String a, String b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    private int resolveAnnoEsercizio() {
        if (annoEsercizio != null) {
            return annoEsercizio;
        }
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    private int yearOf(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    private AccountingEntryLine buildLine(AccountingAccount account, AccountingEntryLine.LineType lineType,
                                          BigDecimal amount, String description) {
        AccountingEntryLine line = new AccountingEntryLine();
        line.setAccount(account);
        line.setLineType(lineType);
        line.setAmount(safe(amount));
        line.setDescription(description);
        return line;
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new com.itextpdf.text.Phrase(text));
        cell.setBackgroundColor(new com.itextpdf.text.BaseColor(230, 230, 230));
        table.addCell(cell);
    }

    private void applyRegistrazioneDateInputs() {
        if (registrazione == null) {
            return;
        }
        if (registrazioneEntryDate != null && !registrazioneEntryDate.trim().isEmpty()) {
            registrazione.setEntryDate(parseDate(registrazioneEntryDate));
        }
        if (registrazioneCompetenceDate != null && !registrazioneCompetenceDate.trim().isEmpty()) {
            registrazione.setCompetenceDate(parseDate(registrazioneCompetenceDate));
        }
    }

    private Date parseDate(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value);
        } catch (Exception e) {
            throw new RuntimeException("Formato data non valido: " + value, e);
        }
    }

    private String formatDate(Date value) {
        return value == null ? null : new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private Long safe(Long value) {
        return value == null ? 0L : value;
    }

    public String getFormattedToday() {
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date());
    }

    private User getCurrentUser() {
        Map<String, Object> session = ActionContext.getContext().getSession();
        return (User) session.get("currentUser");
    }

    public AccountingProfile getProfilo() { return profilo; }
    public void setProfilo(AccountingProfile profilo) { this.profilo = profilo; }
    public AccountingAccount getConto() { return conto; }
    public void setConto(AccountingAccount conto) { this.conto = conto; }
    public AccountingEntry getRegistrazione() { return registrazione; }
    public void setRegistrazione(AccountingEntry registrazione) { this.registrazione = registrazione; }
    public TaxDeadline getScadenza() { return scadenza; }
    public void setScadenza(TaxDeadline scadenza) { this.scadenza = scadenza; }
    public List<AccountingAccount> getConti() { return conti; }
    public List<AccountingEntry> getRegistrazioni() { return registrazioni; }
    public List<TaxDeadline> getScadenze() { return scadenze; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long[] getLineAccountIds() { return lineAccountIds; }
    public void setLineAccountIds(Long[] lineAccountIds) { this.lineAccountIds = lineAccountIds; }
    public String[] getLineTypes() { return lineTypes; }
    public void setLineTypes(String[] lineTypes) { this.lineTypes = lineTypes; }
    public String[] getLineDescriptions() { return lineDescriptions; }
    public void setLineDescriptions(String[] lineDescriptions) { this.lineDescriptions = lineDescriptions; }
    public String[] getLineAmounts() { return lineAmounts; }
    public void setLineAmounts(String[] lineAmounts) { this.lineAmounts = lineAmounts; }
    public String getRegistrazioneEntryDate() { return registrazioneEntryDate; }
    public void setRegistrazioneEntryDate(String registrazioneEntryDate) { this.registrazioneEntryDate = registrazioneEntryDate; }
    public String getRegistrazioneCompetenceDate() { return registrazioneCompetenceDate; }
    public void setRegistrazioneCompetenceDate(String registrazioneCompetenceDate) { this.registrazioneCompetenceDate = registrazioneCompetenceDate; }
    public String getScadenzaDate() { return scadenzaDate; }
    public String getCespitePurchaseDate() { return cespitePurchaseDate; }
    public void setCespitePurchaseDate(String cespitePurchaseDate) { this.cespitePurchaseDate = cespitePurchaseDate; }
    public void setScadenzaDate(String scadenzaDate) {
        this.scadenzaDate = scadenzaDate;
        if (scadenza == null) {
            scadenza = new TaxDeadline();
        }
        if (scadenzaDate != null && !scadenzaDate.trim().isEmpty()) {
            scadenza.setDeadlineDate(parseDate(scadenzaDate));
        }
    }
    public BigDecimal getTotalDare() { return totalDare; }
    public BigDecimal getTotalAvere() { return totalAvere; }
    public Long getPostedEntries() { return postedEntries; }
    public Long getOpenDeadlines() { return openDeadlines; }
    public Long getOverdueDeadlines() { return overdueDeadlines; }
    public AccountingProfile.BusinessForm[] getBusinessForms() { return AccountingProfile.BusinessForm.values(); }
    public AccountingProfile.TaxRegime[] getTaxRegimes() { return AccountingProfile.TaxRegime.values(); }
    public AccountingProfile.VatFrequency[] getVatFrequencies() { return AccountingProfile.VatFrequency.values(); }
    public AccountingAccount.Category[] getAccountCategories() { return AccountingAccount.Category.values(); }
    public AccountingEntry.EntryType[] getEntryTypes() { return AccountingEntry.EntryType.values(); }
    public AccountingEntry.EntryStatus[] getEntryStatuses() { return AccountingEntry.EntryStatus.values(); }
    public TaxDeadline.DeadlineType[] getDeadlineTypes() { return TaxDeadline.DeadlineType.values(); }
    public TaxDeadline.Frequency[] getDeadlineFrequencies() { return TaxDeadline.Frequency.values(); }
    public AccountingAsset.AssetType[] getAssetTypes() { return AccountingAsset.AssetType.values(); }
    public AccountingAsset.AssetStatus[] getAssetStatuses() { return AccountingAsset.AssetStatus.values(); }

    public List<AccountingBalanceRow> getBilancioRows() { return bilancioRows; }
    public List<AccountingEntryLine> getMastrinoLines() { return mastrinoLines; }
    public AccountingAccount getMastrinoConto() { return mastrinoConto; }
    public AccountingAsset getCespite() { return cespite; }
    public void setCespite(AccountingAsset cespite) { this.cespite = cespite; }
    public List<AccountingAsset> getCespiti() { return cespiti; }
    public BigDecimal getBilancioTotaleDare() { return bilancioTotaleDare; }
    public BigDecimal getBilancioTotaleAvere() { return bilancioTotaleAvere; }
    public BigDecimal getBilancioDelta() { return bilancioDelta; }
    public BigDecimal getMastrinoSaldo() { return mastrinoSaldo; }
    public Long getSyncedActive() { return syncedActive; }
    public Long getSyncedPassive() { return syncedPassive; }
    public InputStream getInputStream() { return inputStream; }
    public String getContentDisposition() { return contentDisposition; }
    public Integer getAnnoEsercizio() { return annoEsercizio; }
    public void setAnnoEsercizio(Integer annoEsercizio) { this.annoEsercizio = annoEsercizio; }
    public List<AccountingPostingConfig> getPostingConfigs() { return postingConfigs; }
    public String[] getConfigKeys() { return configKeys; }
    public void setConfigKeys(String[] configKeys) { this.configKeys = configKeys; }
    public String[] getConfigRegimes() { return configRegimes; }
    public void setConfigRegimes(String[] configRegimes) { this.configRegimes = configRegimes; }
    public String[] getConfigForms() { return configForms; }
    public void setConfigForms(String[] configForms) { this.configForms = configForms; }
    public Long[] getConfigAccountIds() { return configAccountIds; }
    public void setConfigAccountIds(Long[] configAccountIds) { this.configAccountIds = configAccountIds; }
    public Long getGeneratedRateiRisconti() { return generatedRateiRisconti; }
    public Long getGeneratedChiusure() { return generatedChiusure; }
    public Long getGeneratedAperture() { return generatedAperture; }
    public Long getGeneratedAmmortamenti() { return generatedAmmortamenti; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getReportDateFrom() { return reportDateFrom; }
    public void setReportDateFrom(String reportDateFrom) { this.reportDateFrom = reportDateFrom; }
    public String getReportDateTo() { return reportDateTo; }
    public void setReportDateTo(String reportDateTo) { this.reportDateTo = reportDateTo; }
    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }
    public Long getReportAccountId() { return reportAccountId; }
    public void setReportAccountId(Long reportAccountId) { this.reportAccountId = reportAccountId; }
    public String getReportDocumentType() { return reportDocumentType; }
    public void setReportDocumentType(String reportDocumentType) { this.reportDocumentType = reportDocumentType; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public List<ReportDefinition> getReportDefinitions() { return reportDefinitions; }
    public List<String> getPreviewHeaders() { return previewHeaders; }
    public List<List<String>> getPreviewRows() { return previewRows; }
    public String getPreviewTitle() { return previewTitle; }
    public List<AccountingAccount> getReportAccounts() { return reportAccounts; }
    public List<String> getTrendLabels() { return trendLabels; }
    public List<BigDecimal> getTrendRicavi() { return trendRicavi; }
    public List<BigDecimal> getTrendCosti() { return trendCosti; }
    public List<BigDecimal> getTrendRisultato() { return trendRisultato; }
    public List<AccountingReportPreset> getReportPresets() { return reportPresets; }
    public Integer getPreviewPage() { return previewPage; }
    public void setPreviewPage(Integer previewPage) { this.previewPage = previewPage; }
    public Integer getPreviewPageSize() { return previewPageSize; }
    public void setPreviewPageSize(Integer previewPageSize) { this.previewPageSize = previewPageSize; }
    public Integer getPreviewTotalPages() { return previewTotalPages; }
    public Long getPreviewTotalRows() { return previewTotalRows; }
    public String getSelectedPreset() { return selectedPreset; }
    public void setSelectedPreset(String selectedPreset) { this.selectedPreset = selectedPreset; }
    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }
    public String[] getAvailablePostingKeys() {
        return new String[]{
                KEY_AR_CLIENTI, KEY_AP_FORNITORI, KEY_REVENUE_MAIN, KEY_COST_MAIN, KEY_IVA,
                KEY_RATEI_ATTIVI, KEY_RATEI_PASSIVI, KEY_RISCONTI_ATTIVI, KEY_RISCONTI_PASSIVI,
                KEY_CONTO_ECONOMICO, KEY_TAX_EXPENSE, KEY_CONTRIBUTION_EXPENSE, KEY_TAX_PAYABLE, KEY_CONTRIBUTION_PAYABLE,
                KEY_ASSET_ACCOUNT, KEY_ACCUM_DEPRECIATION_ACCOUNT, KEY_DEPRECIATION_EXPENSE_ACCOUNT
        };
    }

    public Map<String, String> getAvailableReportTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        for (ReportDefinition definition : buildReportDefinitions()) {
            types.put(definition.key, definition.title);
        }
        return types;
    }

    public Map<String, String> getAvailableReportStatuses() {
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("ANY", "Tutti");
        statuses.put("POSTED", "POSTED");
        statuses.put("DRAFT", "DRAFT");
        return statuses;
    }

    public Map<String, String> getAvailableDocumentTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("ANY", "Tutti");
        types.put("MANUALE", "Manuale (senza fonte)");
        types.put("FATTURA_ATTIVA", "Fattura attiva");
        types.put("FATTURA_PASSIVA", "Fattura passiva");
        types.put("RATEI_RISCONTI_AUTO", "Ratei/Risconti automatici");
        types.put("CHIUSURA_AUTO", "Chiusura automatica");
        types.put("APERTURA_AUTO", "Apertura automatica");
        types.put("INCASSO", "Incasso");
        types.put("PAGAMENTO", "Pagamento");
        return types;
    }

    public Map<String, String> getAvailablePreviewPageSizes() {
        Map<String, String> sizes = new LinkedHashMap<>();
        sizes.put("25", "25");
        sizes.put("50", "50");
        sizes.put("100", "100");
        sizes.put("200", "200");
        return sizes;
    }

    public Map<String, String> getAvailableReportPresets() {
        Map<String, String> presets = new LinkedHashMap<>();
        if (reportPresets == null) {
            return presets;
        }
        for (AccountingReportPreset preset : reportPresets) {
            presets.put(preset.getName(), preset.getName());
        }
        return presets;
    }

    public static class ReportDefinition {
        private final String key;
        private final String title;
        private final String description;

        private ReportDefinition(String key, String title, String description) {
            this.key = key;
            this.title = title;
            this.description = description;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }

    private static class ReportTable {
        private final String title;
        private final String fileBaseName;
        private final List<String> headers = new ArrayList<>();
        private final List<List<String>> rows = new ArrayList<>();

        private ReportTable(String title, String fileBaseName) {
            this.title = title;
            this.fileBaseName = fileBaseName;
        }
    }

    private static class PhraseSafe {
        private final com.itextpdf.text.Phrase value;

        private PhraseSafe(String text) {
            this.value = new com.itextpdf.text.Phrase(text == null ? "" : text);
        }
    }
}
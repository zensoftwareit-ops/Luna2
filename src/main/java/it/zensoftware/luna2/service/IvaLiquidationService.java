package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.AccountingEntryDAO;
import it.zensoftware.luna2.dao.AccountingProfileDAO;
import it.zensoftware.luna2.dao.IvaLiquidationDAO;
import it.zensoftware.luna2.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;

public class IvaLiquidationService {

    private static final Logger logger = LogManager.getLogger(IvaLiquidationService.class);

    private final IvaLiquidationDAO liquidationDAO;
    private final AccountingEntryDAO entryDAO;
    private final AccountingProfileDAO profileDAO;

    public IvaLiquidationService() {
        this.liquidationDAO = new IvaLiquidationDAO();
        this.entryDAO = new AccountingEntryDAO();
        this.profileDAO = new AccountingProfileDAO();
    }

    public IvaLiquidationService(IvaLiquidationDAO liquidationDAO, AccountingEntryDAO entryDAO, AccountingProfileDAO profileDAO) {
        this.liquidationDAO = liquidationDAO;
        this.entryDAO = entryDAO;
        this.profileDAO = profileDAO;
    }

    public IvaLiquidation calculateMonthlyLiquidation(Integer year, Integer month) {
        String period = String.format("%d-%02d", year, month);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = cal.getTime();

        BigDecimal ivaInvoices = getIvaInvoices(startDate, endDate);
        BigDecimal ivaCosts = getIvaCosts(startDate, endDate);
        BigDecimal netIva = computeNetIva(ivaInvoices, ivaCosts);
        Date dueDate = computeDueDate(endDate, AccountingProfile.VatFrequency.MENSILE);

        IvaLiquidation liquidation = new IvaLiquidation();
        liquidation.setLiquidationPeriod(period);
        liquidation.setLiquidationDate(startDate);
        liquidation.setEndDate(endDate);
        liquidation.setPeriodType(IvaLiquidation.PeriodType.MONTHLY);
        liquidation.setIvaInvoicesAmount(ivaInvoices);
        liquidation.setIvaCostsAmount(ivaCosts);
        liquidation.setNetIvaAmount(netIva);
        liquidation.setAmountDue(netIva);
        liquidation.setDueDate(dueDate);
        liquidation.setStatus(IvaLiquidation.LiquidationStatus.CALCULATED);

        try {
            liquidationDAO.save(liquidation);
            logger.info("Created monthly IVA liquidation for period {}", period);
        } catch (Exception e) {
            logger.error("Error saving monthly liquidation", e);
        }

        return liquidation;
    }

    public IvaLiquidation calculateQuarterlyLiquidation(Integer year, Integer quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
        }

        String period = String.format("%d-Q%d", year, quarter);
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = startMonth + 2;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, startMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = cal.getTime();

        cal.set(Calendar.MONTH, endMonth - 1);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = cal.getTime();

        BigDecimal ivaInvoices = getIvaInvoices(startDate, endDate);
        BigDecimal ivaCosts = getIvaCosts(startDate, endDate);
        BigDecimal netIva = computeNetIva(ivaInvoices, ivaCosts);
        Date dueDate = computeDueDate(endDate, AccountingProfile.VatFrequency.TRIMESTRALE);

        IvaLiquidation liquidation = new IvaLiquidation();
        liquidation.setLiquidationPeriod(period);
        liquidation.setLiquidationDate(startDate);
        liquidation.setEndDate(endDate);
        liquidation.setPeriodType(IvaLiquidation.PeriodType.QUARTERLY);
        liquidation.setIvaInvoicesAmount(ivaInvoices);
        liquidation.setIvaCostsAmount(ivaCosts);
        liquidation.setNetIvaAmount(netIva);
        liquidation.setAmountDue(netIva);
        liquidation.setDueDate(dueDate);
        liquidation.setStatus(IvaLiquidation.LiquidationStatus.CALCULATED);

        try {
            liquidationDAO.save(liquidation);
            logger.info("Created quarterly IVA liquidation for period {}", period);
        } catch (Exception e) {
            logger.error("Error saving quarterly liquidation", e);
        }

        return liquidation;
    }

    public BigDecimal getIvaInvoices(Date from, Date to) {
        try {
            List<AccountingEntry> entries = entryDAO.findAllOrdered();
            BigDecimal total = BigDecimal.ZERO;

            for (AccountingEntry entry : entries) {
                if (entry.getEntryDate() == null) continue;
                if (entry.getEntryDate().before(from) || entry.getEntryDate().after(to)) continue;
                if (!"FATTURA_ATTIVA".equals(entry.getSourceType())) continue;

                AccountingEntry full = entryDAO.findWithLines(entry.getId());
                if (full != null && full.getLines() != null) {
                    for (AccountingEntryLine line : full.getLines()) {
                        if (line.getAccount() != null &&
                            line.getAccount().getCategory() == AccountingAccount.Category.TRIBUTI) {
                            BigDecimal amount = line.getAmount();
                            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                                total = total.add(amount);
                            }
                        }
                    }
                }
            }
            return total;
        } catch (Exception e) {
            logger.error("Error calculating IVA invoices", e);
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getIvaCosts(Date from, Date to) {
        try {
            List<AccountingEntry> entries = entryDAO.findAllOrdered();
            BigDecimal total = BigDecimal.ZERO;

            for (AccountingEntry entry : entries) {
                if (entry.getEntryDate() == null) continue;
                if (entry.getEntryDate().before(from) || entry.getEntryDate().after(to)) continue;
                if (!"FATTURA_PASSIVA".equals(entry.getSourceType())) continue;

                AccountingEntry full = entryDAO.findWithLines(entry.getId());
                if (full != null && full.getLines() != null) {
                    for (AccountingEntryLine line : full.getLines()) {
                        if (line.getAccount() != null &&
                            line.getAccount().getCategory() == AccountingAccount.Category.TRIBUTI) {
                            BigDecimal amount = line.getAmount();
                            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                                total = total.add(amount);
                            }
                        }
                    }
                }
            }
            return total;
        } catch (Exception e) {
            logger.error("Error calculating IVA costs", e);
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal computeNetIva(BigDecimal invoices, BigDecimal costs) {
        if (invoices == null) invoices = BigDecimal.ZERO;
        if (costs == null) costs = BigDecimal.ZERO;

        BigDecimal net = invoices.subtract(costs);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    public Date computeDueDate(Date endDate, AccountingProfile.VatFrequency frequency) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(endDate);

        if (frequency == AccountingProfile.VatFrequency.MENSILE) {
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, 16);
        } else {
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.DAY_OF_MONTH, 16);
        }

        return cal.getTime();
    }

    public void generateAutoLiquidations(Integer year) {
        AccountingProfile profile = profileDAO.getDefaultProfile();
        if (profile == null) {
            logger.warn("No accounting profile found");
            return;
        }

        logger.info("Generating auto liquidations for year {} with frequency {}", year, profile.getVatFrequency());

        try {
            if (profile.getVatFrequency() == AccountingProfile.VatFrequency.MENSILE) {
                for (int month = 1; month <= 12; month++) {
                    String period = String.format("%d-%02d", year, month);
                    IvaLiquidation existing = liquidationDAO.findByPeriod(period);
                    if (existing == null) {
                        calculateMonthlyLiquidation(year, month);
                    } else {
                        logger.info("Liquidation for period {} already exists", period);
                    }
                }
            } else {
                for (int quarter = 1; quarter <= 4; quarter++) {
                    String period = String.format("%d-Q%d", year, quarter);
                    IvaLiquidation existing = liquidationDAO.findByPeriod(period);
                    if (existing == null) {
                        calculateQuarterlyLiquidation(year, quarter);
                    } else {
                        logger.info("Liquidation for period {} already exists", period);
                    }
                }
            }
            logger.info("Auto liquidation generation completed for year {}", year);
        } catch (Exception e) {
            logger.error("Error generating auto liquidations", e);
        }
    }

    public List<IvaLiquidation> getUpcomingDueLiquidations(int days) {
        List<IvaLiquidation> unpaid = liquidationDAO.findUnpaid();
        List<IvaLiquidation> upcoming = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date cutoffDate = cal.getTime();

        for (IvaLiquidation liq : unpaid) {
            if (liq.getDueDate() != null && liq.getDueDate().before(cutoffDate)) {
                upcoming.add(liq);
            }
        }

        upcoming.sort((a, b) -> {
            if (a.getDueDate() == null) return 1;
            if (b.getDueDate() == null) return -1;
            return a.getDueDate().compareTo(b.getDueDate());
        });

        return upcoming;
    }

    public void updateLiquidationStatus(Long liquidationId, IvaLiquidation.LiquidationStatus newStatus) {
        try {
            IvaLiquidation liq = liquidationDAO.findById(liquidationId);
            if (liq != null) {
                liq.setStatus(newStatus);
                liquidationDAO.update(liq);
                logger.info("Updated liquidation {} status to {}", liquidationId, newStatus);
            }
        } catch (Exception e) {
            logger.error("Error updating liquidation status", e);
        }
    }
}

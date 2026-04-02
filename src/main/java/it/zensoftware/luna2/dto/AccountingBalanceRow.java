package it.zensoftware.luna2.dto;

import java.math.BigDecimal;

public class AccountingBalanceRow {
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;

    public AccountingBalanceRow() {
    }

    public AccountingBalanceRow(Long accountId, String accountCode, String accountName) {
        this.accountId = accountId;
        this.accountCode = accountCode;
        this.accountName = accountName;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}

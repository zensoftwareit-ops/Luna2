package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.AccountingProfile;

import java.util.List;

public class AccountingProfileDAO extends GenericDAOImpl<AccountingProfile, Long> {

    public AccountingProfileDAO() {
        super(AccountingProfile.class);
    }

    public AccountingProfile getDefaultProfile() {
        List<AccountingProfile> profiles = findAll();
        return profiles.isEmpty() ? null : profiles.get(0);
    }
}
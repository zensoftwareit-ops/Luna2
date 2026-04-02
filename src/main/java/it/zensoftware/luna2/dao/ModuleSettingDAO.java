package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.ModuleSetting;
import it.zensoftware.luna2.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for module settings
 */
public class ModuleSettingDAO {

    public List<ModuleSetting> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ModuleSetting ORDER BY name", ModuleSetting.class).list();
        }
    }

    public ModuleSetting findByCode(String code) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM ModuleSetting WHERE code = :code", ModuleSetting.class)
                    .setParameter("code", code)
                    .uniqueResult();
        }
    }

    public Map<String, Boolean> getEnabledMap() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("CORE", true);
        map.put("MAGAZZINO", true);
        map.put("CRM", true);
        map.put("CRM_BROKER_AUTO", false);
        map.put("PRODUZIONE", false);
        map.put("AI", false);
        map.put("CONTABILITA", false);

        List<ModuleSetting> settings = findAll();
        for (ModuleSetting setting : settings) {
            map.put(setting.getCode(), Boolean.TRUE.equals(setting.getEnabled()));
        }
        return map;
    }

    public void saveOrUpdate(ModuleSetting setting) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.saveOrUpdate(setting);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }
}

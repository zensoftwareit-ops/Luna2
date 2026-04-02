package it.zensoftware.luna2.action;

import it.zensoftware.luna2.dao.AdminOperationDAO;
import it.zensoftware.luna2.model.AdminOperation;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmministrazioneActionTest {

    private static class FakeAdminOperationDAO extends AdminOperationDAO {
        private final Map<Long, AdminOperation> byId = new HashMap<>();
        private long seq = 1;

        @Override
        public AdminOperation save(AdminOperation entity) {
            if (entity.getId() == null) {
                entity.setId(seq++);
            }
            byId.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public void update(AdminOperation entity) {
            byId.put(entity.getId(), entity);
        }

        @Override
        public AdminOperation findById(Long id) {
            return byId.get(id);
        }

        @Override
        public List<AdminOperation> findAllOrdered() {
            List<AdminOperation> out = new ArrayList<>(byId.values());
            out.sort((a, b) -> Long.compare(b.getId(), a.getId()));
            return out;
        }

        @Override
        public List<AdminOperation> findByArea(AdminOperation.Area area) {
            List<AdminOperation> out = new ArrayList<>();
            for (AdminOperation operation : byId.values()) {
                if (operation.getArea() == area) {
                    out.add(operation);
                }
            }
            return out;
        }

        @Override
        public long countByStatus(AdminOperation.Status status) {
            return byId.values().stream().filter(v -> v.getStatus() == status).count();
        }

        @Override
        public long countOverdue(Date today) {
            return byId.values().stream()
                    .filter(v -> v.getStatus() != AdminOperation.Status.COMPLETED)
                    .filter(v -> v.getDueDate() != null && v.getDueDate().before(today))
                    .count();
        }

        @Override
        public java.math.BigDecimal sumOpenAmountByArea(AdminOperation.Area area) {
            return byId.values().stream()
                    .filter(v -> v.getArea() == area)
                    .filter(v -> v.getStatus() != AdminOperation.Status.COMPLETED)
                    .map(v -> v.getAmountDue() == null ? java.math.BigDecimal.ZERO : v.getAmountDue())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        }
    }

    private static class TestableAmministrazioneAction extends AmministrazioneAction {
        @Override
        protected Long getCurrentUserId() {
            return 42L;
        }

        @Override
        public String dashboard() {
            return super.dashboard();
        }
    }

    @Test
    public void saveAndCompleteOperationShouldWork() throws Exception {
        FakeAdminOperationDAO dao = new FakeAdminOperationDAO();
        TestableAmministrazioneAction action = new TestableAmministrazioneAction();
        action.setOperationDAO(dao);

        AdminOperation op = new AdminOperation();
        op.setArea(AdminOperation.Area.ADEMPIMENTI_FISCALI);
        op.setOperationType("F24");
        op.setTitle("Predisposizione F24 mese");
        op.setStatus(AdminOperation.Status.READY);
        action.setOperation(op);
        action.setDueDateInput("2026-04-16");

        assertEquals("success", action.saveOperation());
        assertTrue(dao.findAllOrdered().size() >= 1);

        Long savedId = dao.findAllOrdered().get(0).getId();
        action.setId(savedId);
        assertEquals("success", action.completeOperation());
        assertEquals(AdminOperation.Status.COMPLETED, dao.findById(savedId).getStatus());
    }
}

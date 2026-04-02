package it.zensoftware.luna2.action;

import it.zensoftware.luna2.dao.ApprovalRequestDAO;
import it.zensoftware.luna2.model.ApprovalRequest;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApprovalWorkflowActionTest {

    private static class TestableApprovalWorkflowAction extends ApprovalWorkflowAction {
        @Override
        protected Long getCurrentUserId() {
            return 1L;
        }

        @Override
        protected Long getCurrentCompanyId() {
            return 1L;
        }
    }

    private static class FakeApprovalRequestDAO extends ApprovalRequestDAO {
        private final List<ApprovalRequest> byRequester = new ArrayList<>();
        private final List<ApprovalRequest> pending = new ArrayList<>();
        private boolean approved;
        private boolean rejected;

        @Override
        public List<ApprovalRequest> findByRequester(Long companyId, Long requesterId) {
            return byRequester;
        }

        @Override
        public List<ApprovalRequest> findPendingApprovals(Long companyId, Long approverId) {
            return pending;
        }

        @Override
        public ApprovalRequest save(ApprovalRequest entity) {
            byRequester.add(entity);
            pending.add(entity);
            return entity;
        }

        @Override
        public void approve(Long requestId, Long approverId) {
            approved = true;
        }

        @Override
        public void reject(Long requestId, Long approverId, String rejectionReason) {
            rejected = true;
        }
    }

    @Test
    public void ferieCreateShouldReturnSuccess() {
        ApprovalWorkflowAction action = new TestableApprovalWorkflowAction();
        FakeApprovalRequestDAO dao = new FakeApprovalRequestDAO();
        action.setApprovalRequestDAO(dao);

        ApprovalRequest request = new ApprovalRequest();
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(7));
        request.setDescription("Ferie estive");
        action.setApprovalRequest(request);

        String result = action.ferieCreate();

        assertEquals("success", result);
        assertEquals("SUBMITTED", request.getStatus());
        assertEquals(Integer.valueOf(3), request.getDaysRequested());
    }

    @Test
    public void ferieListShouldLoadRequests() {
        ApprovalWorkflowAction action = new TestableApprovalWorkflowAction();
        FakeApprovalRequestDAO dao = new FakeApprovalRequestDAO();
        action.setApprovalRequestDAO(dao);

        ApprovalRequest request = new ApprovalRequest();
        request.setStartDate(LocalDate.now().plusDays(2));
        request.setEndDate(LocalDate.now().plusDays(3));
        dao.byRequester.add(request);

        String result = action.ferieList();

        assertEquals("success", result);
        assertNotNull(action.getApprovalRequests());
        assertEquals(1, action.getApprovalRequests().size());
    }

    @Test
    public void approveShouldReturnSuccess() {
        ApprovalWorkflowAction action = new TestableApprovalWorkflowAction();
        FakeApprovalRequestDAO dao = new FakeApprovalRequestDAO();
        action.setApprovalRequestDAO(dao);
        action.setRequestId(100L);

        String result = action.approve();

        assertEquals("success", result);
        assertTrue(dao.approved);
    }

    @Test
    public void rejectShouldRequireReason() {
        ApprovalWorkflowAction action = new TestableApprovalWorkflowAction();
        FakeApprovalRequestDAO dao = new FakeApprovalRequestDAO();
        action.setApprovalRequestDAO(dao);
        action.setRequestId(101L);

        String result = action.reject();

        assertEquals("input", result);
        assertFalse(dao.rejected);
    }
}

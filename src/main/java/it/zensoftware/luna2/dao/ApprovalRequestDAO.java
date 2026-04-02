package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.ApprovalRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public class ApprovalRequestDAO extends GenericDAOImpl<ApprovalRequest, Long> {

    private static final Logger logger = LogManager.getLogger(ApprovalRequestDAO.class);

    public ApprovalRequestDAO() {
        super(ApprovalRequest.class);
    }

    public List<ApprovalRequest> findByRequester(Long companyId, Long requesterId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM ApprovalRequest ar WHERE ar.companyId = :companyId AND ar.requesterId = :requesterId ORDER BY ar.createdDate DESC",
                            ApprovalRequest.class)
                    .setParameter("companyId", companyId)
                    .setParameter("requesterId", requesterId)
                    .getResultList();
        }
    }

    public List<ApprovalRequest> findPendingApprovals(Long companyId, Long approverId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "FROM ApprovalRequest ar WHERE ar.companyId = :companyId AND ar.approverId = :approverId AND ar.status = :status ORDER BY ar.submittedDate ASC, ar.createdDate ASC",
                            ApprovalRequest.class)
                    .setParameter("companyId", companyId)
                    .setParameter("approverId", approverId)
                    .setParameter("status", "SUBMITTED")
                    .getResultList();
        }
    }

    public long countPendingByApprover(Long companyId, Long approverId) {
        try (Session session = getSession()) {
            Long count = session.createQuery(
                            "SELECT COUNT(ar.id) FROM ApprovalRequest ar WHERE ar.companyId = :companyId AND ar.approverId = :approverId AND ar.status = :status",
                            Long.class)
                    .setParameter("companyId", companyId)
                    .setParameter("approverId", approverId)
                    .setParameter("status", "SUBMITTED")
                    .uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public void submit(Long requestId, Long approverId) {
        updateStatus(requestId, "SUBMITTED", approverId, null);
    }

    public void approve(Long requestId, Long approverId) {
        updateStatus(requestId, "APPROVED", approverId, null);
    }

    public void reject(Long requestId, Long approverId, String rejectionReason) {
        updateStatus(requestId, "REJECTED", approverId, rejectionReason);
    }

    private void updateStatus(Long requestId, String status, Long actorId, String rejectionReason) {
        Transaction tx = null;
        try (Session session = getSession()) {
            tx = session.beginTransaction();
            ApprovalRequest request = session.get(ApprovalRequest.class, requestId);
            if (request == null) {
                throw new IllegalArgumentException("Richiesta non trovata: id=" + requestId);
            }

            request.setStatus(status);
            request.setApproverId(actorId);
            request.setUpdatedDate(LocalDateTime.now());

            if ("SUBMITTED".equals(status)) {
                request.setSubmittedDate(LocalDateTime.now());
            }
            if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
                request.setApprovalDate(LocalDateTime.now());
            }
            if ("REJECTED".equals(status)) {
                request.setRejectionReason(rejectionReason);
            }

            session.merge(request);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error("Errore aggiornando stato richiesta {} a {}", requestId, status, ex);
            throw new RuntimeException("Errore aggiornamento stato richiesta", ex);
        }
    }
}

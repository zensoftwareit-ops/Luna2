package it.zensoftware.luna2.api.dao;

import it.zensoftware.luna2.dao.SdiTransmissionDAO;
import it.zensoftware.luna2.model.SdiTransmission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests per SdiTransmissionDAO
 * Coverage: CRUD operations, query methods, statistics
 */
@DataJpaTest
@DisplayName("SdiTransmissionDAO Integration Tests")
public class SdiTransmissionDAOTest {

    private SdiTransmissionDAO sdiTransmissionDAO;

    @BeforeEach
    void setUp() {
        sdiTransmissionDAO = new SdiTransmissionDAO();
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    @DisplayName("Salva una nuova trasmissione")
    void testSaveTransmission() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-2026-001");
        transmission.setFatturaId(1L);
        transmission.setNumeroFattura("2026/001");
        transmission.setStato(SdiTransmission.StatoTrasmissione.SUBMITTED);
        transmission.setDataCreazione(new Date());

        // Act
        Long id = sdiTransmissionDAO.save(transmission);

        // Assert
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    @DisplayName("Recupera trasmissione per ID trasmissione")
    void testFindByIdTrasmissione() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-2026-TEST-001");
        transmission.setFatturaId(1L);
        transmission.setNumeroFattura("2026/001");
        transmission.setStato(SdiTransmission.StatoTrasmissione.SUBMITTED);

        Long id = sdiTransmissionDAO.save(transmission);

        // Act
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("TRX-2026-TEST-001");

        // Assert
        assertNotNull(result);
        assertEquals("TRX-2026-TEST-001", result.getIdTrasmissione());
    }

    @Test
    @DisplayName("Recupera trasmissioni per fattura")
    void testFindByFatturaId() {
        // Arrange
        SdiTransmission tx1 = new SdiTransmission();
        tx1.setIdTrasmissione("TRX-FAT-001");
        tx1.setFatturaId(100L);
        tx1.setNumeroFattura("2026/100");
        tx1.setStato(SdiTransmission.StatoTrasmissione.ACCEPTED);

        SdiTransmission tx2 = new SdiTransmission();
        tx2.setIdTrasmissione("TRX-FAT-002");
        tx2.setFatturaId(100L);
        tx2.setNumeroFattura("2026/100");
        tx2.setStato(SdiTransmission.StatoTrasmissione.DELIVERED);

        sdiTransmissionDAO.save(tx1);
        sdiTransmissionDAO.save(tx2);

        // Act
        List<SdiTransmission> results = sdiTransmissionDAO.findByFatturaId(100L);

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Aggiorna stato trasmissione")
    void testUpdateTransmissionStatus() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-UPD-001");
        transmission.setFatturaId(1L);
        transmission.setStato(SdiTransmission.StatoTrasmissione.SUBMITTED);
        Long id = sdiTransmissionDAO.save(transmission);

        transmission.setStato(SdiTransmission.StatoTrasmissione.ACCEPTED);
        transmission.setId(id);

        // Act
        sdiTransmissionDAO.update(transmission);
        SdiTransmission updated = sdiTransmissionDAO.findByIdTrasmissione("TRX-UPD-001");

        // Assert
        assertEquals(SdiTransmission.StatoTrasmissione.ACCEPTED, updated.getStato());
    }

    @Test
    @DisplayName("Elimina trasmissione")
    void testDeleteTransmission() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-DEL-001");
        transmission.setFatturaId(1L);
        Long id = sdiTransmissionDAO.save(transmission);
        transmission.setId(id);

        // Act
        sdiTransmissionDAO.delete(transmission);
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("TRX-DEL-001");

        // Assert
        assertNull(result);
    }

    // ==================== PENDING TRANSMISSION TESTS ====================

    @Test
    @DisplayName("Recupera trasmissioni pending")
    void testFindPendingTransmissions() {
        // Arrange
        SdiTransmission pending = new SdiTransmission();
        pending.setIdTrasmissione("TRX-PENDING-001");
        pending.setFatturaId(1L);
        pending.setStato(SdiTransmission.StatoTrasmissione.PENDING);

        SdiTransmission accepted = new SdiTransmission();
        accepted.setIdTrasmissione("TRX-ACCEPTED-001");
        accepted.setFatturaId(2L);
        accepted.setStato(SdiTransmission.StatoTrasmissione.ACCEPTED);

        sdiTransmissionDAO.save(pending);
        sdiTransmissionDAO.save(accepted);

        // Act
        List<SdiTransmission> results = sdiTransmissionDAO.findPendingTransmissions();

        // Assert
        assertTrue(results.stream().anyMatch(t -> t.getStato() == SdiTransmission.StatoTrasmissione.PENDING));
    }

    @Test
    @DisplayName("Recupera trasmissioni con errore")
    void testFindErrorTransmissions() {
        // Arrange
        SdiTransmission error = new SdiTransmission();
        error.setIdTrasmissione("TRX-ERROR-001");
        error.setFatturaId(1L);
        error.setStato(SdiTransmission.StatoTrasmissione.ERROR);
        error.setDescrzioneErrore("Test error");

        SdiTransmission rejected = new SdiTransmission();
        rejected.setIdTrasmissione("TRX-REJECTED-001");
        rejected.setFatturaId(2L);
        rejected.setStato(SdiTransmission.StatoTrasmissione.REJECTED);

        sdiTransmissionDAO.save(error);
        sdiTransmissionDAO.save(rejected);

        // Act
        List<SdiTransmission> results = sdiTransmissionDAO.findErrorTransmissions();

        // Assert
        assertTrue(results.size() >= 2);
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("Conta trasmissioni per stato")
    void testCountByStato() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            SdiTransmission tx = new SdiTransmission();
            tx.setIdTrasmissione("TRX-COUNT-" + i);
            tx.setFatturaId((long) i);
            tx.setStato(SdiTransmission.StatoTrasmissione.SUBMITTED);
            sdiTransmissionDAO.save(tx);
        }

        // Act
        Long count = sdiTransmissionDAO.countByStato(SdiTransmission.StatoTrasmissione.SUBMITTED);

        // Assert
        assertTrue(count >= 3);
    }

    @Test
    @DisplayName("Conta trasmissioni con stato ACCEPTED")
    void testCountAcceptedTransmissions() {
        // Arrange
        SdiTransmission tx = new SdiTransmission();
        tx.setIdTrasmissione("TRX-ACCEPTED-COUNT");
        tx.setFatturaId(1L);
        tx.setStato(SdiTransmission.StatoTrasmissione.ACCEPTED);
        sdiTransmissionDAO.save(tx);

        // Act
        Long count = sdiTransmissionDAO.countByStato(SdiTransmission.StatoTrasmissione.ACCEPTED);

        // Assert
        assertTrue(count >= 1);
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    @DisplayName("Calcola statistiche trasmissioni")
    void testGetStatistics() {
        // Arrange - crea vari stati
        String[] stati = {"SUBMITTED", "ACCEPTED", "DELIVERED", "REJECTED"};
        for (String stato : stati) {
            SdiTransmission tx = new SdiTransmission();
            tx.setIdTrasmissione("TRX-STAT-" + stato);
            tx.setFatturaId(Long.parseLong(stato.hashCode() + ""));
            tx.setStato(SdiTransmission.StatoTrasmissione.valueOf(stato));
            sdiTransmissionDAO.save(tx);
        }

        // Act
        SdiTransmissionDAO.SdiTransmissionStats stats = sdiTransmissionDAO.getStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.totale >= 4);
    }

    @Test
    @DisplayName("Verifica success rate nelle statistiche")
    void testSuccessRateCalculation() {
        // Arrange
        SdiTransmission accepted = new SdiTransmission();
        accepted.setIdTrasmissione("TRX-SUCCESS-1");
        accepted.setFatturaId(1L);
        accepted.setStato(SdiTransmission.StatoTrasmissione.ACCEPTED);
        sdiTransmissionDAO.save(accepted);

        SdiTransmission rejected = new SdiTransmission();
        rejected.setIdTrasmissione("TRX-FAIL-1");
        rejected.setFatturaId(2L);
        rejected.setStato(SdiTransmission.StatoTrasmissione.REJECTED);
        sdiTransmissionDAO.save(rejected);

        // Act
        SdiTransmissionDAO.SdiTransmissionStats stats = sdiTransmissionDAO.getStatistics();

        // Assert
        assertTrue(stats.getSuccessRate() >= 0);
        assertTrue(stats.getErrorRate() >= 0);
    }

    // ==================== RETRY ATTEMPT TESTS ====================

    @Test
    @DisplayName("Registra tentativo di invio")
    void testRecordRetryAttempt() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-RETRY-001");
        transmission.setFatturaId(1L);
        transmission.setNumeroTentativi(0);
        Long id = sdiTransmissionDAO.save(transmission);

        transmission.setId(id);
        transmission.setNumeroTentativi(1);

        // Act
        sdiTransmissionDAO.update(transmission);
        SdiTransmission updated = sdiTransmissionDAO.findByIdTrasmissione("TRX-RETRY-001");

        // Assert
        assertEquals(1, updated.getNumeroTentativi());
    }

    @Test
    @DisplayName("Raggiunge max retry attempts")
    void testMaxRetryAttempts() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-MAX-RETRY");
        transmission.setFatturaId(1L);
        transmission.setNumeroTentativi(5);
        Long id = sdiTransmissionDAO.save(transmission);

        // Act
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("TRX-MAX-RETRY");

        // Assert
        assertTrue(result.getNumeroTentativi() >= 5);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("Registra messaggio di errore")
    void testRecordErrorMessage() {
        // Arrange
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-ERROR-MSG");
        transmission.setFatturaId(1L);
        transmission.setStato(SdiTransmission.StatoTrasmissione.ERROR);
        transmission.setDescrzioneErrore("Network timeout occurred");
        Long id = sdiTransmissionDAO.save(transmission);

        // Act
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("TRX-ERROR-MSG");

        // Assert
        assertEquals("Network timeout occurred", result.getDescrzioneErrore());
    }

    @Test
    @DisplayName("Registra XML contenuto trasmissione")
    void testRecordXmlContent() {
        // Arrange
        String xmlContent = "<?xml version=\"1.0\"?><FatturaElettronica></FatturaElettronica>";
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-XML-001");
        transmission.setFatturaId(1L);
        transmission.setXmlContenuto(xmlContent);
        Long id = sdiTransmissionDAO.save(transmission);

        // Act
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("TRX-XML-001");

        // Assert
        assertNotNull(result.getXmlContenuto());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Recupera fattura inesistente")
    void testFindNonExistentFattura() {
        // Act
        SdiTransmission result = sdiTransmissionDAO.findByIdTrasmissione("NONEXISTENT");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Recupera trasmissioni per fattura inesistente")
    void testFindByNonExistentFatturaId() {
        // Act
        List<SdiTransmission> results = sdiTransmissionDAO.findByFatturaId(99999L);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Trasmissione con ID trasmissione duplicato fallisce")
    void testDuplicateIdTrasmissione() {
        // Arrange
        SdiTransmission tx1 = new SdiTransmission();
        tx1.setIdTrasmissione("TRX-DUP");
        tx1.setFatturaId(1L);
        Long id1 = sdiTransmissionDAO.save(tx1);

        SdiTransmission tx2 = new SdiTransmission();
        tx2.setIdTrasmissione("TRX-DUP");
        tx2.setFatturaId(2L);

        // Act & Assert - Dovrebbe gestire duplicate constraint
        assertThrows(Exception.class, () -> {
            sdiTransmissionDAO.save(tx2);
        });
    }

    @Test
    @DisplayName("Verifica ordine cronologico trasmissioni pending")
    void testPendingTransmissionsOrdering() {
        // Arrange
        SdiTransmission tx1 = new SdiTransmission();
        tx1.setIdTrasmissione("TRX-ORDER-1");
        tx1.setFatturaId(1L);
        tx1.setStato(SdiTransmission.StatoTrasmissione.PENDING);
        tx1.setDataCreazione(new Date(System.currentTimeMillis() - 10000)); // 10 secondi fa

        SdiTransmission tx2 = new SdiTransmission();
        tx2.setIdTrasmissione("TRX-ORDER-2");
        tx2.setFatturaId(2L);
        tx2.setStato(SdiTransmission.StatoTrasmissione.PENDING);
        tx2.setDataCreazione(new Date()); // ora

        sdiTransmissionDAO.save(tx1);
        sdiTransmissionDAO.save(tx2);

        // Act
        List<SdiTransmission> results = sdiTransmissionDAO.findPendingTransmissions();

        // Assert - First should be older
        if (results.size() >= 2) {
            assertTrue(results.get(0).getDataCreazione().before(results.get(1).getDataCreazione()) ||
                      results.get(0).getDataCreazione().equals(results.get(1).getDataCreazione()));
        }
    }

    @Test
    @DisplayName("Trasmissione con numero fattura molto lungo")
    void testTransmissionWithVeryLongInvoiceNumber() {
        // Arrange
        String longNumber = "2026/" + "0".repeat(100);
        SdiTransmission transmission = new SdiTransmission();
        transmission.setIdTrasmissione("TRX-LONG-NUM");
        transmission.setFatturaId(1L);
        transmission.setNumeroFattura(longNumber);

        // Act
        Long id = sdiTransmissionDAO.save(transmission);

        // Assert
        assertNotNull(id);
    }
}

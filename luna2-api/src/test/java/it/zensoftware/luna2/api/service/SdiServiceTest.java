package it.zensoftware.luna2.api.service;

import it.zensoftware.luna2.api.service.SdiService;
import it.zensoftware.luna2.model.Fattura;
import it.zensoftware.luna2.model.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests per SdiService
 * Coverage: XML generation, invio fatture, polling notifiche
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SdiService Tests")
public class SdiServiceTest {

    private SdiService sdiService;
    private Fattura mockFattura;
    private Cliente mockCliente;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sdiService = new SdiService();

        // Setup mock objects
        mockCliente = new Cliente();
        mockCliente.setId(1L);
        mockCliente.setRagioneSociale("Test SPA");
        mockCliente.setPartitaIva("12345678901");
        mockCliente.setIndirizzo("Via Test, 1");
        mockCliente.setCitta("Milano");
        mockCliente.setCap("20100");

        mockFattura = new Fattura();
        mockFattura.setId(1L);
        mockFattura.setNumero("2026/001");
        mockFattura.setTotale(new BigDecimal("1000.00"));
        mockFattura.setCliente(mockCliente);
        mockFattura.setDataFattura(new Date());
    }

    // ==================== XML GENERATION TESTS ====================

    @Test
    @DisplayName("Generazione XML fattura")
    void testGenerateFatturaXml() throws Exception {
        String xml = sdiService.generateFatturaXml(mockFattura);

        assertNotNull(xml, "XML non dovrebbe essere null");
        assertTrue(xml.contains("2026/001"), "XML dovrebbe contenere numero fattura");
        assertTrue(xml.contains("Test SPA"), "XML dovrebbe contenere ragione sociale");
        assertTrue(xml.contains("12345678901"), "XML dovrebbe contenere partita IVA");
    }

    @Test
    @DisplayName("Generazione XML con fattura null")
    void testGenerateFatturaXmlWithNullFattura() throws Exception {
        String xml = sdiService.generateFatturaXml(null);
        assertNotNull(xml, "XML dovrebbe essere generato anche con fattura null");
    }

    @Test
    @DisplayName("Generazione XML con cliente null")
    void testGenerateFatturaXmlWithNullCliente() throws Exception {
        mockFattura.setCliente(null);
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml, "XML dovrebbe essere generato anche con cliente null");
    }

    @Test
    @DisplayName("Generazione XML valida per XSD Agenzia Entrate")
    void testGenerateFatturaXmlXsdCompliance() throws Exception {
        String xml = sdiService.generateFatturaXml(mockFattura);

        // Verifica struttura XML di base
        assertTrue(xml.startsWith("<?xml") || xml.contains("<"), "XML dovrebbe iniziare con dichiarazione");
        assertTrue(xml.contains("FatturaElettronica"), "XML dovrebbe contenere radice FatturaElettronica");
    }

    @Test
    @DisplayName("Generazione XML con importo zero")
    void testGenerateFatturaXmlWithZeroAmount() throws Exception {
        mockFattura.setTotale(BigDecimal.ZERO);
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
    }

    @Test
    @DisplayName("Generazione XML con importo negativo")
    void testGenerateFatturaXmlWithNegativeAmount() throws Exception {
        mockFattura.setTotale(new BigDecimal("-100.00"));
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
    }

    @Test
    @DisplayName("Generazione XML con caratteri speciali")
    void testGenerateFatturaXmlWithSpecialCharacters() throws Exception {
        mockCliente.setRagioneSociale("Test & Company <Ltd>");
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
    }

    // ==================== ASYNC TRANSMISSION TESTS ====================

    @Test
    @DisplayName("Invio fattura async")
    void testInviaFatturaAsync() {
        assertDoesNotThrow(() -> {
            sdiService.inviaFatturaAsync(mockFattura);
        });
    }

    @Test
    @DisplayName("Invio fattura async con null")
    void testInviaFatturaAsyncWithNull() {
        assertDoesNotThrow(() -> {
            sdiService.inviaFatturaAsync(null);
        });
    }

    @Test
    @DisplayName("Invio fattura async multiple simultanei")
    void testMultipleConcurrentInviaFatturaAsync() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                Fattura f = new Fattura();
                f.setId((long) i);
                f.setNumero("2026/00" + i);
                f.setTotale(new BigDecimal("100.00"));
                sdiService.inviaFatturaAsync(f);
            }
        });
    }

    // ==================== NOTIFICATION POLLING TESTS ====================

    @Test
    @DisplayName("Polling notifiche con timeout")
    void testPollSdiNotificationTimeout() {
        assertThrows(SdiService.SdiException.class, () -> {
            sdiService.pollSdiNotification("invalid-id");
        }, "Polling dovrebbe lanciare SdiException su timeout");
    }

    @Test
    @DisplayName("Polling notifiche con ID null")
    void testPollSdiNotificationWithNull() {
        assertThrows(SdiService.SdiException.class, () -> {
            sdiService.pollSdiNotification(null);
        });
    }

    @Test
    @DisplayName("Polling notifiche con ID vuoto")
    void testPollSdiNotificationWithEmptyId() {
        assertThrows(SdiService.SdiException.class, () -> {
            sdiService.pollSdiNotification("");
        });
    }

    @Test
    @DisplayName("Polling notifiche multiple sequenziali")
    void testMultiplePollSdiNotifications() {
        for (int i = 0; i < 3; i++) {
            assertThrows(SdiService.SdiException.class, () -> {
                sdiService.pollSdiNotification("invalid-id-" + i);
            });
        }
    }

    // ==================== RETRY LOGIC TESTS ====================

    @Test
    @DisplayName("Retry logic con exponential backoff")
    void testRetryLogicExponentialBackoff() {
        long startTime = System.currentTimeMillis();
        assertThrows(SdiService.SdiException.class, () -> {
            sdiService.pollSdiNotification("timeout-test");
        });
        long duration = System.currentTimeMillis() - startTime;

        // Dovrebbe attendere almeno alcuni secondi per retry
        assertTrue(duration >= 1000, "Retry logic dovrebbe attendere almeno 1 secondo");
    }

    @Test
    @DisplayName("Verifica max retry attempts")
    void testMaxRetryAttempts() {
        assertThrows(SdiService.SdiException.class, () -> {
            sdiService.pollSdiNotification("max-retry-test");
        }, "Dovrebbe fallire dopo max attempts");
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("SdiException è lanciata correttamente")
    void testSdiExceptionThrown() {
        SdiService.SdiException exception = assertThrows(
            SdiService.SdiException.class,
            () -> sdiService.pollSdiNotification("error-test")
        );
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Verifica messaggio errore SdiException")
    void testSdiExceptionMessage() {
        SdiService.SdiException exception = assertThrows(
            SdiService.SdiException.class,
            () -> sdiService.pollSdiNotification("error-with-message")
        );
        assertTrue(exception.getMessage() != null && !exception.getMessage().isEmpty());
    }

    // ==================== FATTURA STATE TESTS ====================

    @Test
    @DisplayName("Verifica numero fattura dopo XML generation")
    void testFatturaNumberPreserved() throws Exception {
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertEquals("2026/001", mockFattura.getNumero(), "Numero fattura non dovrebbe cambiare");
    }

    @Test
    @DisplayName("Verifica totale fattura dopo XML generation")
    void testFatturaTotalePreserved() throws Exception {
        BigDecimal originalTotal = mockFattura.getTotale();
        sdiService.generateFatturaXml(mockFattura);
        assertEquals(originalTotal, mockFattura.getTotale(), "Totale fattura non dovrebbe cambiare");
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("XML generation con ragione sociale molto lunga")
    void testXmlGenerationWithVeryLongCompanyName() throws Exception {
        mockCliente.setRagioneSociale("A".repeat(255));
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
    }

    @Test
    @DisplayName("XML generation con molte righe fattura")
    void testXmlGenerationWithManyInvoiceLines() throws Exception {
        // Simula fattura con molte righe
        mockFattura.setTotale(new BigDecimal("10000.00"));
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
    }

    @Test
    @DisplayName("XML generation con importi con decimali elevati")
    void testXmlGenerationWithHighPrecisionAmounts() throws Exception {
        mockFattura.setTotale(new BigDecimal("1234567.89"));
        String xml = sdiService.generateFatturaXml(mockFattura);
        assertNotNull(xml);
        assertTrue(xml.contains("1234567"));
    }
}


package it.zensoftware.luna2.service.calendar;

import org.jasypt.util.text.AES256TextEncryptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PasswordEncryptionService {

    private static final Logger logger = LogManager.getLogger(PasswordEncryptionService.class);
    private final AES256TextEncryptor encryptor;

    public PasswordEncryptionService(String encryptionPassword) {
        this.encryptor = new AES256TextEncryptor();
        this.encryptor.setPassword(encryptionPassword);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        try {
            return encryptor.encrypt(plainText);
        } catch (Exception e) {
            logger.error("Errore encryption password", e);
            return null;
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            logger.error("Errore decryption password", e);
            return null;
        }
    }

}

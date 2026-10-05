package vn.gov.tax.dataplatform.source.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CredentialEncryptionServiceTest {
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final CredentialEncryptionService encryptionService = new CredentialEncryptionService(TEST_KEY);

    @Test
    void encryptsAndDecryptsCredentials() {
        String password = "local-test-password";

        String encrypted = encryptionService.encrypt(password);

        assertNotEquals(password, encrypted);
        assertEquals(password, encryptionService.decrypt(encrypted));
    }

    @Test
    void rejectsTamperedCiphertext() {
        String encrypted = encryptionService.encrypt("local-test-password");
        String[] parts = encrypted.split("\\.", 3);
        byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
        ciphertext[0] ^= 1;
        String tampered = parts[0] + "." + parts[1] + "." + Base64.getEncoder().encodeToString(ciphertext);

        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(tampered));
    }

    @Test
    void requiresA256BitBase64Key() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService("short-key"));
    }
}

package vn.gov.tax.dataplatform.source.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CredentialEncryptionService {
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String encodedKey;

    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(
            @Value("${dataplatform.credentials.encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
        key();
    }

    public String encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v1." + Base64.getEncoder().encodeToString(nonce) + "."
                    + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt source credentials", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        String[] parts = encryptedValue.split("\\.", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            throw new IllegalStateException("Unsupported encrypted credential format");
        }
        try {
            byte[] nonce = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt source credentials", exception);
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != 32) {
                throw new IllegalStateException("DATAPLATFORM_ENCRYPTION_KEY must decode to 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("DATAPLATFORM_ENCRYPTION_KEY must be valid Base64", exception);
        }
    }
}

package solannee.sheridancollege.ca.jobtrackr.security;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import solannee.sheridancollege.ca.jobtrackr.config.GmailOAuthProperties;
import solannee.sheridancollege.ca.jobtrackr.exception.IntegrationUnavailableException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TokenEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final byte FORMAT_VERSION = 1;

    private final GmailOAuthProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public TokenEncryptionService(GmailOAuthProperties properties) {
        this(properties, new SecureRandom());
    }

    TokenEncryptionService(GmailOAuthProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public String encrypt(String plaintext, long userId) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(additionalData(userId));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(1 + iv.length + ciphertext.length);
            payload.put(FORMAT_VERSION);
            payload.put(iv);
            payload.put(ciphertext);
            return Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IntegrationUnavailableException(
                    "Unable to encrypt Gmail credentials; check the token encryption key",
                    exception
            );
        }
    }

    public String decrypt(String encodedPayload, long userId) {
        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            if (payload.length <= 1 + IV_LENGTH || payload[0] != FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported encrypted token format");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[payload.length - 1 - IV_LENGTH];
            System.arraycopy(payload, 1, iv, 0, IV_LENGTH);
            System.arraycopy(payload, 1 + IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(additionalData(userId));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IntegrationUnavailableException(
                    "Unable to decrypt Gmail credentials; check the token encryption key",
                    exception
            );
        }
    }

    private SecretKey encryptionKey() {
        byte[] decoded = Base64.getDecoder().decode(properties.tokenEncryptionKey());
        if (decoded.length != 32) {
            throw new IllegalArgumentException("Gmail token encryption key must contain 32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }

    private byte[] additionalData(long userId) {
        return ("jobtrackr:gmail:" + userId).getBytes(StandardCharsets.UTF_8);
    }
}

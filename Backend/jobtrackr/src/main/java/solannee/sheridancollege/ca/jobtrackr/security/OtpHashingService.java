package solannee.sheridancollege.ca.jobtrackr.security;

import org.springframework.stereotype.Component;
import solannee.sheridancollege.ca.jobtrackr.config.AuthOtpProperties;
import solannee.sheridancollege.ca.jobtrackr.model.AuthCodePurpose;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class OtpHashingService {

    private static final String ALGORITHM = "HmacSHA256";
    private final byte[] pepper;

    public OtpHashingService(AuthOtpProperties properties) {
        this.pepper = properties.pepper().getBytes(StandardCharsets.UTF_8);
    }

    public String hash(long userId, AuthCodePurpose purpose, String code) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(pepper, ALGORITHM));
            byte[] value = (userId + ":" + purpose.name() + ":" + code)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(value));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to secure one-time code", exception);
        }
    }

    public boolean matches(long userId, AuthCodePurpose purpose, String code, String expectedHash) {
        byte[] actual = hash(userId, purpose, code).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}

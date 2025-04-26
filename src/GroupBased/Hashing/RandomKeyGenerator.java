package GroupBased.Hashing;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class RandomKeyGenerator {

    public static String generateSecretKeyHMAC() {
        try {
            // Use "HmacMD5" as the algorithm
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacMD5");
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HMAC-MD5 algorithm not found", e);
        }
    }

}

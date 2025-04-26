package GroupBased.Hashing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MD5Hash {

    public static String hashHmacMd5(String key, String message) {
        return hashHmacMd5Internal(key, message.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashHmacMd5(String key, int message) {
        return hashHmacMd5Internal(key, String.valueOf(message).getBytes(StandardCharsets.UTF_8));
    }

    private static String hashHmacMd5Internal(String key, byte[] messageBytes) {
        try {
            // Create a SecretKeySpec using the key and the HMAC-MD5 algorithm
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacMD5");

            // Initialize the Mac instance with the secret key
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(secretKey);

            // Compute the HMAC-MD5 hash
            byte[] hash = mac.doFinal(messageBytes);

            // Encode the hash as a Base64 string
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating HMAC-MD5", e);
        }
    }

    public static String hashMD5(String input) {
        try {
            // Create a MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Convert the input string to a byte array and compute the hash
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert the byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            // MD5 algorithm not available
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

}


package workout_management.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final int ITERATIONS = 600000;
    private static final int KEY_LENGTH = 256;

    // 회원가입 시 비밀번호 해시 생성
    public static String hash(char[] password) {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] hash = generateHash(password, salt, ITERATIONS);

        return "pbkdf2_sha256$"
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    // 로그인 시 비밀번호 검증
    public static boolean verify(char[] password, String storedHash) {

        if (storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split("\\$");

            if (parts.length != 4 ||
                    !parts[0].equals("pbkdf2_sha256")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);

            if (iterations < 1 || iterations > 2000000) {
                return false;
            }

            byte[] salt =
                    Base64.getDecoder().decode(parts[2]);

            byte[] expectedHash =
                    Base64.getDecoder().decode(parts[3]);

            byte[] actualHash =
                    generateHash(password, salt, iterations);

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] generateHash(
            char[] password, byte[] salt, int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                password, salt, iterations, KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "비밀번호 해시 생성 실패", e);

        } finally {
            spec.clearPassword();
        }
    }
}


package workout_management.util;

import workout_management.domain.Member;
import workout_management.repository.JDBCConnector;

import java.sql.*;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Base64;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

public class SessionUtil {

    private static final Preferences PREFS =
            Preferences.userRoot().node("workout_management/liftlog");

    private static final String TOKEN_KEY = "login_token";

    // 자동 로그인 저장
    public static void remember(Member member)
            throws SQLException, BackingStoreException {

        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding().encodeToString(bytes);

        String sql = """
                INSERT INTO login_session
                (token_hash, member_id, expires_at)
                VALUES (?, ?, SYSTIMESTAMP + INTERVAL '30' DAY)
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, hash(token));
            pstmt.setLong(2, member.getMemberId());

            pstmt.executeUpdate();
        }

        PREFS.put(TOKEN_KEY, token);
        PREFS.flush();
    }

    // 저장된 로그인 정보 확인
    public static Member restore()
            throws SQLException, BackingStoreException {

        String token = PREFS.get(TOKEN_KEY, "");

        if (token.isBlank()) {
            return null;
        }

        String sql = """
                SELECT m.member_id, m.login_id
                FROM login_session s
                JOIN member_account m
                  ON s.member_id = m.member_id
                WHERE s.token_hash = ?
                  AND s.expires_at > SYSTIMESTAMP
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, hash(token));

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    return new Member(
                            rs.getLong("member_id"),
                            rs.getString("login_id"),
                            null
                    );
                }
            }
        }

        forget();

        return null;
    }

    // 로그아웃 및 자동 로그인 해제
    public static void forget()
            throws SQLException, BackingStoreException {

        String token = PREFS.get(TOKEN_KEY, "");

        try {

            PREFS.remove(TOKEN_KEY);
            PREFS.flush();

        } finally {

            if (!token.isBlank()) {

                String sql = """
                        DELETE FROM login_session
                        WHERE token_hash = ?
                        """;

                try (Connection con = JDBCConnector.getConnection();
                     PreparedStatement pstmt =
                             con.prepareStatement(sql)) {

                    pstmt.setString(1, hash(token));
                    pstmt.executeUpdate();
                }
            }
        }
    }

    // 토큰 해시 생성
    private static String hash(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8))
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "토큰 생성 오류", e);
        }
    }
}

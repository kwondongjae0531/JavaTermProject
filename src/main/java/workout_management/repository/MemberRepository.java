
package workout_management.repository;

import workout_management.domain.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MemberRepository {

    // 회원가입
    public int insert(Member member) throws SQLException {

        String sql = """
                INSERT INTO member_account
                (login_id, password_hash)
                VALUES (?, ?)
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, member.getLoginId());
            pstmt.setString(2, member.getPasswordHash());

            return pstmt.executeUpdate();
        }
    }

    // 로그인 아이디로 회원 조회
    public Member select(String loginId) throws SQLException {

        String sql = """
                SELECT member_id, login_id, password_hash
                FROM member_account
                WHERE login_id = ?
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {

                    Member member = new Member();

                    member.setMemberId(
                            rs.getLong("member_id"));

                    member.setLoginId(
                            rs.getString("login_id"));

                    member.setPasswordHash(
                            rs.getString("password_hash"));

                    return member;
                }
            }
        }

        return null;
    }

    // 아이디 중복 확인
    public boolean exists(String loginId) throws SQLException {
        return select(loginId) != null;
    }
}

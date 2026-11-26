
package workout_management.controller;

import workout_management.domain.Member;
import workout_management.repository.MemberRepository;
import workout_management.util.PasswordUtil;

import java.sql.SQLException;

public class AuthController {

    private final MemberRepository memberRepository =
            new MemberRepository();

    // 회원가입
    public void register(String loginId, char[] password)
            throws SQLException {

        loginId = loginId.trim();

        if (loginId.isEmpty() || loginId.length() > 50) {
            throw new IllegalArgumentException(
                    "아이디는 1~50자로 입력하세요.");
        }

        if (password.length < 8 || password.length > 128) {
            throw new IllegalArgumentException(
                    "비밀번호는 8~128자로 입력하세요.");
        }

        if (memberRepository.exists(loginId)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다.");
        }

        Member member = new Member();

        member.setLoginId(loginId);
        member.setPasswordHash(PasswordUtil.hash(password));

        try {
            if (memberRepository.insert(member) != 1) {
                throw new SQLException("회원가입 실패");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) {
                throw new IllegalArgumentException(
                        "이미 사용 중인 아이디입니다.");
            }
            throw e;
        }
    }

    // 로그인
    public Member login(String loginId, char[] password)
            throws SQLException {

        Member member =
                memberRepository.select(loginId.trim());

        if (member == null ||
                !PasswordUtil.verify(
                        password, member.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        member.setPasswordHash(null);

        return member;
    }
}

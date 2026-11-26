package workout_management.domain;

public class Member {

    private long memberId;
    private String loginId;
    private String passwordHash;

    public Member() {
    }

    public Member(long memberId, String loginId, String passwordHash) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
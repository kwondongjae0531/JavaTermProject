package workout_management.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnector {

    private static final String URL =
            "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";

    private static final String USER = "termproject";

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException("DB_PASSWORD 환경변수를 설정하세요.");
        }

        return DriverManager.getConnection(URL, USER, password);
    }

    public static void main(String[] args) {
        try (Connection con = getConnection()) {
            System.out.println("Oracle 데이터베이스 연결 성공!");
        } catch (SQLException e) {
            System.out.println("데이터베이스 연결 실패");
            e.printStackTrace();
        }
    }
}

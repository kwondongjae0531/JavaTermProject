
package workout_management.repository;

import workout_management.domain.SetRecord;

import java.sql.*;
import java.util.ArrayList;

public class SetRecordRepository {

    // 세트기록 조회
    public ArrayList<SetRecord> select(long memberId, long recordId)
            throws SQLException {

        ArrayList<SetRecord> setList = new ArrayList<>();

        String sql = """
                SELECT s.set_record_id, s.record_id,
                       s.set_no, s.weight_kg,
                       s.reps, s.rest_seconds
                FROM set_record s
                JOIN workout_record r
                  ON s.record_id = r.record_id
                WHERE r.member_id = ?
                  AND s.record_id = ?
                ORDER BY s.set_no
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, memberId);
            pstmt.setLong(2, recordId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    SetRecord set = new SetRecord();

                    set.setSetRecordId(
                            rs.getLong("set_record_id"));

                    set.setRecordId(rs.getLong("record_id"));
                    set.setSetNo(rs.getInt("set_no"));
                    set.setWeightKg(rs.getBigDecimal("weight_kg"));
                    set.setReps(rs.getInt("reps"));

                    int rest = rs.getInt("rest_seconds");

                    set.setRestSeconds(
                            rs.wasNull() ? null : rest);

                    setList.add(set);
                }
            }
        }

        return setList;
    }

    // 세트기록 등록
    public int insert(long memberId, SetRecord set)
            throws SQLException {

        try (Connection con = JDBCConnector.getConnection()) {
            return insert(con, memberId, set.getRecordId(), set);
        }
    }

    // 운동기록과 함께 저장할 때 사용하는 등록 메서드
    int insert(Connection con, long memberId,
               long recordId, SetRecord set) throws SQLException {

        String sql = """
                INSERT INTO set_record
                (record_id, set_no, weight_kg, reps, rest_seconds)
                SELECT record_id, ?, ?, ?, ?
                FROM workout_record
                WHERE record_id = ?
                  AND member_id = ?
                """;

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, set.getSetNo());
            pstmt.setBigDecimal(2, set.getWeightKg());
            pstmt.setInt(3, set.getReps());

            if (set.getRestSeconds() == null) {
                pstmt.setNull(4, Types.NUMERIC);
            } else {
                pstmt.setInt(4, set.getRestSeconds());
            }

            pstmt.setLong(5, recordId);
            pstmt.setLong(6, memberId);

            return pstmt.executeUpdate();
        }
    }

    // 세트기록 수정
    public int update(long memberId, SetRecord set)
            throws SQLException {

        String sql = """
                UPDATE set_record s
                SET set_no = ?,
                    weight_kg = ?,
                    reps = ?,
                    rest_seconds = ?
                WHERE set_record_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM workout_record r
                      WHERE r.record_id = s.record_id
                        AND r.member_id = ?
                  )
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setInt(1, set.getSetNo());
            pstmt.setBigDecimal(2, set.getWeightKg());
            pstmt.setInt(3, set.getReps());

            if (set.getRestSeconds() == null) {
                pstmt.setNull(4, Types.NUMERIC);
            } else {
                pstmt.setInt(4, set.getRestSeconds());
            }

            pstmt.setLong(5, set.getSetRecordId());
            pstmt.setLong(6, memberId);

            return pstmt.executeUpdate();
        }
    }

    // 세트기록 삭제
    public int delete(long setRecordId, long memberId)
            throws SQLException {

        String sql = """
                DELETE FROM set_record s
                WHERE set_record_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM workout_record r
                      WHERE r.record_id = s.record_id
                        AND r.member_id = ?
                  )
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, setRecordId);
            pstmt.setLong(2, memberId);

            return pstmt.executeUpdate();
        }
    }
}

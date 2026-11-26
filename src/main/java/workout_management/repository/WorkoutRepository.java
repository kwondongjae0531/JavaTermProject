package workout_management.repository;

import workout_management.domain.WorkoutRecord;
import workout_management.domain.SetRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkoutRepository {

    // 운동 기록 조회

    public ArrayList<WorkoutRecord> select(long memberId)
            throws SQLException {

        ArrayList<WorkoutRecord> workoutList =
                new ArrayList<>();

        String sql = """
                SELECT record_id, member_id,
                       exercise_id, workout_date
                FROM workout_record
                WHERE member_id = ?
                ORDER BY workout_date DESC, record_id DESC
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    workoutList.add(mapRecord(rs));
                }
            }
        }

        return workoutList;
    }

    // 동일한 날짜와 운동의 기존 기록 조회

    public WorkoutRecord findDailyRecord(
            long memberId,
            long exerciseId,
            LocalDate date)
            throws SQLException {

        String sql = """
                SELECT record_id, member_id,
                       exercise_id, workout_date
                FROM workout_record
                WHERE member_id = ?
                  AND exercise_id = ?
                  AND workout_date = ?
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, memberId);
            pstmt.setLong(2, exerciseId);
            pstmt.setDate(3, Date.valueOf(date));

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }

        return null;
    }

    // 기존 화면 호환용 운동 기록 등록

    public long insert(WorkoutRecord record)
            throws SQLException {

        return insert(record, List.of());
    }

    // 운동 기록과 세트 기록을 한 번에 저장

    public long insert(
            WorkoutRecord record,
            List<SetRecord> sets)
            throws SQLException {

        String checkSql = """
                SELECT exercise_id
                FROM exercise
                WHERE exercise_id = ?
                  AND (registered_member_id IS NULL
                       OR registered_member_id = ?)
                """;

        String insertSql = """
                INSERT INTO workout_record
                (member_id, exercise_id, workout_date)
                VALUES (?, ?, ?)
                """;

        try (Connection con = JDBCConnector.getConnection()) {

            con.setAutoCommit(false);

            try {

                // 운동 종목 사용 권한 확인

                try (PreparedStatement pstmt =
                             con.prepareStatement(checkSql)) {

                    pstmt.setLong(
                            1, record.getExerciseId());

                    pstmt.setLong(
                            2, record.getMemberId());

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "사용할 수 없는 운동 종목입니다.");
                        }
                    }
                }

                // 새 운동 기록 생성

                long recordId;

                try (PreparedStatement pstmt =
                             con.prepareStatement(
                                     insertSql,
                                     new String[]{"RECORD_ID"})) {

                    pstmt.setLong(
                            1, record.getMemberId());

                    pstmt.setLong(
                            2, record.getExerciseId());

                    pstmt.setDate(
                            3,
                            Date.valueOf(
                                    record.getWorkoutDate()));

                    try {

                        if (pstmt.executeUpdate() != 1) {

                            throw new SQLException(
                                    "운동 기록 저장 실패");
                        }

                    } catch (SQLException e) {

                        if (e.getErrorCode() == 1) {

                            throw new SQLException(
                                    "해당 날짜에 동일한 운동 기록이 있습니다. "
                                            + "운동 화면을 다시 열어 주세요.",
                                    e.getSQLState(),
                                    e.getErrorCode(),
                                    e);
                        }

                        throw e;
                    }

                    // Oracle에서 생성한 기록번호 조회

                    try (ResultSet rs =
                                 pstmt.getGeneratedKeys()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "기록번호 생성 실패");
                        }

                        recordId = rs.getLong(1);
                    }
                }

                // 동일 트랜잭션에서 세트 저장

                SetRecordRepository setRepository =
                        new SetRecordRepository();

                for (SetRecord set : sets) {

                    int result = setRepository.insert(
                            con,
                            record.getMemberId(),
                            recordId,
                            set);

                    if (result != 1) {

                        throw new SQLException(
                                "세트 기록 저장 실패");
                    }
                }

                // 운동 기록과 세트 모두 성공했을 때만 확정

                con.commit();

                return recordId;

            } catch (SQLException | RuntimeException e) {

                rollback(con, e);

                throw e;
            }
        }
    }

    // 운동 기록 수정

    public int update(WorkoutRecord record)
            throws SQLException {

        String sql = """
                UPDATE workout_record
                SET exercise_id = ?, workout_date = ?
                WHERE record_id = ?
                  AND member_id = ?
                  AND EXISTS (
                      SELECT 1 FROM exercise
                      WHERE exercise_id = ?
                        AND (registered_member_id IS NULL
                             OR registered_member_id = ?)
                  )
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(
                    1, record.getExerciseId());

            pstmt.setDate(
                    2,
                    Date.valueOf(record.getWorkoutDate()));

            pstmt.setLong(
                    3, record.getRecordId());

            pstmt.setLong(
                    4, record.getMemberId());

            pstmt.setLong(
                    5, record.getExerciseId());

            pstmt.setLong(
                    6, record.getMemberId());

            try {

                return pstmt.executeUpdate();

            } catch (SQLException e) {

                if (e.getErrorCode() == 1) {

                    throw new SQLException(
                            "해당 날짜에 동일한 운동 기록이 있습니다.",
                            e.getSQLState(),
                            e.getErrorCode(),
                            e);
                }

                throw e;
            }
        }
    }

    // 운동 기록 전체 삭제

    public int delete(
            long recordId, long memberId)
            throws SQLException {

        String sql = """
                DELETE FROM workout_record
                WHERE record_id = ?
                  AND member_id = ?
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt =
                     con.prepareStatement(sql)) {

            pstmt.setLong(1, recordId);
            pstmt.setLong(2, memberId);

            return pstmt.executeUpdate();
        }
    }

    // 세트 삭제 및 빈 운동 기록 정리

    public boolean deleteSetAndRemoveEmptyRecord(
            long memberId,
            long setRecordId)
            throws SQLException {

        String findSql = """
                SELECT w.record_id
                FROM workout_record w
                WHERE w.member_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM set_record s
                      WHERE s.record_id = w.record_id
                        AND s.set_record_id = ?
                  )
                FOR UPDATE
                """;

        String deleteSetSql = """
                DELETE FROM set_record
                WHERE set_record_id = ?
                  AND record_id = ?
                """;

        String deleteEmptySql = """
                DELETE FROM workout_record w
                WHERE w.record_id = ?
                  AND w.member_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM set_record s
                      WHERE s.record_id = w.record_id
                  )
                """;

        try (Connection con = JDBCConnector.getConnection()) {

            con.setAutoCommit(false);

            try {

                long recordId;

                // 회원 소유권 확인 및 운동 기록 잠금

                try (PreparedStatement pstmt =
                             con.prepareStatement(findSql)) {

                    pstmt.setLong(1, memberId);
                    pstmt.setLong(2, setRecordId);

                    try (ResultSet rs = pstmt.executeQuery()) {

                        if (!rs.next()) {

                            throw new SQLException(
                                    "삭제할 세트 기록을 찾을 수 없습니다.");
                        }

                        recordId = rs.getLong("record_id");
                    }
                }

                // 선택한 세트 삭제

                try (PreparedStatement pstmt =
                             con.prepareStatement(deleteSetSql)) {

                    pstmt.setLong(1, setRecordId);
                    pstmt.setLong(2, recordId);

                    if (pstmt.executeUpdate() != 1) {

                        throw new SQLException(
                                "세트 기록 삭제 실패");
                    }
                }

                // 세트가 하나도 남지 않았다면
                // 운동 기록도 함께 삭제

                boolean recordRemoved;

                try (PreparedStatement pstmt =
                             con.prepareStatement(
                                     deleteEmptySql)) {

                    pstmt.setLong(1, recordId);
                    pstmt.setLong(2, memberId);

                    recordRemoved =
                            pstmt.executeUpdate() == 1;
                }

                con.commit();

                return recordRemoved;

            } catch (SQLException | RuntimeException e) {

                rollback(con, e);

                throw e;
            }
        }
    }

    // ResultSet을 운동 기록 객체로 변환

    private WorkoutRecord mapRecord(ResultSet rs)
            throws SQLException {

        WorkoutRecord record = new WorkoutRecord();

        record.setRecordId(
                rs.getLong("record_id"));

        record.setMemberId(
                rs.getLong("member_id"));

        record.setExerciseId(
                rs.getLong("exercise_id"));

        record.setWorkoutDate(
                rs.getDate("workout_date")
                        .toLocalDate());

        return record;
    }

    // 저장 또는 삭제 실패 시 트랜잭션 취소

    private void rollback(
            Connection con, Throwable original) {

        try {

            con.rollback();

        } catch (SQLException rollbackError) {

            original.addSuppressed(rollbackError);
        }
    }
}
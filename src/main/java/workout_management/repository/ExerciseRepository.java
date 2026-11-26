package workout_management.repository;

import workout_management.domain.Exercise;

import java.sql.*;
import java.util.ArrayList;

public class ExerciseRepository {

    // 기본 운동 및 개인 운동 조회

    public ArrayList<Exercise> select(long memberId)
            throws SQLException {

        ArrayList<Exercise> exercises = new ArrayList<>();

        String sql = """
                SELECT exercise_id,
                       registered_member_id,
                       exercise_name,
                       body_part
                FROM exercise
                WHERE registered_member_id IS NULL
                   OR registered_member_id = ?
                ORDER BY body_part, exercise_name, exercise_id
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, memberId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    Exercise exercise = new Exercise();

                    exercise.setExerciseId(
                            rs.getLong("exercise_id"));

                    long registeredId =
                            rs.getLong("registered_member_id");

                    exercise.setRegisteredMemberId(
                            rs.wasNull() ? null : registeredId);

                    exercise.setExerciseName(
                            rs.getString("exercise_name"));

                    exercise.setBodyPart(
                            rs.getString("body_part"));

                    exercises.add(exercise);
                }
            }
        }

        return exercises;
    }

    // 개인 운동 추가

    public int insert(Exercise exercise)
            throws SQLException {

        if (exercise.getRegisteredMemberId() == null) {

            throw new IllegalArgumentException(
                    "회원 정보가 필요합니다.");
        }

        String sql = """
                INSERT INTO exercise
                (registered_member_id, exercise_name, body_part)
                VALUES (?, ?, ?)
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(
                    1, exercise.getRegisteredMemberId());

            pstmt.setString(
                    2, exercise.getExerciseName());

            pstmt.setString(
                    3, exercise.getBodyPart());

            return pstmt.executeUpdate();
        }
    }

    // 개인 운동 수정

    public int update(
            long memberId,
            long exerciseId,
            String name,
            String bodyPart) throws SQLException {

        String sql = """
                UPDATE exercise e
                SET exercise_name = ?, body_part = ?
                WHERE e.exercise_id = ?
                  AND e.registered_member_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM workout_record w
                      WHERE w.exercise_id = e.exercise_id
                  )
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, bodyPart);
            pstmt.setLong(3, exerciseId);
            pstmt.setLong(4, memberId);

            return pstmt.executeUpdate();
        }
    }

    // 개인 운동 삭제

    public int delete(
            long memberId,
            long exerciseId) throws SQLException {

        String sql = """
                DELETE FROM exercise e
                WHERE e.exercise_id = ?
                  AND e.registered_member_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM workout_record w
                      WHERE w.exercise_id = e.exercise_id
                  )
                """;

        try (Connection con = JDBCConnector.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, exerciseId);
            pstmt.setLong(2, memberId);

            return pstmt.executeUpdate();
        }
    }
}
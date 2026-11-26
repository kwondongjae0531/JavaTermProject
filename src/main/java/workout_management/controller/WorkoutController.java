package workout_management.controller;

import workout_management.domain.*;
import workout_management.repository.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkoutController {

    private final long memberId;

    private final ExerciseRepository exerciseRepository =
            new ExerciseRepository();

    private final WorkoutRepository workoutRepository =
            new WorkoutRepository();

    private final SetRecordRepository setRepository =
            new SetRecordRepository();

    public WorkoutController(Member member) {
        this.memberId = member.getMemberId();
    }

    // 운동 종목 조회

    public ArrayList<Exercise> getExercises()
            throws SQLException {

        return exerciseRepository.select(memberId);
    }

    // 개인 운동 등록

    public void insertExercise(String name, String bodyPart)
            throws SQLException {

        name = validateExerciseName(name, bodyPart);

        checkDuplicateExercise(0, name, bodyPart);

        Exercise exercise = new Exercise();

        exercise.setRegisteredMemberId(memberId);
        exercise.setExerciseName(name);
        exercise.setBodyPart(bodyPart);

        if (exerciseRepository.insert(exercise) != 1) {

            throw new SQLException("운동 종목 추가 실패");
        }
    }

    // 개인 운동 목록

    public ArrayList<Exercise> getPersonalExercises()
            throws SQLException {

        ArrayList<Exercise> personal = new ArrayList<>();

        for (Exercise exercise : getExercises()) {

            if (exercise.getRegisteredMemberId() != null
                    && exercise.getRegisteredMemberId() == memberId) {

                personal.add(exercise);
            }
        }

        return personal;
    }

    // 개인 운동 수정

    public void updateExercise(
            long exerciseId,
            String name,
            String bodyPart) throws SQLException {

        name = validateExerciseName(name, bodyPart);

        checkDuplicateExercise(
                exerciseId, name, bodyPart);

        if (exerciseRepository.update(
                memberId, exerciseId, name, bodyPart) != 1) {

            throw new IllegalArgumentException(
                    "사용 중인 운동이거나 수정할 수 없는 종목입니다.");
        }
    }

    // 개인 운동 삭제

    public void deleteExercise(long exerciseId)
            throws SQLException {

        if (exerciseRepository.delete(
                memberId, exerciseId) != 1) {

            throw new IllegalArgumentException(
                    "운동 기록이 연결되어 있거나 삭제할 수 없는 종목입니다.");
        }
    }

    // 운동 이름 및 부위 검증

    private String validateExerciseName(
            String name, String bodyPart) {

        name = name == null ? "" : name.trim();

        if (name.isEmpty() || name.length() > 50) {

            throw new IllegalArgumentException(
                    "운동 이름은 1~50자로 입력하세요.");
        }

        if (bodyPart == null || !List.of(
                "가슴", "어깨", "등",
                "하체", "팔", "복근"
        ).contains(bodyPart)) {

            throw new IllegalArgumentException(
                    "운동 부위를 선택하세요.");
        }

        return name;
    }

    // 동일 운동 이름 중복 확인

    private void checkDuplicateExercise(
            long excludedId,
            String name,
            String bodyPart) throws SQLException {

        for (Exercise exercise : getExercises()) {

            if (exercise.getExerciseId() == excludedId) {
                continue;
            }

            if (exercise.getExerciseName().equalsIgnoreCase(name)
                    && exercise.getBodyPart().equals(bodyPart)) {

                throw new IllegalArgumentException(
                        "이미 등록된 운동 종목입니다.");
            }
        }
    }

    // 운동 기록 조회

    public ArrayList<WorkoutRecord> getRecords()
            throws SQLException {

        return workoutRepository.select(memberId);
    }

    // 동일 날짜 운동 확인

    public WorkoutRecord findDailyRecord(
            Exercise exercise, LocalDate date)
            throws SQLException {

        validate(exercise, date);

        return workoutRepository.findDailyRecord(
                memberId,
                exercise.getExerciseId(),
                date);
    }

    // 기존 화면 호환용 등록

    public long insertRecord(
            Exercise exercise, LocalDate date)
            throws SQLException {

        validate(exercise, date);

        WorkoutRecord existing =
                findDailyRecord(exercise, date);

        if (existing != null) {
            return existing.getRecordId();
        }

        WorkoutRecord record = new WorkoutRecord(
                0, memberId,
                exercise.getExerciseId(), date);

        return workoutRepository.insert(record);
    }

    // 첫 세트와 운동 기록 동시 저장

    public long insertFirstSet(
            Exercise exercise,
            LocalDate date,
            SetRecord firstSet)
            throws SQLException {

        validate(exercise, date);
        validateSet(firstSet);

        WorkoutRecord existing =
                findDailyRecord(exercise, date);

        if (existing != null) {

            throw new IllegalArgumentException(
                    "이미 등록된 운동입니다. "
                            + "운동 화면을 다시 열어 주세요.");
        }

        WorkoutRecord record = new WorkoutRecord(
                0, memberId,
                exercise.getExerciseId(), date);

        return workoutRepository.insert(
                record, List.of(firstSet));
    }

    // 운동 기록 수정

    public void updateRecord(
            long recordId,
            Exercise exercise,
            LocalDate date)
            throws SQLException {

        validate(exercise, date);

        WorkoutRecord existing =
                findDailyRecord(exercise, date);

        if (existing != null
                && existing.getRecordId() != recordId) {

            throw new IllegalArgumentException(
                    "해당 날짜에 동일한 운동 기록이 있습니다.");
        }

        WorkoutRecord record = new WorkoutRecord(
                recordId, memberId,
                exercise.getExerciseId(), date);

        if (workoutRepository.update(record) != 1) {

            throw new SQLException("운동 기록 수정 실패");
        }
    }

    // 운동 기록 삭제

    public void deleteRecord(long recordId)
            throws SQLException {

        if (workoutRepository.delete(
                recordId, memberId) != 1) {

            throw new SQLException("운동 기록 삭제 실패");
        }
    }

    // 세트 조회

    public ArrayList<SetRecord> getSets(long recordId)
            throws SQLException {

        return setRepository.select(memberId, recordId);
    }

    // 세트 등록

    public void insertSet(SetRecord set)
            throws SQLException {

        validateSet(set);

        if (setRepository.insert(memberId, set) != 1) {

            throw new SQLException("세트 등록 실패");
        }
    }

    // 세트 수정

    public void updateSet(SetRecord set)
            throws SQLException {

        validateSet(set);

        if (setRepository.update(memberId, set) != 1) {

            throw new SQLException("세트 수정 실패");
        }
    }

    // 세트 삭제

    public void deleteSet(long setRecordId)
            throws SQLException {

        if (setRepository.delete(
                setRecordId, memberId) != 1) {

            throw new SQLException("세트 삭제 실패");
        }
    }

    // 마지막 세트 삭제 시 빈 운동 기록 정리

    public boolean deleteSetAndRemoveEmptyRecord(
            long setRecordId)
            throws SQLException {

        return workoutRepository
                .deleteSetAndRemoveEmptyRecord(
                        memberId, setRecordId);
    }

    // 운동 기록 검증

    private void validate(
            Exercise exercise, LocalDate date) {

        if (exercise == null) {

            throw new IllegalArgumentException(
                    "운동 종목을 선택하세요.");
        }

        if (date == null) {

            throw new IllegalArgumentException(
                    "운동 날짜를 선택하세요.");
        }
    }

    // 세트 입력 검증

    private void validateSet(SetRecord set) {

        if (set == null) {

            throw new IllegalArgumentException(
                    "세트 기록을 입력하세요.");
        }

        if (set.getSetNo() < 1
                || set.getSetNo() > 9999) {

            throw new IllegalArgumentException(
                    "세트 번호는 1~9999 사이로 입력하세요.");
        }

        BigDecimal weight = set.getWeightKg();

        if (weight == null
                || weight.signum() < 0
                || weight.stripTrailingZeros().scale() > 2
                || weight.compareTo(
                new BigDecimal("99999.99")) > 0) {

            throw new IllegalArgumentException(
                    "중량을 확인하세요. 소수 둘째 자리까지 입력 가능합니다.");
        }

        if (set.getReps() < 1
                || set.getReps() > 99999) {

            throw new IllegalArgumentException(
                    "반복 횟수를 확인하세요.");
        }

        Integer rest = set.getRestSeconds();

        if (rest != null
                && (rest < 0 || rest > 999999)) {

            throw new IllegalArgumentException(
                    "휴식시간을 확인하세요.");
        }
    }
}
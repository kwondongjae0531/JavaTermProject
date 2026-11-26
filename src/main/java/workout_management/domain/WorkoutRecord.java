package workout_management.domain;

import java.time.LocalDate;

public class WorkoutRecord {

    private long recordId;
    private long memberId;
    private long exerciseId;
    private LocalDate workoutDate;

    public WorkoutRecord() {
    }

    public WorkoutRecord(long recordId, long memberId,
                         long exerciseId, LocalDate workoutDate) {
        this.recordId = recordId;
        this.memberId = memberId;
        this.exerciseId = exerciseId;
        this.workoutDate = workoutDate;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public LocalDate getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(LocalDate workoutDate) {
        this.workoutDate = workoutDate;
    }
}
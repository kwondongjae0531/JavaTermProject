package workout_management.domain;

public class Exercise {

    private long exerciseId;
    private Long registeredMemberId;
    private String exerciseName;
    private String bodyPart;

    public Exercise() {
    }

    public Exercise(long exerciseId, Long registeredMemberId,
                    String exerciseName, String bodyPart) {
        this.exerciseId = exerciseId;
        this.registeredMemberId = registeredMemberId;
        this.exerciseName = exerciseName;
        this.bodyPart = bodyPart;
    }

    public long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public Long getRegisteredMemberId() {
        return registeredMemberId;
    }

    public void setRegisteredMemberId(Long registeredMemberId) {
        this.registeredMemberId = registeredMemberId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getBodyPart() {
        return bodyPart;
    }

    public void setBodyPart(String bodyPart) {
        this.bodyPart = bodyPart;
    }

    @Override
    public String toString() {
        return exerciseName;
    }
}
package workout_management.domain;

import java.math.BigDecimal;

public class SetRecord {

    private long setRecordId;
    private long recordId;
    private int setNo;
    private BigDecimal weightKg;
    private int reps;
    private Integer restSeconds;

    public SetRecord() {
    }

    public SetRecord(long setRecordId, long recordId,
                     int setNo, BigDecimal weightKg,
                     int reps, Integer restSeconds) {
        this.setRecordId = setRecordId;
        this.recordId = recordId;
        this.setNo = setNo;
        this.weightKg = weightKg;
        this.reps = reps;
        this.restSeconds = restSeconds;
    }

    public long getSetRecordId() {
        return setRecordId;
    }

    public void setSetRecordId(long setRecordId) {
        this.setRecordId = setRecordId;
    }

    public long getRecordId() {
        return recordId;
    }

    public void setRecordId(long recordId) {
        this.recordId = recordId;
    }

    public int getSetNo() {
        return setNo;
    }

    public void setSetNo(int setNo) {
        this.setNo = setNo;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public Integer getRestSeconds() {
        return restSeconds;
    }

    public void setRestSeconds(Integer restSeconds) {
        this.restSeconds = restSeconds;
    }
}
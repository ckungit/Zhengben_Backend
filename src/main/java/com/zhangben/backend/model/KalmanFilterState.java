package com.zhangben.backend.model;

import java.time.LocalDateTime;

public class KalmanFilterState {

    private Integer id;
    private Integer userId;
    private String stateVector;          // JSON: [level, trend]
    private String covarianceMatrix;     // JSON: [[p00,p01],[p10,p11]]
    private Long predictedNextMonth;
    private Long predictedConfidenceLow;
    private Long predictedConfidenceHigh;
    private String lastObservationMonth; // "YYYY-MM"
    private Integer consecutiveMonths;
    private Integer totalObservations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getStateVector() { return stateVector; }
    public void setStateVector(String stateVector) { this.stateVector = stateVector; }

    public String getCovarianceMatrix() { return covarianceMatrix; }
    public void setCovarianceMatrix(String covarianceMatrix) { this.covarianceMatrix = covarianceMatrix; }

    public Long getPredictedNextMonth() { return predictedNextMonth; }
    public void setPredictedNextMonth(Long predictedNextMonth) { this.predictedNextMonth = predictedNextMonth; }

    public Long getPredictedConfidenceLow() { return predictedConfidenceLow; }
    public void setPredictedConfidenceLow(Long predictedConfidenceLow) { this.predictedConfidenceLow = predictedConfidenceLow; }

    public Long getPredictedConfidenceHigh() { return predictedConfidenceHigh; }
    public void setPredictedConfidenceHigh(Long predictedConfidenceHigh) { this.predictedConfidenceHigh = predictedConfidenceHigh; }

    public String getLastObservationMonth() { return lastObservationMonth; }
    public void setLastObservationMonth(String lastObservationMonth) { this.lastObservationMonth = lastObservationMonth; }

    public Integer getConsecutiveMonths() { return consecutiveMonths; }
    public void setConsecutiveMonths(Integer consecutiveMonths) { this.consecutiveMonths = consecutiveMonths; }

    public Integer getTotalObservations() { return totalObservations; }
    public void setTotalObservations(Integer totalObservations) { this.totalObservations = totalObservations; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

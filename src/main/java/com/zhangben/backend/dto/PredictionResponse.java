package com.zhangben.backend.dto;

import java.util.List;

public class PredictionResponse {

    private int consecutiveMonths;
    private boolean unlocked;              // consecutiveMonths >= 3
    private String emotionalMessage;
    private String emotionalMessageKey;    // i18n key
    private double unlockProgress;         // 0.0 ~ 1.0
    private Long predictedNextMonth;       // cents (null when locked)
    private Long confidenceLow;            // cents (null when locked)
    private Long confidenceHigh;           // cents (null when locked)
    private Long fixedAmount;              // cents - fixed portion of prediction
    private Long variableAmount;           // cents - variable portion of prediction
    private List<MonthlyDataPoint> historicalData;
    private List<MonthlyDataPoint> predictedData;

    public int getConsecutiveMonths() { return consecutiveMonths; }
    public void setConsecutiveMonths(int consecutiveMonths) { this.consecutiveMonths = consecutiveMonths; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public String getEmotionalMessage() { return emotionalMessage; }
    public void setEmotionalMessage(String emotionalMessage) { this.emotionalMessage = emotionalMessage; }

    public String getEmotionalMessageKey() { return emotionalMessageKey; }
    public void setEmotionalMessageKey(String emotionalMessageKey) { this.emotionalMessageKey = emotionalMessageKey; }

    public double getUnlockProgress() { return unlockProgress; }
    public void setUnlockProgress(double unlockProgress) { this.unlockProgress = unlockProgress; }

    public Long getPredictedNextMonth() { return predictedNextMonth; }
    public void setPredictedNextMonth(Long predictedNextMonth) { this.predictedNextMonth = predictedNextMonth; }

    public Long getConfidenceLow() { return confidenceLow; }
    public void setConfidenceLow(Long confidenceLow) { this.confidenceLow = confidenceLow; }

    public Long getConfidenceHigh() { return confidenceHigh; }
    public void setConfidenceHigh(Long confidenceHigh) { this.confidenceHigh = confidenceHigh; }

    public Long getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(Long fixedAmount) { this.fixedAmount = fixedAmount; }

    public Long getVariableAmount() { return variableAmount; }
    public void setVariableAmount(Long variableAmount) { this.variableAmount = variableAmount; }

    public List<MonthlyDataPoint> getHistoricalData() { return historicalData; }
    public void setHistoricalData(List<MonthlyDataPoint> historicalData) { this.historicalData = historicalData; }

    public List<MonthlyDataPoint> getPredictedData() { return predictedData; }
    public void setPredictedData(List<MonthlyDataPoint> predictedData) { this.predictedData = predictedData; }

    public static class MonthlyDataPoint {
        private String month;       // "YYYY-MM"
        private double amount;      // yuan (already /100)
        private boolean predicted;

        public MonthlyDataPoint() {}

        public MonthlyDataPoint(String month, double amount, boolean predicted) {
            this.month = month;
            this.amount = amount;
            this.predicted = predicted;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public boolean isPredicted() { return predicted; }
        public void setPredicted(boolean predicted) { this.predicted = predicted; }
    }
}

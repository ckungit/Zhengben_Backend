package com.zhangben.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangben.backend.model.KalmanFilterState;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PredictionEngine {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // State transition matrix: [[1,1],[0,1]]
    private static final double[][] A = {{1, 1}, {0, 1}};
    // Observation matrix: [[1,0]]
    private static final double[][] H = {{1, 0}};
    // Process noise covariance Q: diag(1000^2, 500^2) in cents^2
    private static final double[][] Q = {{1000000, 0}, {0, 250000}};

    public static class PredictionResult {
        public long predicted;       // cents
        public long confidenceLow;   // cents
        public long confidenceHigh;  // cents

        public PredictionResult(long predicted, long confidenceLow, long confidenceHigh) {
            this.predicted = predicted;
            this.confidenceLow = confidenceLow;
            this.confidenceHigh = confidenceHigh;
        }
    }

    /**
     * 3-Sigma outlier filtering: replace values > mean+3σ with median
     */
    public List<Long> filterOutliers(List<Long> observations) {
        if (observations.size() < 3) {
            return new ArrayList<>(observations);
        }

        double mean = observations.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = observations.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        List<Long> sorted = new ArrayList<>(observations);
        Collections.sort(sorted);
        long median = sorted.get(sorted.size() / 2);

        double upperBound = mean + 3 * stdDev;

        List<Long> filtered = new ArrayList<>();
        for (Long obs : observations) {
            if (obs > upperBound) {
                filtered.add(median);
            } else {
                filtered.add(obs);
            }
        }
        return filtered;
    }

    /**
     * Predict fixed expenses: use average with very tight confidence (near-zero noise).
     * Fixed expenses like rent/loan are highly predictable.
     */
    public PredictionResult predictFixed(List<Long> monthlyFixed) {
        if (monthlyFixed.isEmpty()) {
            return new PredictionResult(0, 0, 0);
        }
        // Use median of recent fixed spending as prediction
        List<Long> sorted = new ArrayList<>(monthlyFixed);
        Collections.sort(sorted);
        long median = sorted.get(sorted.size() / 2);

        // Very tight confidence for fixed costs (±5%)
        long low = Math.round(median * 0.95);
        long high = Math.round(median * 1.05);
        return new PredictionResult(median, low, high);
    }

    /**
     * Initialize and train Kalman filter from historical spending data
     */
    public KalmanFilterState initializeAndTrain(Integer userId, List<Long> monthlySpending,
                                                 String lastMonth, int consecutiveMonths) {
        List<Long> filtered = filterOutliers(monthlySpending);

        // Estimate observation noise R from data variance
        double mean = filtered.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = filtered.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(1000000);
        double[][] R = {{Math.max(variance * 0.5, 100000)}};

        // Initial state: [first observation, 0 trend]
        double initialLevel = filtered.isEmpty() ? 0 : filtered.get(0);
        double[] initialState = {initialLevel, 0};
        double[][] initialP = {{variance, 0}, {0, variance}};

        RealMatrix aMatrix = new Array2DRowRealMatrix(A);
        RealMatrix qMatrix = new Array2DRowRealMatrix(Q);
        RealMatrix hMatrix = new Array2DRowRealMatrix(H);
        RealMatrix rMatrix = new Array2DRowRealMatrix(R);
        RealVector x0 = new ArrayRealVector(initialState);
        RealMatrix p0 = new Array2DRowRealMatrix(initialP);

        ProcessModel pm = new DefaultProcessModel(aMatrix, null, qMatrix, x0, p0);
        MeasurementModel mm = new DefaultMeasurementModel(hMatrix, rMatrix);
        KalmanFilter kf = new KalmanFilter(pm, mm);

        // Train on all observations
        for (Long obs : filtered) {
            kf.predict();
            kf.correct(new double[]{obs});
        }

        // Extract state
        double[] stateEstimate = kf.getStateEstimation();
        double[][] errorCov = kf.getErrorCovarianceMatrix().getData();

        // Predict next month
        PredictionResult nextMonthPred = predictFromState(stateEstimate, errorCov, 1);

        KalmanFilterState state = new KalmanFilterState();
        state.setUserId(userId);
        state.setStateVector(serializeVector(stateEstimate));
        state.setCovarianceMatrix(serializeMatrix(errorCov));
        state.setPredictedNextMonth(nextMonthPred.predicted);
        state.setPredictedConfidenceLow(nextMonthPred.confidenceLow);
        state.setPredictedConfidenceHigh(nextMonthPred.confidenceHigh);
        state.setLastObservationMonth(lastMonth);
        state.setConsecutiveMonths(consecutiveMonths);
        state.setTotalObservations(filtered.size());

        return state;
    }

    /**
     * Update existing state with new observations
     */
    public KalmanFilterState updateWithNewObservations(KalmanFilterState existingState,
                                                        List<Long> newObservations,
                                                        String lastMonth,
                                                        int consecutiveMonths,
                                                        int totalObservations) {
        double[] currentState = deserializeVector(existingState.getStateVector());
        double[][] currentP = deserializeMatrix(existingState.getCovarianceMatrix());

        // Estimate R from previous data
        double[][] R = {{Math.max(currentP[0][0] * 0.5, 100000)}};

        RealMatrix aMatrix = new Array2DRowRealMatrix(A);
        RealMatrix qMatrix = new Array2DRowRealMatrix(Q);
        RealMatrix hMatrix = new Array2DRowRealMatrix(H);
        RealMatrix rMatrix = new Array2DRowRealMatrix(R);
        RealVector x0 = new ArrayRealVector(currentState);
        RealMatrix p0 = new Array2DRowRealMatrix(currentP);

        ProcessModel pm = new DefaultProcessModel(aMatrix, null, qMatrix, x0, p0);
        MeasurementModel mm = new DefaultMeasurementModel(hMatrix, rMatrix);
        KalmanFilter kf = new KalmanFilter(pm, mm);

        List<Long> filtered = filterOutliers(newObservations);
        for (Long obs : filtered) {
            kf.predict();
            kf.correct(new double[]{obs});
        }

        double[] stateEstimate = kf.getStateEstimation();
        double[][] errorCov = kf.getErrorCovarianceMatrix().getData();

        PredictionResult nextMonthPred = predictFromState(stateEstimate, errorCov, 1);

        existingState.setStateVector(serializeVector(stateEstimate));
        existingState.setCovarianceMatrix(serializeMatrix(errorCov));
        existingState.setPredictedNextMonth(nextMonthPred.predicted);
        existingState.setPredictedConfidenceLow(nextMonthPred.confidenceLow);
        existingState.setPredictedConfidenceHigh(nextMonthPred.confidenceHigh);
        existingState.setLastObservationMonth(lastMonth);
        existingState.setConsecutiveMonths(consecutiveMonths);
        existingState.setTotalObservations(totalObservations);

        return existingState;
    }

    /**
     * Predict future months from current state
     */
    public List<PredictionResult> predict(KalmanFilterState state, int monthsAhead) {
        double[] currentState = deserializeVector(state.getStateVector());
        double[][] currentP = deserializeMatrix(state.getCovarianceMatrix());

        List<PredictionResult> results = new ArrayList<>();
        RealMatrix aMatrix = new Array2DRowRealMatrix(A);
        RealMatrix qMatrix = new Array2DRowRealMatrix(Q);
        RealVector xVec = new ArrayRealVector(currentState);
        RealMatrix pMat = new Array2DRowRealMatrix(currentP);

        for (int i = 0; i < monthsAhead; i++) {
            // Predict step (without correction)
            xVec = aMatrix.operate(xVec);
            pMat = aMatrix.multiply(pMat).multiply(aMatrix.transpose()).add(qMatrix);

            double predicted = xVec.getEntry(0);
            double uncertainty = Math.sqrt(pMat.getEntry(0, 0));

            // 95% confidence interval (~2 sigma)
            long predCents = Math.max(0, Math.round(predicted));
            long low = Math.max(0, Math.round(predicted - 2 * uncertainty));
            long high = Math.max(0, Math.round(predicted + 2 * uncertainty));

            results.add(new PredictionResult(predCents, low, high));
        }

        return results;
    }

    private PredictionResult predictFromState(double[] state, double[][] P, int monthsAhead) {
        RealMatrix aMatrix = new Array2DRowRealMatrix(A);
        RealMatrix qMatrix = new Array2DRowRealMatrix(Q);
        RealVector xVec = new ArrayRealVector(state);
        RealMatrix pMat = new Array2DRowRealMatrix(P);

        for (int i = 0; i < monthsAhead; i++) {
            xVec = aMatrix.operate(xVec);
            pMat = aMatrix.multiply(pMat).multiply(aMatrix.transpose()).add(qMatrix);
        }

        double predicted = xVec.getEntry(0);
        double uncertainty = Math.sqrt(pMat.getEntry(0, 0));

        long predCents = Math.max(0, Math.round(predicted));
        long low = Math.max(0, Math.round(predicted - 2 * uncertainty));
        long high = Math.max(0, Math.round(predicted + 2 * uncertainty));

        return new PredictionResult(predCents, low, high);
    }

    // --- Serialization helpers ---

    private String serializeVector(double[] vec) {
        try {
            return objectMapper.writeValueAsString(vec);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize state vector", e);
        }
    }

    private String serializeMatrix(double[][] mat) {
        try {
            return objectMapper.writeValueAsString(mat);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize covariance matrix", e);
        }
    }

    private double[] deserializeVector(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize state vector", e);
        }
    }

    private double[][] deserializeMatrix(String json) {
        try {
            return objectMapper.readValue(json, double[][].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize covariance matrix", e);
        }
    }
}

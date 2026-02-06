package com.zhangben.backend.service.impl;

import com.zhangben.backend.dto.PredictionResponse;
import com.zhangben.backend.dto.PredictionResponse.MonthlyDataPoint;
import com.zhangben.backend.mapper.PredictionMapper;
import com.zhangben.backend.model.KalmanFilterState;
import com.zhangben.backend.model.UserMonthlySpending;
import com.zhangben.backend.service.PredictionEngine;
import com.zhangben.backend.service.PredictionEngine.PredictionResult;
import com.zhangben.backend.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PredictionServiceImpl implements PredictionService {

    @Autowired
    private PredictionMapper predictionMapper;

    @Autowired
    private PredictionEngine predictionEngine;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // Emotional messages by stage and language
    private static final Map<String, Map<Integer, String>> EMOTIONAL_MESSAGES = new HashMap<>();
    static {
        Map<Integer, String> zhCN = new HashMap<>();
        zhCN.put(1, "遇见你的第一个月。我正在努力记住你的消费节奏，请多指教。");
        zhCN.put(2, "慢慢变默契了。我已经察觉到你的一些消费规律了，再给我一点时间。");
        zhCN.put(3, "就快准备好了。下个月，我将为你开启专属的支出预测。");
        zhCN.put(4, "根据咱们共同记录的这段日子，我为你预估了下月的支出。");
        EMOTIONAL_MESSAGES.put("zh-CN", zhCN);

        Map<Integer, String> enUS = new HashMap<>();
        enUS.put(1, "Our first month together. I'm learning your spending rhythm.");
        enUS.put(2, "Getting in sync. I've noticed some patterns. Give me more time.");
        enUS.put(3, "Almost ready. Next month, I'll unlock your forecast.");
        enUS.put(4, "Based on our time together, here's my estimate.");
        EMOTIONAL_MESSAGES.put("en-US", enUS);

        Map<Integer, String> jaJP = new HashMap<>();
        jaJP.put(1, "初めましての一ヶ月。支出リズムを覚えている最中です。");
        jaJP.put(2, "だんだん息が合ってきました。もう少し時間をください。");
        jaJP.put(3, "もう少しで準備完了。来月、予測を開きます。");
        jaJP.put(4, "これまでの記録に基づいて、来月の支出を予測しました。");
        EMOTIONAL_MESSAGES.put("ja-JP", jaJP);
    }

    private static final Map<Integer, String> STAGE_KEYS = Map.of(
            1, "prediction.stage1",
            2, "prediction.stage2",
            3, "prediction.stage3",
            4, "prediction.stageUnlocked"
    );

    @Override
    public PredictionResponse getPrediction(Integer userId, String language) {
        // 1. Refresh materialized data
        List<UserMonthlySpending> aggregated = predictionMapper.aggregateMonthlySpending(userId);
        for (UserMonthlySpending record : aggregated) {
            record.setUserId(userId);
            predictionMapper.upsertMonthlySpending(record);
        }

        // 2. Load all monthly spending records
        List<UserMonthlySpending> monthlyRecords = predictionMapper.selectMonthlySpending(userId);

        // 3. Calculate consecutive months from current month backwards
        int consecutiveMonths = calculateConsecutiveMonths(monthlyRecords);

        // 4. Determine stage
        int stage = Math.min(consecutiveMonths, 4);
        if (stage < 1) stage = 1;
        boolean unlocked = consecutiveMonths >= 3;

        // 5. Build response
        PredictionResponse response = new PredictionResponse();
        response.setConsecutiveMonths(consecutiveMonths);
        response.setUnlocked(unlocked);
        response.setUnlockProgress(Math.min(consecutiveMonths / 3.0, 1.0));

        // Emotional message
        Map<Integer, String> langMessages = EMOTIONAL_MESSAGES.getOrDefault(language,
                EMOTIONAL_MESSAGES.get("zh-CN"));
        int msgStage = unlocked ? 4 : stage;
        response.setEmotionalMessage(langMessages.getOrDefault(msgStage, langMessages.get(1)));
        response.setEmotionalMessageKey(STAGE_KEYS.getOrDefault(msgStage, "prediction.stage1"));

        // Historical data points
        List<MonthlyDataPoint> historicalData = monthlyRecords.stream()
                .map(r -> new MonthlyDataPoint(r.getMonth(), r.getTotalSpending() / 100.0, false))
                .collect(Collectors.toList());
        response.setHistoricalData(historicalData);

        if (unlocked && !monthlyRecords.isEmpty()) {
            // 6. Category-aware prediction: Sum(Fixed) + Kalman_Predict(Variable)
            List<UserMonthlySpending> fixedRecords = predictionMapper.aggregateFixedSpending(userId);
            List<UserMonthlySpending> variableRecords = predictionMapper.aggregateVariableSpending(userId);

            // Variable spending: use Kalman filter
            List<Long> variableValues = variableRecords.stream()
                    .map(UserMonthlySpending::getTotalSpending)
                    .collect(Collectors.toList());
            String lastMonth = monthlyRecords.get(monthlyRecords.size() - 1).getMonth();

            KalmanFilterState existingState = predictionMapper.selectByUserId(userId);

            KalmanFilterState updatedState;
            // Train Kalman on variable spending only (if any), otherwise total
            List<Long> kalmanInput = variableValues.isEmpty()
                    ? monthlyRecords.stream().map(UserMonthlySpending::getTotalSpending).collect(Collectors.toList())
                    : variableValues;

            if (existingState == null) {
                updatedState = predictionEngine.initializeAndTrain(
                        userId, kalmanInput, lastMonth, consecutiveMonths);
                predictionMapper.insertKalmanState(updatedState);
            } else {
                updatedState = predictionEngine.initializeAndTrain(
                        userId, kalmanInput, lastMonth, consecutiveMonths);
                updatedState.setUserId(userId);
                predictionMapper.updateByUserId(updatedState);
            }

            // Predict variable portion 1~3 months ahead
            List<PredictionResult> variablePredictions = predictionEngine.predict(updatedState, 3);

            // Fixed portion: use median-based prediction (near-zero noise)
            List<Long> fixedValues = fixedRecords.stream()
                    .map(UserMonthlySpending::getTotalSpending)
                    .collect(Collectors.toList());
            PredictionResult fixedPrediction = predictionEngine.predictFixed(fixedValues);

            // Combine: Total = Fixed + Variable
            long combinedPredicted = fixedPrediction.predicted + variablePredictions.get(0).predicted;
            long combinedLow = fixedPrediction.confidenceLow + variablePredictions.get(0).confidenceLow;
            long combinedHigh = fixedPrediction.confidenceHigh + variablePredictions.get(0).confidenceHigh;

            response.setPredictedNextMonth(combinedPredicted);
            response.setConfidenceLow(combinedLow);
            response.setConfidenceHigh(combinedHigh);
            response.setFixedAmount(fixedPrediction.predicted);
            response.setVariableAmount(variablePredictions.get(0).predicted);

            // Build predicted data points (combined)
            YearMonth currentYM = YearMonth.parse(lastMonth, MONTH_FMT);
            List<MonthlyDataPoint> predictedData = new ArrayList<>();
            for (int i = 0; i < variablePredictions.size(); i++) {
                YearMonth futureMonth = currentYM.plusMonths(i + 1);
                long totalPred = fixedPrediction.predicted + variablePredictions.get(i).predicted;
                predictedData.add(new MonthlyDataPoint(
                        futureMonth.format(MONTH_FMT),
                        totalPred / 100.0,
                        true
                ));
            }
            response.setPredictedData(predictedData);
        } else {
            response.setPredictedData(Collections.emptyList());
        }

        return response;
    }

    /**
     * Calculate consecutive months with data, counting backwards from the current or most recent month
     */
    private int calculateConsecutiveMonths(List<UserMonthlySpending> records) {
        if (records.isEmpty()) return 0;

        Set<String> monthSet = records.stream()
                .map(UserMonthlySpending::getMonth)
                .collect(Collectors.toSet());

        // Start from current month and go backwards
        YearMonth current = YearMonth.now();
        int count = 0;

        // If current month has no data, start from the most recent month with data
        if (!monthSet.contains(current.format(MONTH_FMT))) {
            // Check if previous month has data (user might not have recorded this month yet)
            YearMonth prevMonth = current.minusMonths(1);
            if (monthSet.contains(prevMonth.format(MONTH_FMT))) {
                current = prevMonth;
            } else {
                return 0;
            }
        }

        while (monthSet.contains(current.format(MONTH_FMT))) {
            count++;
            current = current.minusMonths(1);
        }

        return count;
    }
}

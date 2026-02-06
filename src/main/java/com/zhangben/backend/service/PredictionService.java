package com.zhangben.backend.service;

import com.zhangben.backend.dto.PredictionResponse;

public interface PredictionService {
    PredictionResponse getPrediction(Integer userId, String language);
}

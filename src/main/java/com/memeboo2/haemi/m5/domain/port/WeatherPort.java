package com.memeboo2.haemi.m5.domain.port;

import com.memeboo2.haemi.m5.domain.model.care.WeatherCondition;

public interface WeatherPort {
    WeatherCondition currentCondition(String elderId);
}

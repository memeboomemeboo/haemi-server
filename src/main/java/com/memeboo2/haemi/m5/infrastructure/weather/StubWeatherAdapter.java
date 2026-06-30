package com.memeboo2.haemi.m5.infrastructure.weather;

import com.memeboo2.haemi.m5.domain.model.care.WeatherCondition;
import com.memeboo2.haemi.m5.domain.port.WeatherPort;
import org.springframework.stereotype.Component;

@Component
public class StubWeatherAdapter implements WeatherPort {
    @Override
    public WeatherCondition currentCondition(String elderId) {
        return WeatherCondition.CLEAR;
    }
}

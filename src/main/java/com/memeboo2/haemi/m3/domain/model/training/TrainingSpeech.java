package com.memeboo2.haemi.m3.domain.model.training;

public record TrainingSpeech(
        String text,
        String ssml,
        String locale,
        double speechRate
) {
}

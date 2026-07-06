package com.memeboo2.haemi.m3.domain.port;

import com.memeboo2.haemi.m3.domain.model.training.TrainingSpeech;

public interface TrainingSpeechSynthesisPort {

    TrainingSpeech synthesize(String text);
}

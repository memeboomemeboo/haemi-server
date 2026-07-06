package com.memeboo2.haemi.m3.infrastructure.tts;

import com.memeboo2.haemi.m3.domain.model.training.TrainingSpeech;
import com.memeboo2.haemi.m3.domain.port.TrainingSpeechSynthesisPort;
import org.springframework.stereotype.Component;

@Component
public class SsmlTrainingSpeechAdapter implements TrainingSpeechSynthesisPort {

    private static final String LOCALE = "ko-KR";
    private static final double SPEECH_RATE = 0.85;

    @Override
    public TrainingSpeech synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("TTS로 변환할 문장이 필요합니다.");
        }
        String ssml = """
                <speak xml:lang="%s"><prosody rate="85%%">%s</prosody></speak>
                """.formatted(LOCALE, escapeXml(text)).trim();
        return new TrainingSpeech(text, ssml, LOCALE, SPEECH_RATE);
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

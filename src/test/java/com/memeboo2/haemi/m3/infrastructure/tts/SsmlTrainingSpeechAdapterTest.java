package com.memeboo2.haemi.m3.infrastructure.tts;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsmlTrainingSpeechAdapterTest {

    private final SsmlTrainingSpeechAdapter adapter = new SsmlTrainingSpeechAdapter();

    @Test
    void createsKoreanSsmlAndEscapesUserVisibleText() {
        var speech = adapter.synthesize("가족 <사진> & 추억");

        assertThat(speech.text()).isEqualTo("가족 <사진> & 추억");
        assertThat(speech.locale()).isEqualTo("ko-KR");
        assertThat(speech.speechRate()).isEqualTo(0.85);
        assertThat(speech.ssml())
                .contains("rate=\"85%\"")
                .contains("가족 &lt;사진&gt; &amp; 추억");
    }

    @Test
    void rejectsBlankSpeechText() {
        assertThatThrownBy(() -> adapter.synthesize(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

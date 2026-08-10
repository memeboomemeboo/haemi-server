package com.memeboo2.haemi.m2.infrastructure.stt;

import com.memeboo2.haemi.m2.domain.model.post.UnsupportedVoiceContentTypeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceInputTooLargeException;
import com.memeboo2.haemi.m2.infrastructure.ai.GeminiGenerationClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GeminiSttAdapterTest {

    private final GeminiGenerationClient gemini = mock(GeminiGenerationClient.class);

    @Test
    void sendsTheOriginalAudioBytesToGeminiAndNormalizesTranscriptWhitespace() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 1024);
        when(gemini.transcribe(any(), eq("audio/mpeg"))).thenReturn("  고마워요\n정말요  ");

        String transcript = adapter.transcribe(
                new ByteArrayInputStream("voice".getBytes(StandardCharsets.UTF_8)), "audio/mpeg; charset=binary");

        ArgumentCaptor<byte[]> audio = ArgumentCaptor.forClass(byte[].class);
        verify(gemini).transcribe(audio.capture(), eq("audio/mpeg"));
        assertThat(audio.getValue()).containsExactly("voice".getBytes(StandardCharsets.UTF_8));
        assertThat(transcript).isEqualTo("고마워요 정말요");
    }

    @Test
    void rejectsNonAudioContentTypeWithoutCallingTheModel() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 1024);

        assertThatThrownBy(() -> adapter.transcribe(new ByteArrayInputStream(new byte[]{1}), "text/plain"))
                .isInstanceOf(UnsupportedVoiceContentTypeException.class);
        verifyNoInteractions(gemini);
    }

    @Test
    void rejectsAudioThatCannotFitInGeminiInlineRequest() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 3);

        assertThatThrownBy(() -> adapter.transcribe(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), "audio/mpeg"))
                .isInstanceOf(VoiceInputTooLargeException.class);
        verifyNoInteractions(gemini);
    }
}

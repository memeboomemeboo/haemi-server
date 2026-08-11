package com.memeboo2.haemi.m2.infrastructure.stt;

import com.memeboo2.haemi.m2.domain.model.post.UnsupportedVoiceContentTypeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceInputTooLargeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceTranscriptTooLongException;
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
                new ByteArrayInputStream("voice".getBytes(StandardCharsets.UTF_8)),
                "audio/mpeg; charset=binary", "reply.mp3");

        ArgumentCaptor<byte[]> audio = ArgumentCaptor.forClass(byte[].class);
        verify(gemini).transcribe(audio.capture(), eq("audio/mpeg"));
        assertThat(audio.getValue()).containsExactly("voice".getBytes(StandardCharsets.UTF_8));
        assertThat(transcript).isEqualTo("고마워요 정말요");
    }

    @Test
    void rejectsNonAudioContentTypeWithoutCallingTheModel() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 1024);

        assertThatThrownBy(() -> adapter.transcribe(new ByteArrayInputStream(new byte[]{1}), "text/plain", "reply.txt"))
                .isInstanceOf(UnsupportedVoiceContentTypeException.class);
        verifyNoInteractions(gemini);
    }

    @Test
    void rejectsAudioThatCannotFitInGeminiInlineRequest() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 3);

        assertThatThrownBy(() -> adapter.transcribe(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), "audio/mpeg", "reply.mp3"))
                .isInstanceOf(VoiceInputTooLargeException.class);
        verifyNoInteractions(gemini);
    }

    @Test
    void infersAudioMimeTypeFromFilenameWhenMobileSendsOctetStream() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 1024);
        when(gemini.transcribe(any(), eq("audio/mp4"))).thenReturn("고마워요");

        String transcript = adapter.transcribe(new ByteArrayInputStream(new byte[]{1}),
                "application/octet-stream", "voice.m4a");

        assertThat(transcript).isEqualTo("고마워요");
        verify(gemini).transcribe(any(), eq("audio/mp4"));
    }

    @Test
    void rejectsAnOverlongTranscriptBeforeItCanReachTheReplyEntity() {
        GeminiSttAdapter adapter = new GeminiSttAdapter(gemini, 1024);
        when(gemini.transcribe(any(), any())).thenReturn("가".repeat(301));

        assertThatThrownBy(() -> adapter.transcribe(new ByteArrayInputStream(new byte[]{1}),
                "audio/mpeg", "reply.mp3"))
                .isInstanceOf(VoiceTranscriptTooLongException.class)
                .hasMessageContaining("짧게 다시 녹음");
    }

    @Test
    void rejectsUnsafeInlineAudioLimitDuringConstruction() {
        assertThatThrownBy(() -> new GeminiSttAdapter(gemini, 12L * 1024 * 1024 + 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inline-audio-max-bytes");
    }
}

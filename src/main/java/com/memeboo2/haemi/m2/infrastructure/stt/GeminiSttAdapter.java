package com.memeboo2.haemi.m2.infrastructure.stt;

import com.memeboo2.haemi.m2.domain.model.post.AiGenerationUnavailableException;
import com.memeboo2.haemi.m2.domain.model.post.EmptyReplyContentException;
import com.memeboo2.haemi.m2.domain.model.post.UnsupportedVoiceContentTypeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceInputTooLargeException;
import com.memeboo2.haemi.m2.domain.port.SttPort;
import com.memeboo2.haemi.m2.infrastructure.ai.GeminiGenerationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Gemini audio understanding으로 비실시간 음성 답변을 전사한다. */
@Component
public class GeminiSttAdapter implements SttPort {

    private final GeminiGenerationClient gemini;
    private final long maxInlineAudioBytes;

    @Autowired
    public GeminiSttAdapter(
            GeminiGenerationClient gemini,
            @Value("${haemi.ai.gemini.inline-audio-max-bytes:15MB}") DataSize maxInlineAudioSize
    ) {
        this.gemini = gemini;
        this.maxInlineAudioBytes = maxInlineAudioSize.toBytes();
    }

    GeminiSttAdapter(GeminiGenerationClient gemini, long maxInlineAudioBytes) {
        this.gemini = gemini;
        this.maxInlineAudioBytes = maxInlineAudioBytes;
    }

    @Override
    public String transcribe(InputStream audioStream, String contentType) {
        if (audioStream == null) {
            throw new EmptyReplyContentException();
        }
        String normalizedContentType = normalizeAudioContentType(contentType);
        byte[] audio = readAtMost(audioStream);
        if (audio.length == 0) {
            throw new EmptyReplyContentException();
        }
        return gemini.transcribe(audio, normalizedContentType).replaceAll("\\s+", " ").trim();
    }

    private String normalizeAudioContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new UnsupportedVoiceContentTypeException();
        }
        String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("audio/")) {
            throw new UnsupportedVoiceContentTypeException();
        }
        return normalized;
    }

    private byte[] readAtMost(InputStream audioStream) {
        if (maxInlineAudioBytes >= Integer.MAX_VALUE) {
            throw new IllegalStateException("Gemini inline audio maximum must fit into an in-memory request.");
        }
        try (audioStream) {
            byte[] audio = audioStream.readNBytes((int) maxInlineAudioBytes + 1);
            if (audio.length > maxInlineAudioBytes) {
                throw new VoiceInputTooLargeException(maxInlineAudioBytes);
            }
            return audio;
        } catch (VoiceInputTooLargeException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new AiGenerationUnavailableException("음성 답변을 읽지 못했어요. 다시 녹음해주세요.", exception);
        }
    }
}

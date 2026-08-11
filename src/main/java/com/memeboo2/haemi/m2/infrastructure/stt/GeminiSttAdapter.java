package com.memeboo2.haemi.m2.infrastructure.stt;

import com.memeboo2.haemi.m2.domain.model.post.AiGenerationRateLimitedException;
import com.memeboo2.haemi.m2.domain.model.post.AiGenerationUnavailableException;
import com.memeboo2.haemi.m2.domain.model.post.EmptyReplyContentException;
import com.memeboo2.haemi.m2.domain.model.post.UnsupportedVoiceContentTypeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceInputTooLargeException;
import com.memeboo2.haemi.m2.domain.model.post.VoiceTranscriptTooLongException;
import com.memeboo2.haemi.m2.domain.port.SttPort;
import com.memeboo2.haemi.m2.infrastructure.ai.GeminiGenerationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.Semaphore;

/** Gemini audio understanding으로 비실시간 음성 답변을 전사한다. */
@Component
public class GeminiSttAdapter implements SttPort {

    // 12MB raw audio becomes about 16MB base64 before JSON serialization. Gemini inlineData 20MB 한도와 힙 여유를 함께 지킨다.
    private static final long MAX_SAFE_INLINE_AUDIO_BYTES = 12L * 1024 * 1024;
    private static final int MAX_TRANSCRIPT_LENGTH = 300;

    private final GeminiGenerationClient gemini;
    private final long maxInlineAudioBytes;
    private final Semaphore audioRequestPermits;

    @Autowired
    public GeminiSttAdapter(
            GeminiGenerationClient gemini,
            @Value("${haemi.ai.gemini.inline-audio-max-bytes:12MB}") DataSize maxInlineAudioSize,
            @Value("${haemi.ai.gemini.max-concurrent-audio-requests:2}") int maxConcurrentAudioRequests
    ) {
        this(gemini, maxInlineAudioSize.toBytes(), maxConcurrentAudioRequests);
    }

    GeminiSttAdapter(GeminiGenerationClient gemini, long maxInlineAudioBytes) {
        this(gemini, maxInlineAudioBytes, 1);
    }

    GeminiSttAdapter(GeminiGenerationClient gemini, long maxInlineAudioBytes, int maxConcurrentAudioRequests) {
        this.gemini = gemini;
        validateConfiguration(maxInlineAudioBytes, maxConcurrentAudioRequests);
        this.maxInlineAudioBytes = maxInlineAudioBytes;
        this.audioRequestPermits = new Semaphore(maxConcurrentAudioRequests);
    }

    @Override
    public String transcribe(InputStream audioStream, String contentType, String originalFilename) {
        if (audioStream == null) {
            throw new EmptyReplyContentException();
        }
        String normalizedContentType = normalizeAudioContentType(contentType, originalFilename);
        if (!audioRequestPermits.tryAcquire()) {
            throw new AiGenerationRateLimitedException("음성 전사 요청이 많아요. 잠시 후 다시 시도해주세요.");
        }
        try {
            byte[] audio = readAtMost(audioStream);
            if (audio.length == 0) {
                throw new EmptyReplyContentException();
            }
            String transcript = gemini.transcribe(audio, normalizedContentType).replaceAll("\\s+", " ").trim();
            if (transcript.length() > MAX_TRANSCRIPT_LENGTH) {
                throw new VoiceTranscriptTooLongException(MAX_TRANSCRIPT_LENGTH);
            }
            return transcript;
        } finally {
            audioRequestPermits.release();
        }
    }

    private String normalizeAudioContentType(String contentType, String originalFilename) {
        if (contentType != null && !contentType.isBlank()) {
            String normalized = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("audio/")) {
                return normalized;
            }
        }
        return inferAudioContentType(originalFilename);
    }

    private String inferAudioContentType(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new UnsupportedVoiceContentTypeException();
        }
        String filename = originalFilename.toLowerCase(Locale.ROOT);
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        if (filename.endsWith(".m4a") || filename.endsWith(".mp4")) return "audio/mp4";
        if (filename.endsWith(".aac")) return "audio/aac";
        if (filename.endsWith(".wav")) return "audio/wav";
        if (filename.endsWith(".ogg") || filename.endsWith(".oga")) return "audio/ogg";
        if (filename.endsWith(".webm")) return "audio/webm";
        if (filename.endsWith(".3gp")) return "audio/3gpp";
        throw new UnsupportedVoiceContentTypeException();
    }

    private byte[] readAtMost(InputStream audioStream) {
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

    private static void validateConfiguration(long maxInlineAudioBytes, int maxConcurrentAudioRequests) {
        if (maxInlineAudioBytes <= 0 || maxInlineAudioBytes > MAX_SAFE_INLINE_AUDIO_BYTES) {
            throw new IllegalStateException("haemi.ai.gemini.inline-audio-max-bytes must be between 1B and 12MB.");
        }
        if (maxConcurrentAudioRequests <= 0) {
            throw new IllegalStateException("haemi.ai.gemini.max-concurrent-audio-requests must be positive.");
        }
    }
}

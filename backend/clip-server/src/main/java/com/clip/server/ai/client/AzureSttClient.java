package com.clip.server.ai.client;

import com.clip.server.file.service.S3Service;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureSttClient {

    private final SpeechConfig speechConfig;
    private final S3Service s3Service;
    private final AudioConverter audioConverter;

    /**
     * S3 URL의 음성 파일을 텍스트로 변환
     */
    public String transcribe(String audioUrl) {
        Path downloadedFile = null;
        Path wavFile = null;

        try {
            // 1. S3에서 다운로드 (WebM 등 원본 형식)
            downloadedFile = s3Service.downloadToTempFile(audioUrl);

            // 2.  WAV로 변환 (Azure STT 호환)
            wavFile = audioConverter.convertToWav(downloadedFile);

            // 3. STT 변환
            String result = transcribeFromFile(wavFile.toString());
            log.info("STT 변환 완료. result={}", result);

            return result;

        } catch (Exception e) {
            log.error("STT 변환 실패. audioUrl={}", audioUrl, e);
            throw new RuntimeException("음성 인식에 실패했습니다.", e);
        } finally {
            // 임시 파일 정리 (둘 다!)
            deleteTempFile(downloadedFile);
            deleteTempFile(wavFile);
        }
    }

    /**
     * 로컬 파일에서 STT 변환 (WAV 전용)
     */
    public String transcribeFromFile(String filePath) throws Exception {
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(filePath);
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        try {
            Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
            SpeechRecognitionResult result = task.get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                return result.getText();
            } else if (result.getReason() == ResultReason.NoMatch) {
                log.warn("음성 인식 불가 (NoMatch)");
                throw new RuntimeException("음성을 인식할 수 없습니다. 더 또렷하게 말씀해주세요.");
            } else if (result.getReason() == ResultReason.Canceled) {
                CancellationDetails details = CancellationDetails.fromResult(result);
                log.error("STT 취소. reason={}, errorDetails={}",
                        details.getReason(), details.getErrorDetails());
                throw new RuntimeException("음성 인식 실패: " + details.getErrorDetails());
            } else {
                throw new RuntimeException("STT 실패: " + result.getReason());
            }
        } finally {
            recognizer.close();
            audioConfig.close();
        }
    }

    /**
     * 임시 파일 삭제
     */
    private void deleteTempFile(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패. path={}", tempFile, e);
            }
        }
    }
}
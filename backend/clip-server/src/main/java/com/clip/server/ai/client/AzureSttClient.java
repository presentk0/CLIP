package com.clip.server.ai.client;

import com.clip.server.file.service.S3Service;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
     *  S3 URL의 음성 파일을 STT + 발음 평가 동시 수행
     * @return SttResult (텍스트 + 발음 점수)
     */
    public SttResult transcribeWithPronunciation(String audioUrl) {
        Path downloadedFile = null;
        Path wavFile = null;

        try {
            // 1. S3에서 다운로드
            downloadedFile = s3Service.downloadToTempFile(audioUrl);

            // 2. WAV로 변환
            wavFile = audioConverter.convertToWav(downloadedFile);

            // 3. STT + 발음 평가 동시 수행
            SttResult result = transcribeAndAssessFromFile(wavFile.toString());
            log.info("STT + 발음 평가 완료. text={}, score={}",
                    result.getText(), result.getPronunciationScore());

            return result;

        } catch (Exception e) {
            log.error("STT + 발음 평가 실패. audioUrl={}", audioUrl, e);
            throw new RuntimeException("음성 인식에 실패했습니다.", e);
        } finally {
            deleteTempFile(downloadedFile);
            deleteTempFile(wavFile);
        }
    }

    /**
     * S3 URL의 음성 파일을 텍스트로 변환 (기존 - 발음 평가 없음)
     * → 필요 없으면 삭제하거나, 기존 호출부 그대로 두려면 유지
     */
    public String transcribe(String audioUrl) {
        Path downloadedFile = null;
        Path wavFile = null;

        try {
            downloadedFile = s3Service.downloadToTempFile(audioUrl);
            wavFile = audioConverter.convertToWav(downloadedFile);

            String result = transcribeFromFile(wavFile.toString());
            log.info("STT 변환 완료. result={}", result);

            return result;

        } catch (Exception e) {
            log.error("STT 변환 실패. audioUrl={}", audioUrl, e);
            throw new RuntimeException("음성 인식에 실패했습니다.", e);
        } finally {
            deleteTempFile(downloadedFile);
            deleteTempFile(wavFile);
        }
    }

    /**
     *  로컬 WAV 파일에서 STT + 발음 평가 동시 수행
     */
    private SttResult transcribeAndAssessFromFile(String filePath) throws Exception {
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(filePath);
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        // 발음 평가 설정
        // referenceText는 빈 문자열 = "Unscripted" 모드 (STT 결과 기준으로 평가)
        PronunciationAssessmentConfig pronunciationConfig = new PronunciationAssessmentConfig(
                "",  // referenceText: 빈 문자열 = Unscripted (사용자가 뭐라 했는지 모르니까)
                PronunciationAssessmentGradingSystem.HundredMark,  // 0~100점
                PronunciationAssessmentGranularity.Phoneme,  // 음소 단위까지
                true  // enableMiscue: 잘못 말한 것도 감지
        );

        pronunciationConfig.applyTo(recognizer);

        try {
            Future<SpeechRecognitionResult> task = recognizer.recognizeOnceAsync();
            SpeechRecognitionResult result = task.get();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                String text = result.getText();

                // 발음 평가 결과 추출
                PronunciationAssessmentResult pronunciationResult =
                        PronunciationAssessmentResult.fromResult(result);

                BigDecimal pronunciationScore = null;
                if (pronunciationResult != null) {
                    pronunciationScore = BigDecimal.valueOf(pronunciationResult.getPronunciationScore())
                            .setScale(2, RoundingMode.HALF_UP);
                    log.info("발음 평가 완료. score={}, accuracy={}, fluency={}, completeness={}",
                            pronunciationResult.getPronunciationScore(),
                            pronunciationResult.getAccuracyScore(),
                            pronunciationResult.getFluencyScore(),
                            pronunciationResult.getCompletenessScore());
                } else {
                    log.warn("발음 평가 결과 없음");
                }

                return SttResult.builder()
                        .text(text)
                        .pronunciationScore(pronunciationScore)
                        .build();

            } else if (result.getReason() == ResultReason.NoMatch) {
                log.warn("음성 인식 불가 (NoMatch)");
                throw new RuntimeException("음성을 인식할 수 없습니다.");
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
            pronunciationConfig.close();
        }
    }

    /**
     * 로컬 파일에서 STT 변환 (WAV 전용, 발음 평가 없음)
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
                throw new RuntimeException("음성을 인식할 수 없습니다.");
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

    /**
     *  STT + 발음 평가 결과
     */
    @Getter
    @Builder
    public static class SttResult {
        private final String text;
        private final BigDecimal pronunciationScore;  // 0~100 (null이면 평가 실패)
    }
}
package com.clip.server.ai.client;

import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureTtsClient {

    private final SpeechConfig speechConfig;

    @Value("${azure.speech.voice-name-female}")
    private String voiceNameFemale;

    @Value("${azure.speech.voice-name-male}")
    private String voiceNameMale;

    /**
     * 텍스트를 음성(MP3 byte[])으로 변환
     *
     * @param text   변환할 텍스트
     * @param gender "FEMALE" or "MALE"
     * @return MP3 음성 데이터 (byte 배열)
     */
    public byte[] synthesize(String text, String gender) {
        // 성별에 따라 음성 선택
        String voiceName = "MALE".equalsIgnoreCase(gender) ? voiceNameMale : voiceNameFemale;
        speechConfig.setSpeechSynthesisVoiceName(voiceName);
        // MP3 출력 포맷 설정
        speechConfig.setSpeechSynthesisOutputFormat(
                SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3
        );

        // 메모리에서 직접 처리 (파일 X)
        SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null);

        try {
            Future<SpeechSynthesisResult> task = synthesizer.SpeakTextAsync(text);
            SpeechSynthesisResult result = task.get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                byte[] audioData = result.getAudioData();
                log.info("TTS 변환 완료. text 길이={}, 음성 크기={}KB",
                        text.length(), audioData.length / 1024);
                return audioData;
            } else {
                log.error("TTS 실패. reason={}", result.getReason());
                throw new RuntimeException("음성 합성 실패: " + result.getReason());
            }
        } catch (Exception e) {
            log.error("TTS 변환 중 오류. text={}", text, e);
            throw new RuntimeException("음성 합성 중 오류가 발생했습니다.", e);
        } finally {
            synthesizer.close();
        }
    }


    /**
     * 텍스트를 WAV 음성으로 변환 (STT 테스트용)
     */
    public byte[] synthesizeWav(String text, String gender) {
        String voiceName = "MALE".equalsIgnoreCase(gender) ? voiceNameMale : voiceNameFemale;
        speechConfig.setSpeechSynthesisVoiceName(voiceName);

        // WAV 출력 포맷 (16kHz, 16bit, Mono - STT에 최적)
        speechConfig.setSpeechSynthesisOutputFormat(
                SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm
        );

        SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null);

        try {
            Future<SpeechSynthesisResult> task = synthesizer.SpeakTextAsync(text);
            SpeechSynthesisResult result = task.get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                return result.getAudioData();
            } else {
                throw new RuntimeException("WAV 합성 실패: " + result.getReason());
            }
        } catch (Exception e) {
            throw new RuntimeException("WAV 합성 중 오류", e);
        } finally {
            synthesizer.close();
        }
    }
}
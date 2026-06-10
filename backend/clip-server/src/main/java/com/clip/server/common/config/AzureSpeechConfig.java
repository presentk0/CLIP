package com.clip.server.common.config;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AzureSpeechConfig {

    @Value("${azure.speech.key}")
    private String speechKey;

    @Value("${azure.speech.region}")
    private String speechRegion;

    /**
     * Azure Speech Service 공통 설정
     * - STT, TTS, 발음 평가에서 공통 사용
     */
    @Bean
    public SpeechConfig speechConfig() {
        SpeechConfig config = SpeechConfig.fromSubscription(speechKey, speechRegion);
        // 음성 인식 언어 (영어)
        config.setSpeechRecognitionLanguage("en-US");
        return config;
    }
}
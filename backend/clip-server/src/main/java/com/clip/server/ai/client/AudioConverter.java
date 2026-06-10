package com.clip.server.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Component
public class AudioConverter {

    /**
     * WebM/MP3/M4A 등을 Azure STT가 인식 가능한 WAV로 변환
     * - 16kHz, 16bit, Mono PCM
     */
    public Path convertToWav(Path inputFile) throws IOException, InterruptedException {
        Path outputFile = Files.createTempFile("converted_" + UUID.randomUUID(), ".wav");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-y",                       // 덮어쓰기
                "-i", inputFile.toString(),  // 입력 파일
                "-ar", "16000",              // 샘플레이트 16kHz
                "-ac", "1",                  // 모노
                "-c:a", "pcm_s16le",         // 16bit PCM
                outputFile.toString()        // 출력 파일
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpeg 출력 로그 (디버깅용)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            Files.deleteIfExists(outputFile);
            throw new IOException("FFmpeg 변환 실패. exitCode=" + exitCode);
        }

        log.info("음성 변환 완료. {} → {} ({}KB)",
                inputFile.getFileName(),
                outputFile.getFileName(),
                Files.size(outputFile) / 1024);

        return outputFile;
    }
}
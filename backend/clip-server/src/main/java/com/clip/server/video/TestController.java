package com.clip.server.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${youtube.api.key}")
    private String apiKey;

    @GetMapping("/test/api-key")
    public String testApiKey() {
        // 키 일부만 표시 (보안)
        return apiKey.substring(0, 10) + "...";
    }
}
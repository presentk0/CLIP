package com.clip.server.translation.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeepLClient implements TranslationClient {

    @Value("${deepl.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";

    @Override
    public List<String> translateBatch(List<String> texts) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("target_lang", "KO");
        for (String text : texts) {
            map.add("text", text);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        // API 호출
        Map<String, Object> response = restTemplate.postForObject(DEEPL_API_URL, request, Map.class);

        if (response == null || !response.containsKey("translations")) {
            return List.of();
        }

        List<Map<String, String>> translations = (List<Map<String, String>>) response.get("translations");

        return translations.stream()
                .map(t -> t.get("text"))
                .toList();
    }
}

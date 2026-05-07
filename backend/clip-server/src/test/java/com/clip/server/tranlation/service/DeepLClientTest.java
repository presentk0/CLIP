package com.clip.server.tranlation.service;

import com.clip.server.common.config.AppConfig;
import com.clip.server.translation.client.DeepLClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(DeepLClient.class)
@Import(AppConfig.class)
@ActiveProfiles("test")
public class DeepLClientTest {

    @Autowired
    private DeepLClient deepLClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @DisplayName("DeepL API로 번역 요청을 보내고 결과를 리스트로 받는다")
    void translateBatch_Success() {
        // given
        List<String> inputs = List.of("Hello", "World");
        String expectedResponse = "{\"translations\": [{\"text\": \"안녕\"}, {\"text\": \"세상\"}]}";

        mockServer.expect(requestTo("https://api.deepl.com/v2/translate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "DeepL-Auth-Key test-key"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        // when
        List<String> results = deepLClient.translateBatch(inputs);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo("안녕");
        assertThat(results.get(1)).isEqualTo("세상");
    }
}
package com.clip.server.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // allowedOrigins 대신 allowedOriginPatterns를 사용하세요!
                .allowedOriginPatterns(
                        "chrome-extension://nodoegcapbckibocgdleinfoniaboejg",
                        "chrome-extension://hbhkcidgpjdniofchhofhekjfdcdkkde",
                        "chrome-extension://hchkjnbfbmfgfidilneahojmiddkdeaj",
                        "chrome-extension://eaodakmjolhkaoaofnkfbcodihnncmkn",
                        "chrome-extension://hldepbmgooakmbnbobafccbngfflhilj",
                        "chrome-extension://fiikffmgckeladckjjoalgbeljanpbal",
                        "http://clip-server.com",
                        "https://clip-server.com",
                        "https://*.youtube.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // OPTIONS 추가 권장
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

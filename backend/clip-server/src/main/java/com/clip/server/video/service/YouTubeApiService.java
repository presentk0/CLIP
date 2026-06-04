package com.clip.server.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.clip.server.video.dto.response.VideoMetadata;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeApiService {

    private final RestTemplate restTemplate;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${youtube.api.base-url}")
    private String baseUrl;

    /**
     * videoId로 채널 프로필 이미지 URL 조회
     * - 실패 시 null 반환 (영상 저장은 계속 진행)
     */
    public String getChannelProfileUrl(String videoId) {
        try {
            // 1. videoId로 channelId 조회
            String channelId = getChannelIdByVideoId(videoId);
            if (channelId == null) {
                log.warn("YouTube channelId 조회 실패: videoId={}", videoId);
                return null;
            }

            // 2. channelId로 프로필 이미지 URL 조회
            String profileUrl = getChannelThumbnailUrl(channelId);
            log.info("YouTube 채널 프로필 조회 성공: videoId={}, channelId={}", videoId, channelId);
            return profileUrl;

        } catch (Exception e) {
            log.error("YouTube API 호출 실패: videoId={}, error={}", videoId, e.getMessage());
            return null;  // 실패해도 영상 저장은 진행
        }
    }

    /**
     * videoId → channelId 조회
     */
    @SuppressWarnings("unchecked")
    private String getChannelIdByVideoId(String videoId) {
        String url = String.format("%s/videos?part=snippet&id=%s&key=%s",
                baseUrl, videoId, apiKey);

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null) return null;

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return null;

        Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");
        return (String) snippet.get("channelId");
    }

    /**
     * channelId → 채널 프로필 이미지 URL 조회
     */
    @SuppressWarnings("unchecked")
    private String getChannelThumbnailUrl(String channelId) {
        String url = String.format("%s/channels?part=snippet&id=%s&key=%s",
                baseUrl, channelId, apiKey);

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null) return null;

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return null;

        Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");
        Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
        Map<String, Object> defaultThumb = (Map<String, Object>) thumbnails.get("default");

        return (String) defaultThumb.get("url");
    }

    /**
     * videoId로 영상 전체 메타데이터 조회
     */
    public VideoMetadata getVideoMetadata(String videoId) {
        try {
            // 1. videos API로 영상 + 채널 정보 한 번에 조회
            String url = String.format(
                    "%s/videos?part=snippet,contentDetails&id=%s&key=%s",
                    baseUrl, videoId, apiKey
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) return null;

            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            if (items == null || items.isEmpty()) return null;

            Map<String, Object> item = items.get(0);
            Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
            Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");

            // 2. 메타데이터 추출
            String title = (String) snippet.get("title");
            String channelId = (String) snippet.get("channelId");
            String channelName = (String) snippet.get("channelTitle");

            // 썸네일 (high 화질 우선, 없으면 default)
            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
            String thumbnailUrl = extractThumbnailUrl(thumbnails);

            // 영상 길이 (ISO 8601 형식 → 초 변환)
            String durationStr = (String) contentDetails.get("duration");
            Integer duration = parseDuration(durationStr);

            // 3. 채널 프로필 URL 별도 조회
            String channelProfileUrl = getChannelThumbnailUrl(channelId);

            return VideoMetadata.builder()
                    .videoId(videoId)
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .duration(duration)
                    .channelName(channelName)
                    .channelProfileImageUrl(channelProfileUrl)
                    .build();

        } catch (Exception e) {
            log.error("YouTube 메타데이터 조회 실패: videoId={}", videoId, e);
            return null;
        }
    }

    /**
     * 썸네일 URL 추출 (high → medium → default 순)
     */
    @SuppressWarnings("unchecked")
    private String extractThumbnailUrl(Map<String, Object> thumbnails) {
        if (thumbnails == null) return null;

        String[] qualities = {"high", "medium", "default"};
        for (String quality : qualities) {
            Map<String, Object> thumb = (Map<String, Object>) thumbnails.get(quality);
            if (thumb != null && thumb.get("url") != null) {
                return (String) thumb.get("url");
            }
        }
        return null;
    }

    /**
     * ISO 8601 duration (PT4M13S) → 초로 변환
     */
    private Integer parseDuration(String durationStr) {
        if (durationStr == null) return null;
        try {
            return (int) java.time.Duration.parse(durationStr).getSeconds();
        } catch (Exception e) {
            log.warn("Duration 파싱 실패: {}", durationStr);
            return null;
        }
    }
}
package com.clip.server.analytics.mapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class EventNameMapper {

    // 정적 경로 (ID 없는 API)
    private static final Map<String, String> EVENT_MAP = Map.ofEntries(
            // 인증
            Map.entry("POST /api/auth/google/login", "LOGIN"),
            Map.entry("POST /api/auth/logout", "LOGOUT"),

            // 온보딩 & 설정
            Map.entry("POST /api/preferences/onboarding", "ONBOARDING_COMPLETE"),
            Map.entry("PATCH /api/users/me/preferences", "PREFERENCE_UPDATE"),

            // 단어
            Map.entry("POST /api/words/collect", "WORD_COLLECT"),
            Map.entry("GET /api/words/my-collection", "VIEW_WORDS"),

            // 퀴즈
            Map.entry("POST /api/quiz/sessions/generate/sections", "QUIZ_START"),
            Map.entry("POST /api/quiz/sessions/generate/matching", "QUIZ_MATCHING_START"),
            Map.entry("POST /api/quiz/submit", "QUIZ_ANSWER"),
            Map.entry("GET /api/quiz/sessions/history", "VIEW_QUIZ_HISTORY"),

            // 영상
            Map.entry("GET /api/videos/recommended", "VIEW_RECOMMENDED"),

            // 채팅
            Map.entry("POST /api/chats/rooms", "CHAT_START"),
            Map.entry("GET /api/chats/scenarios", "VIEW_SCENARIOS"),
            Map.entry("POST /api/chats/reports", "CHAT_REPORT"),

            // 대시보드
            Map.entry("GET /api/users/me/dashboard", "VIEW_DASHBOARD"),
            Map.entry("GET /api/users/me/growth", "VIEW_GROWTH"),

            // 피드백
            Map.entry("POST /api/feedback", "FEEDBACK_SUBMIT")
    );

    // 동적 경로 (URL에 ID 포함)
    private static final List<PatternEntry> PATTERN_MAP = List.of(
            new PatternEntry(Pattern.compile("^POST /api/quiz/sessions/\\d+/complete$"), "QUIZ_COMPLETE"),
            new PatternEntry(Pattern.compile("^PATCH /api/chats/rooms/\\d+/complete$"), "CHAT_COMPLETE"),
            new PatternEntry(Pattern.compile("^GET /api/badges/video/.+$"), "VIEW_BADGE"),
            new PatternEntry(Pattern.compile("^GET /api/chats/rooms/\\d+/messages$"), "VIEW_CHAT_HISTORY"),
            new PatternEntry(Pattern.compile("^GET /api/chats/rooms/\\d+/report$"), "VIEW_CHAT_REPORT")
    );

    public Optional<String> resolveEventName(String method, String path) {
        String key = method + " " + path;

        // 1. 정확 매칭
        if (EVENT_MAP.containsKey(key)) {
            return Optional.of(EVENT_MAP.get(key));
        }

        // 2. 패턴 매칭
        for (PatternEntry entry : PATTERN_MAP) {
            if (entry.pattern.matcher(key).matches()) {
                return Optional.of(entry.eventName);
            }
        }

        return Optional.empty();
    }

    private record PatternEntry(Pattern pattern, String eventName) {}
}
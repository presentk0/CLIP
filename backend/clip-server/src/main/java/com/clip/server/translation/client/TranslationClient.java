package com.clip.server.translation.client;

import java.util.List;

public interface TranslationClient {
    List<String> translateBatch(List<String> texts);
}

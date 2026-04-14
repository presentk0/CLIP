package com.clip.server.word.dto.response;

import com.clip.server.common.response.PaginationResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WordListResponse {

    private List<WordResponse> words;
    private PaginationResponse pagination;
}

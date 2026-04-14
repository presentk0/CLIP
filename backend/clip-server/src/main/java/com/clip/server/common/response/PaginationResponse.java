package com.clip.server.common.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaginationResponse {

    private Long totalCount;
    private int currentPage;
    private int totalPage;
    private int pageSize;
}

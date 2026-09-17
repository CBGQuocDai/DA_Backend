package com.backend.domain.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PageResponse <T> {
    private List<T> content;
    private Integer total;
    private Integer totalPage;
    private Integer page;
    private Integer pageSize;
}

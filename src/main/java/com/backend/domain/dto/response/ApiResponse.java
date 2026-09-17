package com.backend.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class ApiResponse<T> {
    @Builder.Default
    private Integer code=2000;
    @Builder.Default
    private String message="thành công";
    private T data;
}

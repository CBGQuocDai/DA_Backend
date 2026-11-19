package com.backend.domain.model;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListenProgress {
    private Long id;
    private Long customerId;
    private Long bookId;
    private Long chapterId;
    private Integer lastPlayAt;
    private Boolean isComplete;
    private Instant updatedAt;
}

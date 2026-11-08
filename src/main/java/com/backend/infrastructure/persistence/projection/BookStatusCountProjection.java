package com.backend.infrastructure.persistence.projection;

import com.backend.domain.valueobject.BookStatus;

public interface BookStatusCountProjection {
    BookStatus getStatus();
    Long getTotal();
}

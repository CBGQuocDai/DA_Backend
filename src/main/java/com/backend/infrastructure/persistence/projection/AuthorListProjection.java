package com.backend.infrastructure.persistence.projection;

public interface AuthorListProjection {
    Long getId();
    String getFullName();
    String getAvatar();
    String getBio();
    Long getBookCount();
}

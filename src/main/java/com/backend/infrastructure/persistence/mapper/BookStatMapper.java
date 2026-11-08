package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.Author;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.BookStat;
import com.backend.domain.model.Category;
import com.backend.domain.model.Voice;
import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import com.backend.infrastructure.persistence.projection.BookListProjection;
import com.backend.infrastructure.persistence.projection.BookSalesProjection;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class BookStatMapper {

    private BookStatMapper() {}

    public static BookStat toBookStat(
            BookListProjection proj,
            List<JpaBookAuthorEntity> authorEntities,
            List<JpaBookCategoryEntity> categoryEntities) {

        BookStat bookStat = BookStat.builder()
                .id(proj.getId())
                .title(proj.getTitle())
                .price(proj.getPrice())
                .description(proj.getDescription())
                .coverImage(proj.getCoverImage())
                .contentFile(proj.getContentFile())
                .status(proj.getStatus())
                .averageRating(proj.getAverageRating())
                .build();

        if (Objects.nonNull(proj.getVoiceId())) {
            Voice voice = Voice.builder()
                    .id(proj.getVoiceId())
                    .name(proj.getVoiceName())
                    .exampleAudio(proj.getVoiceExampleAudio())
                    .description(proj.getVoiceDescription())
                    .build();
            bookStat.setVoice(voice);
        }

        if (Objects.nonNull(authorEntities) && !authorEntities.isEmpty()) {
            Map<Long, List<JpaBookAuthorEntity>> byBook = authorEntities.stream()
                    .collect(Collectors.groupingBy(JpaBookAuthorEntity::getBookId));

            List<JpaBookAuthorEntity> bookAuthors = byBook.getOrDefault(proj.getId(), List.of());
            bookStat.setAuthors(bookAuthors.stream()
                    .map(e -> {
                        Author author = Author.builder()
                                .id(e.getAuthorId())
                                .fullName(Objects.nonNull(e.getAuthor()) ? e.getAuthor().getFullName() : null)
                                .avatar(Objects.nonNull(e.getAuthor()) ? e.getAuthor().getAvatar() : null)
                                .bio(Objects.nonNull(e.getAuthor()) ? e.getAuthor().getBio() : null)
                                .build();
                        return BookAuthor.builder()
                                .id(e.getId())
                                .author(author)
                                .build();
                    })
                    .toList());
        }

        if (Objects.nonNull(categoryEntities) && !categoryEntities.isEmpty()) {
            Map<Long, List<JpaBookCategoryEntity>> byBook = categoryEntities.stream()
                    .collect(Collectors.groupingBy(JpaBookCategoryEntity::getBookId));

            List<JpaBookCategoryEntity> bookCategories = byBook.getOrDefault(proj.getId(), List.of());
            bookStat.setCategories(bookCategories.stream()
                    .map(e -> {
                        Category category = Category.builder()
                                .id(e.getCategoryId())
                                .name(Objects.nonNull(e.getCategory()) ? e.getCategory().getName() : null)
                                .description(Objects.nonNull(e.getCategory()) ? e.getCategory().getDescription() : null)
                                .build();
                        return BookCategory.builder()
                                .id(e.getId())
                                .category(category)
                                .build();
                    })
                    .toList());
        }

        return bookStat;
    }

    /** A bestseller row; {@code previousPurchases} is dropped when there is no earlier window to compare with. */
    public static BookStat toBookStat(BookSalesProjection row, boolean hasPrevious) {
        return BookStat.builder()
                .id(row.getId())
                .title(row.getTitle())
                .price(row.getPrice())
                .coverImage(row.getCoverImage())
                .purchases(row.getPurchases())
                .previousPurchases(hasPrevious ? row.getPreviousPurchases() : null)
                .build();
    }
}

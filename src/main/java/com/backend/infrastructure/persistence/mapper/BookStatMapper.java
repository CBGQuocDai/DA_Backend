package com.backend.infrastructure.persistence.mapper;

import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.BookStat;
import com.backend.domain.model.Voice;
import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import com.backend.infrastructure.persistence.projection.BookListProjection;
import com.backend.infrastructure.persistence.projection.BookSalesProjection;
import com.backend.infrastructure.persistence.repository.JpaBookAuthorRepository;
import com.backend.infrastructure.persistence.repository.JpaBookCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookStatMapper {

    private final JpaBookAuthorRepository jpaBookAuthorRepository;
    private final JpaBookCategoryRepository jpaBookCategoryRepository;
    private final BookAuthorMapper bookAuthorMapper;
    private final BookCategoryMapper bookCategoryMapper;

    /**
     * Projection -> BookStat kèm tác giả/thể loại: nạp theo lô cho cả trang (mỗi loại 1 truy vấn, JOIN FETCH),
     * convert entity -> domain và gom theo bookId một lần, mỗi sách chỉ tra map O(1).
     */
    public List<BookStat> toBookStats(List<BookListProjection> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = rows.stream().map(BookListProjection::getId).toList();

        Map<Long, List<BookAuthor>> authorsByBook = jpaBookAuthorRepository.findByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(JpaBookAuthorEntity::getBookId,
                        Collectors.mapping(bookAuthorMapper::toDomain, Collectors.toList())));

        Map<Long, List<BookCategory>> categoriesByBook = jpaBookCategoryRepository.findByBookIdIn(bookIds).stream()
                .collect(Collectors.groupingBy(JpaBookCategoryEntity::getBookId,
                        Collectors.mapping(bookCategoryMapper::toDomainWithoutBook, Collectors.toList())));

        return rows.stream()
                .map(row -> toBookStat(row,
                        authorsByBook.getOrDefault(row.getId(), List.of()),
                        categoriesByBook.getOrDefault(row.getId(), List.of())))
                .toList();
    }

    /** {@code authors}/{@code categories} là của riêng cuốn sách này. */
    public BookStat toBookStat(
            BookListProjection proj,
            List<BookAuthor> authors,
            List<BookCategory> categories) {

        BookStat bookStat = BookStat.builder()
                .id(proj.getId())
                .title(proj.getTitle())
                .price(proj.getPrice())
                .description(proj.getDescription())
                .coverImage(proj.getCoverImage())
                .contentFile(proj.getContentFile())
                .status(proj.getStatus())
                .averageRating(proj.getAverageRating())
                .ratingCount(proj.getRatingCount())
                .authors(authors)
                .categories(categories)
                .build();

        if (Objects.nonNull(proj.getVoiceId())) {
            bookStat.setVoice(Voice.builder()
                    .id(proj.getVoiceId())
                    .name(proj.getVoiceName())
                    .exampleAudio(proj.getVoiceExampleAudio())
                    .description(proj.getVoiceDescription())
                    .build());
        }
        return bookStat;
    }

    /** A bestseller row; {@code previousPurchases} is dropped when there is no earlier window to compare with. */
    public BookStat toBookStat(BookSalesProjection row, boolean hasPrevious) {
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

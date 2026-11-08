package com.backend.application.dto.response.book;

import com.backend.application.dto.response.file.FileResponse;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.dto.response.voice.VoiceResponse;
import com.backend.domain.valueobject.BookStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {
    private Long id;
    private String title;
    private BigDecimal price;
    private String description;
    private FileResponse coverImage;
    private FileResponse contentFile;
    private VoiceResponse voice;
    private BookStatus status;
    private List<BookCategoryResponse> categories;
    private List<BookAuthorResponse> authors;
    private List<ChapterResponse> chapters;
    private Double averageRating;
    private Long ratingCount;
    /** Bestseller lists only: purchases in the chosen period, paid or free. */
    private Long purchases;
    /** Bestseller lists only: purchases in the period before; absent when there is none to compare with. */
    private Long previousPurchases;
    /** Bestseller lists only: paid in the chosen period. */
    private BigDecimal revenue;
}

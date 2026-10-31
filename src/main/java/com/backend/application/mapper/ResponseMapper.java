package com.backend.application.mapper;

import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.book.BookAuthorResponse;
import com.backend.application.dto.response.book.BookCategoryResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.dto.response.file.FileResponse;
import com.backend.application.dto.response.payment.PaymentResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import com.backend.application.dto.response.voice.VoiceResponse;
import com.backend.application.adapter.storage.StorageService;
import com.backend.domain.model.Author;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.BookStat;
import com.backend.domain.model.Category;
import com.backend.domain.model.Chapter;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Payment;
import com.backend.domain.model.Review;
import com.backend.domain.model.Voice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/** Turns domain models into the responses use cases hand to the outside world. */
@Component
@RequiredArgsConstructor
public class ResponseMapper {

    private static final long SIGNED_URL_TTL_MINUTES = 60L;

    private final StorageService storageService;

    public <T, R> PageResponse<R> toPageResponse(PageResponse<T> page, Function<T, R> mapper) {
        return PageResponse.<R>builder()
                .content(mapList(page.getContent(), mapper))
                .total(page.getTotal())
                .totalPage(page.getTotalPage())
                .page(page.getPage())
                .pageSize(page.getPageSize())
                .build();
    }

    public FileResponse toFileResponse(String path) {
        if (path == null) {
            return null;
        }
        return FileResponse.builder().path(path).url(signUrl(path)).build();
    }

    public String signUrl(String path) {
        return storageService.createSignUrl(path, SIGNED_URL_TTL_MINUTES);
    }

    public BookResponse toBookResponse(Book book) {
        BookResponse.BookResponseBuilder response = BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .price(book.getPrice())
                .description(book.getDescription())
                .coverImage(toFileResponse(book.getCoverImage()))
                .contentFile(toFileResponse(book.getContentFile()))
                .voice(book.getVoice() == null ? null : toVoiceResponse(book.getVoice()))
                .status(book.getStatus())
                .categories(mapList(book.getCategories(), this::toBookCategoryResponse))
                .authors(mapList(book.getAuthors(), this::toBookAuthorResponse))
                .chapters(mapList(book.getChapters(), this::toChapterResponse));
        if (book instanceof BookStat stat) {
            response.averageRating(stat.getAverageRating()).ratingCount(stat.getRatingCount());
        }
        return response.build();
    }

    public VoiceResponse toVoiceResponse(Voice voice) {
        return VoiceResponse.builder()
                .id(voice.getId())
                .name(voice.getName())
                .description(voice.getDescription())
                .exampleAudio(toFileResponse(voice.getExampleAudio()))
                .build();
    }

    public ChapterResponse toChapterResponse(Chapter chapter) {
        return ChapterResponse.builder()
                .id(chapter.getId())
                .title(chapter.getTitle())
                .chapterOrder(chapter.getChapterOrder())
                .duration(chapter.getDuration())
                .audioUrl(chapter.getAudioUrl())
                .rawText(chapter.getRawText())
                .readingSpeed(chapter.getReadingSpeed())
                .status(chapter.getStatus())
                .build();
    }

    public AuthorResponse toAuthorResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .fullName(author.getFullName())
                .avatar(author.getAvatar())
                .bio(author.getBio())
                .build();
    }

    public CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    public ReviewResponse toReviewResponse(Review review) {
        Customer customer = review.getCustomer();
        Long customerId = customer == null ? null : customer.getId();
        String customerName = customer == null ? null : customer.getFullName();
        String customerAvatar = customer == null ? null : customer.getAvatarUrl();
        return ReviewResponse.builder()
                .id(review.getId())
                .bookId(review.getBook() == null ? null : review.getBook().getId())
                .rate(review.getRate())
                .rating(review.getRate())
                .content(review.getContent())
                .comment(review.getContent())
                .createAt(review.getCreateAt())
                .createdAt(review.getCreateAt())
                .customerId(customerId)
                .userId(customerId)
                .customerName(customerName)
                .userName(customerName)
                .customerAvatar(customerAvatar)
                .userAvatar(customerAvatar)
                .build();
    }

    public PaymentResponse toPaymentResponse(Payment payment, Book book) {
        String cover = book.getCoverImage();
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentId(payment.getId())
                .paymentCode(payment.getPaymentCode())
                .orderCode(payment.getPaymentCode())
                .customerId(payment.getCustomerId())
                .bookId(payment.getBookId())
                .bookTitle(book.getTitle())
                .bookCover(cover == null || cover.isBlank() ? cover : signUrl(cover))
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentStatus(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private BookAuthorResponse toBookAuthorResponse(BookAuthor bookAuthor) {
        return BookAuthorResponse.builder()
                .id(bookAuthor.getId())
                .author(bookAuthor.getAuthor() == null ? null : toAuthorResponse(bookAuthor.getAuthor()))
                .build();
    }

    private BookCategoryResponse toBookCategoryResponse(BookCategory bookCategory) {
        return BookCategoryResponse.builder()
                .id(bookCategory.getId())
                .category(bookCategory.getCategory() == null ? null : toCategoryResponse(bookCategory.getCategory()))
                .build();
    }

    private static <T, R> List<R> mapList(List<T> items, Function<T, R> mapper) {
        return items == null ? null : items.stream().map(mapper).toList();
    }
}

package com.backend.application.mapper;

import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.book.BookAuthorResponse;
import com.backend.application.dto.response.book.BookCategoryResponse;
import com.backend.application.dto.response.book.BookResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.dto.response.chapter.CustomerChapterResponse;
import com.backend.application.dto.response.file.FileResponse;
import com.backend.application.dto.response.payment.PaymentResponse;
import com.backend.application.dto.response.review.ReviewResponse;
import com.backend.application.dto.response.voice.VoiceResponse;
import com.backend.application.adapter.content.ChapterAttachment;
import com.backend.application.adapter.content.ChapterContentParser;
import com.backend.application.dto.response.chapter.ChapterAttachmentResponse;
import com.backend.application.adapter.storage.StorageService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Author;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.BookStat;
import com.backend.domain.model.Category;
import com.backend.domain.model.CategoryStat;
import com.backend.domain.model.Chapter;
import com.backend.domain.model.Customer;
import com.backend.domain.model.Payment;
import com.backend.domain.model.Review;
import com.backend.domain.model.Voice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Turns domain models into the responses use cases hand to the outside world. */
@Component
@RequiredArgsConstructor
public class ResponseMapper {

    private static final long SIGNED_URL_TTL_MINUTES = 60L;

    private final StorageService storageService;
    private final ChapterContentParser chapterContentParser;

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

    public CustomerChapterResponse toCustomerChapter(Chapter chapter) {
        String audio = chapter.getAudioUrl();
        return CustomerChapterResponse.builder()
                .id(chapter.getId()).title(chapter.getTitle()).chapterOrder(chapter.getChapterOrder())
                .duration(chapter.getDuration()).status(chapter.getStatus())
                .audioUrl(audio == null || audio.isBlank() ? null : signUrl(audio))
                .attachments(toAttachmentResponses(chapter.getContentJson()))
                .build();
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
            response.averageRating(stat.getAverageRating()).ratingCount(stat.getRatingCount())
                    .purchases(stat.getPurchases()).previousPurchases(stat.getPreviousPurchases());
        }
        return response.build();
    }

    public VoiceResponse toVoiceResponse(Voice voice) {
        return VoiceResponse.builder()
                .id(voice.getId())
                .name(voice.getName())
                .description(voice.getDescription())
                .exampleAudio(toFileResponse(voice.getExampleAudio()))
                .exampleTranscript(voice.getExampleTranscript())
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
                .contentJson(signImages(chapter.getContentJson()))
                .readingSpeed(chapter.getReadingSpeed())
                .status(chapter.getStatus())
                .build();
    }

    private String signImages(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return contentJson;
        }
        try {
            return chapterContentParser.withImageUrls(contentJson, this::signUrl);
        } catch (BusinessException e) {
            // A broken stored document must not make the whole book unreadable; the editor falls back to rawText.
            return contentJson;
        }
    }

    /** Tệp đính kèm của chương cho người nghe: ảnh kèm URL đã ký, bảng kèm dữ liệu, đánh số riêng theo loại. */
    public List<ChapterAttachmentResponse> toAttachmentResponses(String contentJson) {
        List<ChapterAttachmentResponse> responses = new ArrayList<>();
        int images = 0;
        int tables = 0;
        for (ChapterAttachment attachment : chapterContentParser.attachments(contentJson)) {
            boolean isImage = attachment.type() == ChapterAttachment.Type.IMAGE;
            responses.add(ChapterAttachmentResponse.builder()
                    .type(attachment.type())
                    .label(isImage ? "Hình " + ++images : "Bảng " + ++tables)
                    .imageUrl(isImage ? signUrl(attachment.imagePath()) : null)
                    .rows(isImage ? null : attachment.rows())
                    .headerRows(isImage ? null : attachment.headerRows())
                    .build());
        }
        return responses;
    }

    public AuthorResponse toAuthorResponse(Author author) {
        return AuthorResponse.builder()
                .id(author.getId())
                .fullName(author.getFullName())
                .avatar(toFileResponse(author.getAvatar()))
                .bio(author.getBio())
                .build();
    }

    public CategoryResponse toCategoryResponse(Category category) {
        CategoryResponse.CategoryResponseBuilder response = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription());
        if (category instanceof CategoryStat stat) {
            response.purchases(stat.getPurchases()).previousPurchases(stat.getPreviousPurchases());
        }
        return response.build();
    }

    public ReviewResponse toReviewResponse(Review review) {
        Customer customer = review.getCustomer();
        Long customerId = customer == null ? null : customer.getId();
        String customerName = customer == null ? null : customer.getFullName();
        if (customerName == null || customerName.isBlank()) customerName = "Bạn đọc";
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
                .verifiedPurchase(review.isVerifiedPurchase())
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

package com.backend;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.service.BookService;
import com.backend.application.service.FileManagementService;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.ChapterStatus;
import com.backend.presentation.controller.BookController;
import com.backend.presentation.controller.ChapterController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real H2 queries and service transactions, plus the customer and legacy HTTP contracts. */
@SpringBootTest
@ActiveProfiles("test")
class BookControllerIntegrationTest {
    @MockitoBean OcrService ocrService;
    @MockitoBean StorageService storageService;
    @MockitoBean FileManagementService fileManagementService;
    @MockitoBean MessageQueue messageQueue;

    @Autowired BookRepository bookRepository;
    @Autowired ChapterRepository chapterRepository;
    @Autowired BookService bookService;
    @Autowired WebApplicationContext context;
    @Autowired BookController adminBooks;
    @Autowired ChapterController adminChapters;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(storageService.createSignUrl(anyString(), anyLong())).thenAnswer(invocation ->
                "https://audio.example/" + invocation.getArgument(0));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void defaultsReturnTenOrderedChaptersWithoutEditorContent() throws Exception {
        Book book = newBook(BookStatus.PUBLISHED);
        seedChapters(book, 23);

        mvc.perform(get("/books/{id}/chapters/page", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.content[0].title").value("Chương 1"))
                .andExpect(jsonPath("$.data.content[9].chapterOrder").value(10))
                .andExpect(jsonPath("$.data.content[0].audioUrl").value("https://audio.example/audio/1.mp3"))
                .andExpect(jsonPath("$.data.content[0].rawText").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].contentJson").doesNotExist())
                .andExpect(jsonPath("$.data.total").value(23))
                .andExpect(jsonPath("$.data.totalPage").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10));
    }

    @Test
    void pageBoundariesDoNotOverlapAndLastPageHasThreeChapters() throws Exception {
        Book book = newBook(BookStatus.PUBLISHED);
        seedChapters(book, 23);
        var first = bookService.getCustomerChapters(book.getId(), 0, 10);
        var second = bookService.getCustomerChapters(book.getId(), 1, 10);
        assertTrue(first.getContent().stream().noneMatch(a -> second.getContent().stream()
                .anyMatch(b -> a.getId().equals(b.getId()))));
        assertEquals(11, second.getContent().getFirst().getChapterOrder());

        mvc.perform(get("/books/{id}/chapters/page", book.getId()).param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].chapterOrder").value(21))
                .andExpect(jsonPath("$.data.content[2].chapterOrder").value(23))
                .andExpect(jsonPath("$.data.total").value(23));
    }

    @Test
    void duplicateOrdersUseIdAsAStableTieBreaker() {
        Book book = newBook(BookStatus.PUBLISHED);
        var stored = chapterRepository.saveAll(List.of(
                Chapter.builder().title("A").chapterOrder(0).build(),
                Chapter.builder().title("B").chapterOrder(0).build()), book);
        assertEquals(stored.get(0).getId(), bookService.getCustomerChapters(book.getId(), 0, 1).getContent().getFirst().getId());
        assertEquals(stored.get(1).getId(), bookService.getCustomerChapters(book.getId(), 1, 1).getContent().getFirst().getId());
    }

    @Test
    void overviewReportsWholeBookTotalsAndFirstChapterWithoutLoadingFullChapters() throws Exception {
        Book book = newBook(BookStatus.PUBLISHED);
        seedChapters(book, 23);
        mvc.perform(get("/books/{id}/overview", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapterCount").value(23))
                .andExpect(jsonPath("$.data.totalDuration").value(1380))
                .andExpect(jsonPath("$.data.firstChapter.chapterOrder").value(1))
                .andExpect(jsonPath("$.data.firstChapter.rawText").doesNotExist())
                .andExpect(jsonPath("$.data.book.chapters").doesNotExist());
        assertNull(bookRepository.findDetailWithoutChapters(book.getId()).orElseThrow().getChapters());
    }

    @Test
    void emptyBookAndPagePastTheEndHaveEmptyLists() throws Exception {
        Book empty = newBook(BookStatus.PUBLISHED);
        mvc.perform(get("/books/{id}/overview", empty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapterCount").value(0))
                .andExpect(jsonPath("$.data.totalDuration").value(0))
                .andExpect(jsonPath("$.data.firstChapter").doesNotExist());
        mvc.perform(get("/books/{id}/chapters/page", empty.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalPage").value(0));

        seedChapters(empty, 1);
        mvc.perform(get("/books/{id}/chapters/page", empty.getId()).param("page", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void durationsIgnoreNullAndOtherBooks() {
        Book book = newBook(BookStatus.PUBLISHED);
        chapterRepository.saveAll(List.of(
                Chapter.builder().title("A").chapterOrder(0).duration(null).build(),
                Chapter.builder().title("B").chapterOrder(1).duration(45).build()), book);
        seedChapters(newBook(BookStatus.PUBLISHED), 23);
        assertEquals(45, bookService.getCustomerBookOverview(book.getId()).getTotalDuration());
        assertEquals(2, bookService.getCustomerBookOverview(book.getId()).getChapterCount());
    }

    @Test
    void invalidPaginationReturnsBadRequest() throws Exception {
        Book book = newBook(BookStatus.PUBLISHED);
        for (String[] params : List.of(new String[]{"-1", "10"}, new String[]{"0", "0"},
                new String[]{"0", "101"}, new String[]{"2147483647", "10"}, new String[]{"abc", "10"})) {
            mvc.perform(get("/books/{id}/chapters/page", book.getId())
                            .param("page", params[0]).param("size", params[1]))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingAndUnpublishedBooksAreUnavailableToCustomers() throws Exception {
        Book draft = newBook(BookStatus.UNPUBLISHED);
        for (Long id : List.of(-1L, draft.getId())) {
            for (String path : List.of("/overview", "/chapters/page")) {
                mvc.perform(get("/books/" + id + path))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(4006));
            }
        }
    }

    @Test
    void legacyAdminReadsStillReturnEveryChapterAndEditorContent() throws Exception {
        Book book = newBook(BookStatus.UNPUBLISHED);
        seedChapters(book, 23);
        mvc.perform(get("/books/{id}/chapters", book.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(23))
                .andExpect(jsonPath("$[0].rawText").value("Nội dung 1"));
        assertEquals(23, bookRepository.findById(book.getId()).orElseThrow().getChapters().size());
    }

    @Test
    void adminBookAndChapterReadsRequireTheAdminRole() throws Exception {
        Book book = newBook(BookStatus.PUBLISHED);
        seedChapters(book, 1);
        for (String path : List.of("/books", "/books/" + book.getId(),
                "/books/" + book.getId() + "/chapters", "/books/categories", "/books/category/7")) {
            mvc.perform(get(path).param("page", "0").param("size", "10"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get(path).param("page", "0").param("size", "10")
                            .with(user("customer").roles("CUSTOMER")))
                    .andExpect(status().isForbidden());
            mvc.perform(get(path).param("page", "0").param("size", "10")
                            .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void preAuthorizeAlsoProtectsCallsOutsideTheHttpFilter() {
        assertThrows(AccessDeniedException.class, () -> adminBooks.getBookById(-1L));
        assertThrows(AccessDeniedException.class, () -> adminChapters.getChapters(-1L));
    }

    @Test
    void publicCatalogOnlyReturnsPublishedBooksEvenWhenCallerRequestsDrafts() throws Exception {
        String prefix = "catalog-" + System.nanoTime();
        Book published = newBook(BookStatus.PUBLISHED, prefix + " công khai");
        newBook(BookStatus.UNPUBLISHED, prefix + " nháp");
        mvc.perform(get("/books/catalog").param("bookName", prefix)
                        .param("status", "UNPUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(published.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"));
        mvc.perform(get("/books/catalog").param("categoryId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void customerCatalogWhitelistDoesNotExposeOwnershipEndpoints() throws Exception {
        mvc.perform(get("/customer/books")).andExpect(status().isUnauthorized());
        mvc.perform(get("/customer/books/7/check")).andExpect(status().isUnauthorized());
    }

    private Book newBook(BookStatus status) {
        return newBook(status, "Sách phân trang");
    }

    private Book newBook(BookStatus status, String title) {
        Book book = new Book();
        book.setTitle(title);
        book.setPrice(BigDecimal.TEN);
        book.setStatus(status);
        return bookRepository.insert(book);
    }

    private void seedChapters(Book book, int count) {
        List<Chapter> chapters = new ArrayList<>();
        // Insert backwards to ensure SQL orders the records before slicing pages.
        for (int i = count; i >= 1; i--) {
            chapters.add(Chapter.builder().title("Chương " + i).chapterOrder(i).duration(60)
                    .audioUrl("audio/" + i + ".mp3").rawText("Nội dung " + i)
                    .contentJson("{\"type\":\"doc\"}").status(ChapterStatus.AUDIO_READY).build());
        }
        chapterRepository.saveAll(chapters, book);
    }
}

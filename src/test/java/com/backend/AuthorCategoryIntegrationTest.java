package com.backend;

import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.ocr.OcrService;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.service.AuthorService;
import com.backend.application.service.CategoryService;
import com.backend.application.service.FileManagementService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Author;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.domain.model.Category;
import com.backend.domain.valueobject.BookStatus;
import com.backend.domain.valueobject.BusinessError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The search queries and the "in use" guards, against the real persistence adapters on H2. */
@SpringBootTest
@ActiveProfiles("test")
class AuthorCategoryIntegrationTest {

    @MockitoBean
    OcrService ocrService;
    @MockitoBean
    StorageService storageService;
    @MockitoBean
    FileManagementService fileManagementService;
    @MockitoBean
    MessageQueue messageQueue;

    @Autowired
    AuthorService authorService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    BookRepository bookRepository;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private AuthorResponse author(String name) {
        return authorService.createAuthor(AuthorRequest.builder().fullName(name).build());
    }

    @Test
    void authorSearchIsCaseInsensitiveAndTreatsWildcardsLiterally() {
        String tag = unique();
        author("Nam_Cao " + tag);
        author("NamXCao " + tag);
        author("Giảm 100% " + tag);

        assertEquals(3, authorService.searchAuthors(tag.toUpperCase(), 0, 10).getTotal());
        assertEquals(1, authorService.searchAuthors("nam_cao " + tag, 0, 10).getTotal());
        assertEquals(1, authorService.searchAuthors("100% " + tag, 0, 10).getTotal());
    }

    @Test
    void authorSearchPagesNewestFirst() {
        String tag = unique();
        author("Một " + tag);
        AuthorResponse newest = author("Hai " + tag);

        var page = authorService.searchAuthors(tag, 0, 1);

        assertEquals(2, page.getTotal());
        assertEquals(2, page.getTotalPage());
        assertEquals(newest.getId(), page.getContent().get(0).getId());
    }

    @Test
    void categorySearchMatchesDescriptionToo() {
        String tag = unique();
        categoryService.createCategory(CategoryRequest.builder().name("Tên " + tag).build());
        categoryService.createCategory(CategoryRequest.builder().name("Khác " + unique()).description("về " + tag).build());

        assertEquals(2, categoryService.searchCategories(tag, 0, 10).getTotal());
    }

    @Test
    void categoryNamesAreUniqueIgnoringCase() {
        String name = "Kinh dị " + unique();
        CategoryResponse created = categoryService.createCategory(CategoryRequest.builder().name(name).build());

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.createCategory(
                CategoryRequest.builder().name(name.toUpperCase()).build()));
        assertEquals(BusinessError.CATEGORY_EXISTED, ex.getError());
        // Saving a category under its own name is not a duplicate
        assertDoesNotThrow(() -> categoryService.updateCategory(created.getId(),
                CategoryRequest.builder().name(name).description("Mới").build()));
    }

    @Test
    void authorsAndCategoriesInABookCannotBeDeleted() {
        AuthorResponse usedAuthor = author("Đang dùng " + unique());
        CategoryResponse usedCategory = categoryService.createCategory(
                CategoryRequest.builder().name("Đang dùng " + unique()).build());
        Book book = new Book();
        book.setTitle("Sách");
        book.setPrice(BigDecimal.ONE);
        book.setDescription("Mô tả");
        book.setStatus(BookStatus.PUBLISHED);
        book.setAuthors(List.of(BookAuthor.builder().author(Author.builder().id(usedAuthor.getId()).build()).build()));
        book.setCategories(List.of(BookCategory.builder().category(Category.builder().id(usedCategory.getId()).build()).build()));
        bookRepository.insert(book);

        assertEquals(BusinessError.AUTHOR_IN_USE, assertThrows(BusinessException.class,
                () -> authorService.deleteAuthor(usedAuthor.getId())).getError());
        assertEquals(BusinessError.CATEGORY_IN_USE, assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(usedCategory.getId())).getError());

        AuthorResponse freeAuthor = author("Rảnh " + unique());
        authorService.deleteAuthor(freeAuthor.getId());
        assertEquals(BusinessError.AUTHOR_NOT_FOUND, assertThrows(BusinessException.class,
                () -> authorService.getAuthor(freeAuthor.getId())).getError());
    }
}

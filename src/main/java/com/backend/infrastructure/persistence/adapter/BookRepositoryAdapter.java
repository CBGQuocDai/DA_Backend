package com.backend.infrastructure.persistence.adapter;

import com.backend.domain.adapter.repository.BookRepository;
import com.backend.domain.dto.response.PageResponse;
import com.backend.domain.mapper.BookMapper;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookAuthor;
import com.backend.domain.model.BookCategory;
import com.backend.infrastructure.persistence.entity.JpaBookAuthorEntity;
import com.backend.infrastructure.persistence.entity.JpaBookCategoryEntity;
import com.backend.infrastructure.persistence.entity.JpaBookEntity;
import com.backend.infrastructure.persistence.repository.JpaBookAuthorRepository;
import com.backend.infrastructure.persistence.repository.JpaBookCategoryRepository;
import com.backend.infrastructure.persistence.repository.JpaBookRepository;
import com.backend.infrastructure.persistence.repository.JpaBookUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class BookRepositoryAdapter implements BookRepository {

    private final JpaBookRepository jpaBookRepository;
    private final JpaBookUpdateRepository jpaBookUpdateRepository;
    private final JpaBookAuthorRepository jpaBookAuthorRepository;
    private final JpaBookCategoryRepository jpaBookCategoryRepository;
    private final BookMapper bookMapper;

    @Override
    @Transactional
    public Book insert(Book book) {
        JpaBookEntity entity = JpaBookEntity.builder()
                .title(book.getTitle())
                .price(book.getPrice())
                .description(book.getDescription())
                .coverImage(book.getCoverImage())
                .contentFile(book.getContentFile())
                .voiceId(book.getVoice() != null ? book.getVoice().getId() : null)
                .status(book.getStatus())
                .build();

        JpaBookEntity savedEntity = jpaBookRepository.save(entity);

        if (book.getBookAuthors() != null) {
            for (BookAuthor bookAuthor : book.getBookAuthors()) {
                JpaBookAuthorEntity authorEntity = JpaBookAuthorEntity.builder()
                        .bookId(savedEntity.getId())
                        .authorId(bookAuthor.getAuthor() != null ? bookAuthor.getAuthor().getId() : null)
                        .build();
                jpaBookAuthorRepository.save(authorEntity);
            }
        }

        if (book.getBookCategories() != null) {
            for (BookCategory bookCategory : book.getBookCategories()) {
                JpaBookCategoryEntity categoryEntity = JpaBookCategoryEntity.builder()
                        .bookId(savedEntity.getId())
                        .categoryId(bookCategory.getCategory() != null ? bookCategory.getCategory().getId() : null)
                        .build();
                jpaBookCategoryRepository.save(categoryEntity);
            }
        }

        book.setId(savedEntity.getId());
        return book;
    }

    @Override
    @Transactional
    public void update(Book book) {
        jpaBookUpdateRepository.updateBook(
                book.getId(),
                book.getTitle(),
                book.getPrice(),
                book.getDescription(),
                book.getCoverImage(),
                book.getContentFile(),
                book.getVoice() != null ? book.getVoice().getId() : null,
                book.getStatus()
        );

        if (book.getBookAuthors() != null) {
            jpaBookAuthorRepository.deleteByBookId(book.getId());
            for (BookAuthor bookAuthor : book.getBookAuthors()) {
                JpaBookAuthorEntity authorEntity = JpaBookAuthorEntity.builder()
                        .bookId(book.getId())
                        .authorId(bookAuthor.getAuthor() != null ? bookAuthor.getAuthor().getId() : null)
                        .build();
                jpaBookAuthorRepository.save(authorEntity);
            }
        }

        if (book.getBookCategories() != null) {
            jpaBookCategoryRepository.deleteByBookId(book.getId());
            for (BookCategory bookCategory : book.getBookCategories()) {
                JpaBookCategoryEntity categoryEntity = JpaBookCategoryEntity.builder()
                        .bookId(book.getId())
                        .categoryId(bookCategory.getCategory() != null ? bookCategory.getCategory().getId() : null)
                        .build();
                jpaBookCategoryRepository.save(categoryEntity);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Book> findById(Long id) {
        return jpaBookRepository.findById(id)
                .map(bookMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Book> findAll(int page, int size) {
        Page<JpaBookEntity> entityPage = jpaBookRepository.findAll(PageRequest.of(page, size));
        List<Book> books = entityPage.getContent().stream()
                .map(bookMapper::toDomain)
                .toList();

        return PageResponse.<Book>builder()
                .content(books)
                .total((int) entityPage.getTotalElements())
                .totalPage(entityPage.getTotalPages())
                .page(page)
                .pageSize(size)
                .build();
    }

    @Override
    @Transactional
    public void delete(Book book) {
        if (book.getId() != null) {
            jpaBookRepository.deleteById(book.getId());
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaBookRepository.deleteById(id);
    }
}

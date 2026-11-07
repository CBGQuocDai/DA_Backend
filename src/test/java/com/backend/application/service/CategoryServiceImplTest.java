package com.backend.application.service;

import com.backend.application.adapter.repository.CategoryRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.category.CategoryRequest;
import com.backend.application.dto.response.category.CategoryResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.impl.CategoryServiceImpl;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Category;
import com.backend.domain.valueobject.BusinessError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, new ResponseMapper(mock(StorageService.class)));
    }

    @Test
    void createTrimsAndBlanksDescription() {
        when(categoryRepository.existsByName(" Trinh thám ", null)).thenReturn(false);
        when(categoryRepository.save(any())).then(returnsFirstArg());

        CategoryResponse response = categoryService.createCategory(
                CategoryRequest.builder().name(" Trinh thám ").description("  ").build());

        assertEquals("Trinh thám", response.getName());
        assertNull(response.getDescription());
    }

    @Test
    void duplicateNameIsRefused() {
        when(categoryRepository.existsByName("Văn học", null)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.createCategory(
                CategoryRequest.builder().name("Văn học").build()));

        assertEquals(BusinessError.CATEGORY_EXISTED, ex.getError());
    }

    @Test
    void renameChecksDuplicatesExcludingItself() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).name("Cũ").build()));
        when(categoryRepository.existsByName("Mới", 2L)).thenReturn(false);
        when(categoryRepository.save(any())).then(returnsFirstArg());

        assertEquals("Mới", categoryService.updateCategory(2L, CategoryRequest.builder().name("Mới").build()).getName());
    }

    @Test
    void deleteIsRefusedWhileABookUsesTheCategory() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(Category.builder().id(2L).build()));
        when(categoryRepository.isUsedByAnyBook(2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> categoryService.deleteCategory(2L));

        assertEquals(BusinessError.CATEGORY_IN_USE, ex.getError());
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}

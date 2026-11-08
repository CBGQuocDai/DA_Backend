package com.backend.application.service;

import com.backend.application.adapter.repository.AuthorRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.author.AuthorRequest;
import com.backend.application.dto.response.author.AuthorResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.impl.AuthorServiceImpl;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Author;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    private static final String TEMP_AVATAR = "tmp/images/moi.png";

    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private FileManagementService fileManagementService;
    @Mock
    private StorageService storageService;

    private AuthorServiceImpl authorService;

    @BeforeEach
    void setUp() {
        authorService = new AuthorServiceImpl(authorRepository, fileManagementService, storageService,
                new ResponseMapper(storageService));
    }

    private static AuthorRequest request(String avatar) {
        return AuthorRequest.builder().fullName("  Nam Cao ").bio(" ").avatar(avatar).build();
    }

    private void givenExisting(String avatar) {
        Author existing = Author.builder().id(5L).fullName("Cũ").avatar(avatar).build();
        when(authorRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(authorRepository.save(any())).then(returnsFirstArg());
    }

    @Test
    void createWithoutAvatarTouchesNoFile() {
        when(authorRepository.save(any())).then(returnsFirstArg());

        AuthorResponse response = authorService.createAuthor(request(null));

        assertEquals("Nam Cao", response.getFullName());
        assertNull(response.getAvatar());
        assertNull(response.getBio());
        verifyNoInteractions(fileManagementService);
    }

    @Test
    void createWithAvatarVerifiesThenMovesToPermanentPath() {
        when(authorRepository.save(any())).then(returnsFirstArg());
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("signed");

        authorService.createAuthor(request(TEMP_AVATAR));

        ArgumentCaptor<Author> saved = ArgumentCaptor.forClass(Author.class);
        verify(authorRepository).save(saved.capture());
        assertEquals("images/moi.png", saved.getValue().getAvatar());
        verify(fileManagementService).verifyUpload(TEMP_AVATAR, UploadType.IMAGE);
        verify(fileManagementService).moveToPermanent(TEMP_AVATAR, UploadType.IMAGE);
    }

    @Test
    void updateKeepingAvatarDeletesNothing() throws Exception {
        givenExisting("images/cu.png");
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("signed");

        authorService.updateAuthor(5L, request("images/cu.png"));

        verifyNoInteractions(fileManagementService);
        verify(storageService, never()).delete(anyString());
    }

    @Test
    void updateWithNewAvatarReplacesAndDeletesOld() throws Exception {
        givenExisting("images/cu.png");
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("signed");

        AuthorResponse response = authorService.updateAuthor(5L, request(TEMP_AVATAR));

        assertEquals("images/moi.png", response.getAvatar().getPath());
        verify(fileManagementService).moveToPermanent(TEMP_AVATAR, UploadType.IMAGE);
        verify(storageService).delete("images/cu.png");
    }

    @Test
    void updateWithBlankAvatarRemovesIt() throws Exception {
        givenExisting("images/cu.png");

        AuthorResponse response = authorService.updateAuthor(5L, request(""));

        assertNull(response.getAvatar());
        verify(storageService).delete("images/cu.png");
    }

    @Test
    void deleteIsRefusedWhileABookUsesTheAuthor() {
        when(authorRepository.findById(5L)).thenReturn(Optional.of(Author.builder().id(5L).build()));
        when(authorRepository.isUsedByAnyBook(5L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authorService.deleteAuthor(5L));

        assertEquals(BusinessError.AUTHOR_IN_USE, ex.getError());
        verify(authorRepository, never()).deleteById(anyLong());
    }

    @Test
    void unknownAuthorIsNotFound() {
        when(authorRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authorService.getAuthor(9L));

        assertEquals(BusinessError.AUTHOR_NOT_FOUND, ex.getError());
    }
}

package com.backend.application.service;

import com.backend.application.service.impl.FileManagementServiceImpl;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.upload.UploadUrlRequest;
import com.backend.application.dto.response.upload.UploadUrlResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import com.backend.application.adapter.storage.SignedUpload;
import com.backend.infrastructure.util.TikaFileDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Runs against the real Tika detector: which bytes it recognizes is the point of the check. */
@ExtendWith(MockitoExtension.class)
class FileManagementServiceImplTest {

    private static final byte[] PDF = ascii("%PDF-1.7\n%âãÏÓ\n");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0};
    private static final byte[] WEBP = ascii("RIFF$\0\0\0WEBPVP8 ");
    private static final byte[] WAV = ascii("RIFF$\0\0\0WAVEfmt ");
    private static final byte[] MP3_ID3 = ascii("ID3\u0003\0\0\0\0\0\0");
    private static final byte[] MP3_FRAME = {(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x44, 0, 0, 0, 0};

    @Mock
    private StorageService storageService;

    private FileManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileManagementServiceImpl(storageService, new TikaFileDetector());
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static UploadUrlRequest request(UploadType type, String fileName, long size) {
        return UploadUrlRequest.builder().type(type).fileName(fileName).size(size).build();
    }

    private void assertSignRejected(BusinessError expected, UploadUrlRequest request) {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createUploadUrl(request));
        assertEquals(expected, ex.getError());
        verifyNoInteractions(storageService);
    }

    private void givenUpload(String path, byte[] head) {
        when(storageService.findSize(path)).thenReturn(Optional.of(1024L));
        when(storageService.loadHead(eq(path), anyInt())).thenReturn(head);
    }

    private void assertVerifyRejected(BusinessError expected, String path, UploadType type) {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.verifyUpload(path, type));
        assertEquals(expected, ex.getError());
    }

    @Test
    void createUploadUrl_signsAFreshTempPathForTheTypeWithItsContentTypeAndSizeCap() {
        when(storageService.createUploadSignUrl(anyString(), eq("image/png"), eq(UploadType.IMAGE.getMaxSize()), anyLong()))
                .thenReturn(new SignedUpload("https://signed", Map.of("Content-Type", "image/png")));

        UploadUrlResponse response = service.createUploadUrl(request(UploadType.IMAGE, "Bìa.PNG", 1024));

        assertEquals("https://signed", response.getUploadUrl());
        assertTrue(response.getFilePath().startsWith("tmp/images/"));
        assertTrue(response.getFilePath().endsWith(".png"));
        assertEquals("image/png", response.getHeaders().get("Content-Type"));
    }

    @Test
    void createUploadUrl_signsEachExtensionWithItsOwnContentType() {
        Map<String, String> expected = Map.of("a.jpg", "image/jpeg", "a.jpeg", "image/jpeg", "a.webp", "image/webp");
        expected.forEach((fileName, contentType) -> {
            when(storageService.createUploadSignUrl(anyString(), eq(contentType), anyLong(), anyLong()))
                    .thenReturn(new SignedUpload("https://signed", Map.of("Content-Type", contentType)));

            assertEquals(contentType,
                    service.createUploadUrl(request(UploadType.IMAGE, fileName, 1024)).getHeaders().get("Content-Type"));
        });
        when(storageService.createUploadSignUrl(anyString(), eq("audio/mpeg"), anyLong(), anyLong()))
                .thenReturn(new SignedUpload("https://signed", Map.of("Content-Type", "audio/mpeg")));
        when(storageService.createUploadSignUrl(anyString(), eq("audio/wav"), anyLong(), anyLong()))
                .thenReturn(new SignedUpload("https://signed", Map.of("Content-Type", "audio/wav")));

        assertEquals("audio/mpeg",
                service.createUploadUrl(request(UploadType.AUDIO, "a.mp3", 1024)).getHeaders().get("Content-Type"));
        assertEquals("audio/wav",
                service.createUploadUrl(request(UploadType.AUDIO, "a.wav", 1024)).getHeaders().get("Content-Type"));
    }

    @Test
    void createUploadUrl_rejectsExtensionNotAllowedForTheType() {
        assertSignRejected(BusinessError.PDF_FORMAT_INCORRECT, request(UploadType.PDF, "anh.png", 1024));
    }

    @Test
    void createUploadUrl_rejectsFileNameWithoutExtension() {
        assertSignRejected(BusinessError.IMAGE_FORMAT_INCORRECT, request(UploadType.IMAGE, "bia", 1024));
    }

    @Test
    void createUploadUrl_rejectsFileOverTheSizeCap() {
        assertSignRejected(BusinessError.AUDIO_TOO_LARGE, request(
                UploadType.AUDIO, "mau.wav", UploadType.AUDIO.getMaxSize() + 1));
    }

    @Test
    void verifyUpload_acceptsEveryAllowedFormatAndKeepsTheFile() throws IOException {
        Map<String, byte[]> images = Map.of(
                "tmp/images/a.jpg", JPEG, "tmp/images/a.jpeg", JPEG, "tmp/images/a.png", PNG, "tmp/images/a.webp", WEBP);
        Map<String, byte[]> audios = Map.of(
                "tmp/audio/a.wav", WAV, "tmp/audio/id3.mp3", MP3_ID3, "tmp/audio/frame.mp3", MP3_FRAME);

        images.forEach((path, head) -> {
            givenUpload(path, head);
            service.verifyUpload(path, UploadType.IMAGE);
        });
        audios.forEach((path, head) -> {
            givenUpload(path, head);
            service.verifyUpload(path, UploadType.AUDIO);
        });
        givenUpload("tmp/pdfs/a.pdf", PDF);
        service.verifyUpload("tmp/pdfs/a.pdf", UploadType.PDF);

        verify(storageService, never()).delete(anyString());
    }

    @Test
    void verifyUpload_rejectsPathOutsideTheTempFolderOfTheType() {
        assertVerifyRejected(BusinessError.UPLOAD_NOT_FOUND, "pdfs/cua-sach-khac.pdf", UploadType.PDF);
        assertVerifyRejected(BusinessError.UPLOAD_NOT_FOUND, "tmp/images/a.png", UploadType.PDF);

        verifyNoInteractions(storageService);
    }

    @Test
    void verifyUpload_rejectsPathWithNoFile() throws IOException {
        when(storageService.findSize("tmp/pdfs/a.pdf")).thenReturn(Optional.empty());

        assertVerifyRejected(BusinessError.UPLOAD_NOT_FOUND, "tmp/pdfs/a.pdf", UploadType.PDF);

        verify(storageService, never()).delete(anyString());
    }

    @Test
    void verifyUpload_rejectsAndDeletesFileOverTheSizeCap() throws IOException {
        when(storageService.findSize("tmp/images/a.png")).thenReturn(Optional.of(UploadType.IMAGE.getMaxSize() + 1));

        assertVerifyRejected(BusinessError.IMAGE_TOO_LARGE, "tmp/images/a.png", UploadType.IMAGE);

        verify(storageService).delete("tmp/images/a.png");
    }

    @Test
    void verifyUpload_rejectsAndDeletesRenamedFile() throws IOException {
        givenUpload("tmp/pdfs/a.pdf", ascii("khong phai pdf"));

        assertVerifyRejected(BusinessError.PDF_FORMAT_INCORRECT, "tmp/pdfs/a.pdf", UploadType.PDF);

        verify(storageService).delete("tmp/pdfs/a.pdf");
    }

    @Test
    void verifyUpload_rejectsExtensionThatDoesNotMatchTheBytes() throws IOException {
        givenUpload("tmp/images/a.jpg", PNG);

        assertVerifyRejected(BusinessError.IMAGE_FORMAT_INCORRECT, "tmp/images/a.jpg", UploadType.IMAGE);

        verify(storageService).delete("tmp/images/a.jpg");
    }

    @Test
    void verifyUpload_stillReportsTheFormatErrorWhenDeleteFails() throws IOException {
        givenUpload("tmp/audio/a.mp3", new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        doThrow(new IOException("gcs")).when(storageService).delete("tmp/audio/a.mp3");

        assertVerifyRejected(BusinessError.AUDIO_FORMAT_INCORRECT, "tmp/audio/a.mp3", UploadType.AUDIO);
    }

    @Test
    void moveToPermanent_dropsTheTempPrefix() {
        service.moveToPermanent("tmp/pdfs/a.pdf", UploadType.PDF);

        verify(storageService).move("tmp/pdfs/a.pdf", "pdfs/a.pdf");
    }
}

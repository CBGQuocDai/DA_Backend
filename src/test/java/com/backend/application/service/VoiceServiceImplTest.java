package com.backend.application.service;

import com.backend.application.dto.response.voice.VoiceResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.service.impl.VoiceServiceImpl;
import com.backend.application.adapter.audio.AudioConverter;
import com.backend.application.adapter.repository.VoiceRepository;
import com.backend.application.adapter.storage.StorageService;
import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Voice;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.UploadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceServiceImplTest {

    private static final String TEMP_WAV = "tmp/audio/moi.wav";
    private static final String TEMP_MP3 = "tmp/audio/moi.mp3";

    @Mock
    private VoiceRepository voiceRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private FileManagementService fileManagementService;
    @Mock
    private AudioConverter audioConverter;

    private VoiceServiceImpl voiceService;

    @BeforeEach
    void setUp() {
        voiceService = new VoiceServiceImpl(voiceRepository, storageService, fileManagementService,
                new ResponseMapper(storageService), audioConverter);
    }

    private static VoiceRequest request(String exampleAudio) {
        return VoiceRequest.builder().name("Giọng").description("Mô tả").exampleAudio(exampleAudio).build();
    }

    private void givenExistingVoice() {
        Voice existing = new Voice();
        existing.setExampleAudio("audio/cu.wav");
        when(voiceRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(voiceRepository.update(any())).then(returnsFirstArg());
        when(storageService.createSignUrl(anyString(), anyLong())).thenReturn("https://signed");
    }

    @Test
    void createVoice_verifiesBeforeSavingThenMovesTheWav() {
        when(voiceRepository.save(any())).then(returnsFirstArg());

        VoiceResponse created = voiceService.createVoice(request(TEMP_WAV));

        assertEquals("audio/moi.wav", created.getExampleAudio().getPath());
        InOrder order = inOrder(fileManagementService, voiceRepository);
        order.verify(fileManagementService).verifyUpload(TEMP_WAV, UploadType.AUDIO);
        order.verify(voiceRepository).save(any());
        order.verify(fileManagementService).moveToPermanent(TEMP_WAV, UploadType.AUDIO);
    }

    @Test
    void createVoice_savesNothingWhenTheFileIsRejected() {
        doThrow(new BusinessException(BusinessError.AUDIO_FORMAT_INCORRECT))
                .when(fileManagementService).verifyUpload(TEMP_WAV, UploadType.AUDIO);

        assertThrows(BusinessException.class, () -> voiceService.createVoice(request(TEMP_WAV)));

        verify(voiceRepository, never()).save(any());
    }

    @Test
    void createVoice_failsWhenAnMp3CannotBeConvertedSoTheSaveRollsBack() {
        when(voiceRepository.save(any())).then(returnsFirstArg());
        when(storageService.load(TEMP_MP3)).thenReturn(new byte[]{1, 2, 3, 4});
        when(audioConverter.mp3ToWav(new byte[]{1, 2, 3, 4})).thenThrow(new BusinessException(BusinessError.FILE_FORMAT_INCORRECT));

        BusinessException ex = assertThrows(BusinessException.class, () -> voiceService.createVoice(request(TEMP_MP3)));

        assertEquals(BusinessError.FILE_FORMAT_INCORRECT, ex.getError());
        ArgumentCaptor<Voice> saved = ArgumentCaptor.forClass(Voice.class);
        verify(voiceRepository).save(saved.capture());
        assertTrue(saved.getValue().getExampleAudio().matches("audio/.+\\.wav"));
        verify(storageService, never()).store(any(), anyString(), anyString());
    }

    @Test
    void updateVoice_keepsTheSampleWhenItsPathIsUnchanged() throws IOException {
        givenExistingVoice();

        voiceService.updateVoice(3L, request("audio/cu.wav"));

        verifyNoInteractions(fileManagementService);
        verify(storageService, never()).delete(anyString());
        verify(storageService).createSignUrl("audio/cu.wav", 60L);
    }

    @Test
    void updateVoice_replacesTheSampleAndDeletesTheOldOne() throws IOException {
        givenExistingVoice();

        voiceService.updateVoice(3L, request(TEMP_WAV));

        verify(fileManagementService).verifyUpload(TEMP_WAV, UploadType.AUDIO);
        verify(fileManagementService).moveToPermanent(TEMP_WAV, UploadType.AUDIO);
        verify(storageService).delete("audio/cu.wav");
        verify(storageService).createSignUrl("audio/moi.wav", 60L);
    }

    @Test
    void updateVoice_changesNothingWhenTheNewSampleIsRejected() throws IOException {
        Voice existing = new Voice();
        existing.setExampleAudio("audio/cu.wav");
        when(voiceRepository.findById(3L)).thenReturn(Optional.of(existing));
        doThrow(new BusinessException(BusinessError.UPLOAD_NOT_FOUND))
                .when(fileManagementService).verifyUpload("audio/cua-giong-khac.wav", UploadType.AUDIO);

        assertThrows(BusinessException.class,
                () -> voiceService.updateVoice(3L, request("audio/cua-giong-khac.wav")));

        verify(voiceRepository, never()).update(any());
        verify(storageService, never()).delete(anyString());
    }
}

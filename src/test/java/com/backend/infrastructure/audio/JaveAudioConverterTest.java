package com.backend.infrastructure.audio;

import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JaveAudioConverterTest {

    @Test
    void mp3ToWav_reportsAFormatErrorForBytesThatAreNotAudio() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> new JaveAudioConverter().mp3ToWav(new byte[]{1, 2, 3, 4}));

        assertEquals(BusinessError.FILE_FORMAT_INCORRECT, ex.getError());
    }
}

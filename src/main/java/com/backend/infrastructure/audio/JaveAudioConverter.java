package com.backend.infrastructure.audio;

import com.backend.application.adapter.audio.AudioConverter;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class JaveAudioConverter implements AudioConverter {

    private static final String MP3_TYPE = "mp3";
    private static final String WAV_TYPE = "wav";

    @Override
    public byte[] mp3ToWav(byte[] mp3) {
        Path source = null;
        Path target = null;
        try {
            source = Files.createTempFile("voice-src-", "." + MP3_TYPE);
            target = Files.createTempFile("voice-out-", "." + WAV_TYPE);
            Files.write(source, mp3);

            MultimediaObject sourceObj = new MultimediaObject(source.toFile());
            MultimediaInfo info = sourceObj.getInfo();
            int channels = (info.getAudio() != null && info.getAudio().getChannels() > 0)
                    ? info.getAudio().getChannels()
                    : 1;
            int sampleRate = (info.getAudio() != null && info.getAudio().getSamplingRate() > 0)
                    ? info.getAudio().getSamplingRate()
                    : 44100;

            AudioAttributes audioAttrs = new AudioAttributes();
            audioAttrs.setCodec("pcm_s16le");
            audioAttrs.setChannels(channels);
            audioAttrs.setSamplingRate(sampleRate);
            audioAttrs.setBitRate(256_000);

            EncodingAttributes encodingAttrs = new EncodingAttributes();
            encodingAttrs.setOutputFormat(WAV_TYPE);
            encodingAttrs.setAudioAttributes(audioAttrs);

            new Encoder().encode(sourceObj, target.toFile(), encodingAttrs);

            return Files.readAllBytes(target);
        } catch (EncoderException | IOException e) {
            log.error("Failed to convert mp3 to wav", e);
            throw new BusinessException(BusinessError.FILE_FORMAT_INCORRECT);
        } finally {
            deleteIfExists(source);
            deleteIfExists(target);
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }
}

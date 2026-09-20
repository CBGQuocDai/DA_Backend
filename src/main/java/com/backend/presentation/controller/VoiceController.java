package com.backend.presentation.controller;


import com.backend.application.VoiceService;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.model.Voice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;
    @GetMapping
    public ResponseEntity<ApiResponse<com.backend.domain.dto.response.PageResponse<Voice>>> getListVoice() {
        return ResponseEntity.ok(ApiResponse.<com.backend.domain.dto.response.PageResponse<Voice>>builder()
                .data(voiceService.getListVoice())
                .build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Voice>> createVoice(
            @RequestPart("voiceInfo") String voiceInfo,
            @RequestPart("exampleAudio") MultipartFile exampleAudio) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<Voice>builder()
                .data(voiceService.createVoice(voiceInfo, exampleAudio))
                .build());
    }

    @PutMapping("/{voiceId}")
    public ResponseEntity<ApiResponse<Voice>> updateVoice(@PathVariable Long voiceId, @RequestBody Voice voice) {
        return ResponseEntity.ok(ApiResponse.<Voice>builder()
                .data(voiceService.updateVoice(voiceId, voice))
                .build());
    }

    @DeleteMapping("/{voiceId}")
    public ResponseEntity<ApiResponse<Void>> deleteVoice(@PathVariable Long voiceId) {
        voiceService.deleteVoice(voiceId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

}

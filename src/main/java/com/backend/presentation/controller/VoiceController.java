package com.backend.presentation.controller;


import com.backend.application.service.VoiceService;
import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.dto.response.PageResponse;
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
    public ResponseEntity<ApiResponse<PageResponse<Voice>>> getListVoice(
            @RequestParam(value="page", defaultValue = "0")  int page,
            @RequestParam(value= "size", defaultValue = "10") int size,
            @RequestParam(value = "voiceName", required = false) String voiceName
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<Voice>>builder()
                .data(voiceService.getListVoice(voiceName, page, size))
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

    @PutMapping(value = "/{voiceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Voice>> updateVoice(
            @PathVariable Long voiceId,
            @RequestPart(value = "voiceInfo", required = false) String voiceInfo,
            @RequestPart(value = "exampleAudio", required = false) MultipartFile exampleAudio) {
        return ResponseEntity.ok(ApiResponse.<Voice>builder()
                .data(voiceService.updateVoice(voiceId, voiceInfo, exampleAudio))
                .build());
    }

    @DeleteMapping("/{voiceId}")
    public ResponseEntity<ApiResponse<Void>> deleteVoice(@PathVariable Long voiceId) {
        voiceService.deleteVoice(voiceId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}

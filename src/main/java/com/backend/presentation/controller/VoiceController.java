package com.backend.presentation.controller;


import com.backend.application.service.VoiceService;
import com.backend.application.dto.request.voice.VoiceRequest;
import com.backend.presentation.dto.ApiResponse;
import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.voice.VoiceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VoiceResponse>>> getListVoice(
            @RequestParam(value="page", defaultValue = "0")  int page,
            @RequestParam(value= "size", defaultValue = "10") int size,
            @RequestParam(value = "voiceName", required = false) String voiceName
    ) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<VoiceResponse>>builder()
                .data(voiceService.getListVoice(voiceName, page, size))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoiceResponse>> createVoice(
            @Valid @RequestBody VoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<VoiceResponse>builder()
                .data(voiceService.createVoice(request))
                .build());
    }

    @PutMapping("/{voiceId}")
    public ResponseEntity<ApiResponse<VoiceResponse>> updateVoice(
            @PathVariable Long voiceId,
            @Valid @RequestBody VoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.<VoiceResponse>builder()
                .data(voiceService.updateVoice(voiceId, request))
                .build());
    }

    @DeleteMapping("/{voiceId}")
    public ResponseEntity<ApiResponse<Void>> deleteVoice(@PathVariable Long voiceId) {
        voiceService.deleteVoice(voiceId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}

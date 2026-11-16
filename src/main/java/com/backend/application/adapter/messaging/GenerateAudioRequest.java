package com.backend.application.adapter.messaging;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAudioRequest {
    private Long chapterId;
    /** Mã của lần tạo audio này; worker trả lại trong kết quả và dùng để nhận lệnh hủy. */
    private String jobId;
    private String exampleVoiceUrl;
    /** Lời thoại của giọng mẫu; null thì worker để thư viện omnivoice tự nhận dạng. */
    private String exampleVoiceText;
    private String text;
    private Double speed;
}

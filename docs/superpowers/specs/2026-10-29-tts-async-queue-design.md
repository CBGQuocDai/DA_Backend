# Generate audio bất đồng bộ qua RabbitMQ (phía backend)

Ngày: 2026-10-29 · Branch: `admin`

## Mục tiêu

Gen audio cho chapter (OmniVoice TTS) mất nhiều thời gian, nên backend không gọi HTTP đồng bộ nữa.
- Backend gửi job vào RabbitMQ.
- Backend lắng nghe queue kết quả để cập nhật chapter.

**Hoàn thành khi:**
- `POST /books/{bookId}/chapters/{chapterId}/audio` trả về ngay, chapter chuyển sang `PROCESSING_AUDIO`, và đúng một job nằm trong `tts.jobs`.
- Một message kết quả trong `tts.results`:
  - thành công → chapter có `audioUrl` + `duration`, chuyển sang `AUDIO_READY`;
  - thất bại → chapter chuyển sang `FAILED`.

**Không thuộc phạm vi:**
- Worker OmniVoice: consume `tts.jobs`, gen audio, upload GCS, publish kết quả. Repo này chỉ định nghĩa định dạng message để worker làm theo.
- Gen audio cho cả sách.
- Thông báo realtime cho frontend. Frontend tự poll `GET /books/{bookId}/chapters`.
- Tự retry khi job lỗi.

## Quyết định đã chốt

| # | Quyết định |
|---|---|
| 1 | Worker OmniVoice tự consume từ RabbitMQ (không còn HTTP `/generate-audio`). |
| 2 | Hai queue một chiều trên exchange `tts`: `tts.jobs` (backend → worker), `tts.results` (worker → backend). |
| 3 | `ChapterStatus` = `FAILED`, `PROCESSING_AUDIO`, `AUDIO_READY`; `null` = chưa gen audio. DB đã được user cập nhật. |
| 4 | Gọi gen audio khi chapter đang `PROCESSING_AUDIO` → 409. `AUDIO_READY`/`FAILED`/`null` thì gen được. |
| 5 | Worker tự upload audio lên GCS và trả path (giữ như `OmniVoiceService` cũ). |
| 6 | Listener xử lý cả kết quả thành công (`AUDIO_READY`) lẫn thất bại (`FAILED`). |
| 7 | Metadata audio lưu trên `Chapter`: chỉ `duration` (giây) và `readingSpeed`. Không thêm field. |

### Vì sao chỉ lưu `duration` + `readingSpeed`

Chapter và audio quan hệ 1–1, nên thuộc tính của audio phụ thuộc trực tiếp vào `chapter.id`; `audio_url` là duy nhất theo chapter nên không có phụ thuộc bắc cầu. Lưu như vậy vẫn đạt 3NF/BCNF. Việc giá trị trong DB trùng với thông tin trong file là cache dữ liệu dẫn xuất, an toàn vì file không đổi sau khi gen: lần gen lại sẽ cập nhật path và metadata cùng lúc.

Chỉ lưu thứ backend dùng:
- `duration`: hiển thị trong danh sách chapter, tính tổng thời lượng sách, tính tiến độ nghe trong `ListenProgress`.
- `readingSpeed`: quyết định nghiệp vụ, không đọc lại được từ file.

Size, mime, bitrate để frontend tự lấy từ file hoặc header.

## Luồng

```
POST /books/{bookId}/chapters/{chapterId}/audio
  → ChapterService.generateAudio()                 [@Transactional]
      1. load book (BOOK_NOT_FOUND), chapter (CHAPTER_NOT_FOUND)
      2. chapterRepository.markProcessingAudio(chapterId, speed)
           = UPDATE ... SET status='PROCESSING_AUDIO', reading_speed=?
             WHERE id=? AND (status IS NULL OR status <> 'PROCESSING_AUDIO')
         0 dòng → 409 CHAPTER_AUDIO_PROCESSING
      3. messageQueue.send(job)   → exchange "tts" → queue "tts.jobs"
         send lỗi → exception → rollback, status không bị kẹt
  ← 200

[ngoài phạm vi] OmniVoice worker: consume tts.jobs → gen → upload GCS → publish tts.results → ack

Backend @RabbitListener(queues = "tts.results")   [infrastructure]
  → ChapterService.handleAudioResult(result)       [@Transactional]
      chapter không tồn tại           → log warn, bỏ qua
      status != PROCESSING_AUDIO      → log warn, bỏ qua
      success → audioUrl = audioPath, duration, status = AUDIO_READY
      fail    → status = FAILED, log error (giữ audioUrl/duration cũ)
```

Update có điều kiện ở bước 2 là atomic, nên 2 request đồng thời không thể cùng gửi job.

## Định dạng message

Content type `application/json`, UTF-8, message persistent.

**Job** — backend publish, exchange `tts`, routing key `tts.jobs`:
```json
{ "chapterId": 12, "text": "...", "exampleVoiceUrl": "voice-example/abc.wav", "speed": 1.0 }
```
- `exampleVoiceUrl`: path GCS của sample voice (`Voice.exampleAudio`); rỗng nếu sách không có voice.
- `speed`: 0.5–2.0, mặc định 1.0.

**Kết quả** — worker publish, exchange `tts`, routing key `tts.results`:
```json
{ "chapterId": 12, "success": true, "audioPath": "books/3/audio/12.wav", "duration": 314, "error": null }
```
- `success=false` → `audioPath`/`duration` null, `error` là mô tả lỗi.
- `duration`: số nguyên, giây.

**Yêu cầu với worker** (để tham khảo khi sửa OmniVoice, không làm trong repo này):
- Ack job sau khi đã publish kết quả.
- Mọi lỗi khi gen/upload phải được trả về bằng một message `success=false`.
- Worker không được khai báo `tts.jobs`/`tts.results` với tham số khác backend. Hoặc dùng passive declaration, hoặc khai báo đúng như backend: durable, và `tts.results` có `x-dead-letter-exchange=tts.dlx`. Nếu không RabbitMQ trả về PRECONDITION_FAILED và backend không gửi được.

## Thành phần

| Layer | File | Thay đổi |
|---|---|---|
| domain | `adapter/messageQueue/MessageQueue.java` | `void send(GenerateAudioRequest job)` |
| domain | `dto/request/book/chapter/GenerateAudioRequest.java` | thêm `chapterId` |
| domain | `dto/request/book/chapter/GenerateAudioResult.java` (mới) | `chapterId, success, audioPath, duration, error` |
| domain | `adapter/repository/ChapterRepository.java` | `boolean markProcessingAudio(Long id, Double speed)` |
| domain | `valueobject/BusinessError.java` | `CHAPTER_AUDIO_PROCESSING` (409) |
| domain | `model/Chapter.java` | ghi chú `duration` = giây |
| application | `ChapterService` / `ChapterServiceImpl` | `generateAudio` qua queue; thêm `handleAudioResult(GenerateAudioResult)` |
| infrastructure | `messageQueue/RabbitMqConfig.java` | exchange `tts`; queue durable `tts.jobs`, `tts.results`; DLX `tts.dlx` + `tts.results.dlq`; JSON converter (Jackson 3, khớp `tools.jackson` đang dùng) |
| infrastructure | `messageQueue/RabbitMQPublisher.java` | implement `MessageQueue` bằng `RabbitTemplate` |
| infrastructure | `messageQueue/AudioResultListener.java` (mới) | `@RabbitListener(queues = "tts.results")` → `ChapterService.handleAudioResult` |
| infrastructure | persistence adapter của `ChapterRepository` + `JpaChapterUpdateRepository` | query update có điều kiện |
| infrastructure | `ocr/OcrServiceImpl.java` | bỏ `.status(TEXT_READY)` (chapter mới = `null`) |
| config | `pom.xml` | `spring-boot-starter-amqp` |
| config | `application.yaml`, `.env.example` | `spring.rabbitmq.*` từ `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`; `listener.simple.default-requeue-rejected: false` |

Listener nằm ở infrastructure và gọi thẳng `ChapterService` (như controller), nên không cần port cho chiều nhận.

## Xử lý lỗi

- **Gửi job lỗi** (RabbitMQ down): exception làm rollback, chapter giữ status cũ, API trả 500.
- **Message kết quả không parse được / listener ném exception:** không requeue (tránh lặp vô hạn); message đi qua DLX `tts.dlx` vào `tts.results.dlq` để xem trên RabbitMQ UI.
- **Chapter bị xóa khi đang gen, hoặc kết quả trùng/trễ** (chapter không còn `PROCESSING_AUDIO`): log warn rồi ack, không vào DLQ.
- **Kết quả không bao giờ tới** (worker mất message): chapter kẹt `PROCESSING_AUDIO`, phải sửa tay trong DB. Chấp nhận, không làm timeout.

## Test

- **Unit `ChapterServiceImpl`** (mock repo, `MessageQueue`):
  - Book / chapter không tồn tại → lỗi tương ứng, không gọi `send`.
  - `markProcessingAudio` trả false → 409, không gọi `send`.
  - Trả true → `send` được gọi với đúng `chapterId`, text, `exampleVoiceUrl`, speed (null → 1.0).
  - Kết quả success → `AUDIO_READY`, audioUrl, duration.
  - Kết quả fail → `FAILED`, giữ audio cũ.
  - Chapter không tồn tại / không `PROCESSING_AUDIO` → không update.
- **`mvn verify`** pass (profile test không cần RabbitMQ thật).
- **Thủ công:**
  - RabbitMQ bằng Docker.
  - Gọi API → chapter `PROCESSING_AUDIO`, thấy 1 message trong `tts.jobs`.
  - Gọi lại → 409.
  - Publish tay một kết quả thành công / thất bại vào `tts.results` → chapter `AUDIO_READY` / `FAILED`.
  - Publish JSON hỏng → vào `tts.results.dlq`.

# Generate audio bất đồng bộ qua RabbitMQ — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /books/{bookId}/chapters/{chapterId}/audio` gửi job vào RabbitMQ rồi trả về ngay; backend lắng nghe `tts.results` để cập nhật chapter.

**Architecture:**
- Domain port `MessageQueue` được implement bằng `RabbitTemplate` ở infrastructure.
- `ChapterServiceImpl.generateAudio` khóa chapter bằng một UPDATE có điều kiện rồi publish job.
- `AudioResultListener` (infrastructure) nhận kết quả và gọi `ChapterService.handleAudioResult`.
- Message hỏng → DLQ.

**Tech Stack:**
- Spring Boot 4.0.8, Spring AMQP (`spring-boot-starter-amqp`, version theo Boot BOM)
- Jackson 3 (`tools.jackson`), Spring Data JPA, Lombok
- JUnit 5 + Mockito

**Spec:** `docs/superpowers/specs/2026-10-29-tts-async-queue-design.md`

## Global Constraints

- Exchange `tts` (direct). Queue durable `tts.jobs` (routing key `tts.jobs`), `tts.results` (routing key `tts.results`).
- DLX `tts.dlx` (direct), queue `tts.results.dlq` bind với routing key `tts.results`.
- Job JSON: `{ "chapterId", "text", "exampleVoiceUrl", "speed" }`. Kết quả JSON: `{ "chapterId", "success", "audioPath", "duration", "error" }`. `duration` = số nguyên, giây.
- `ChapterStatus` = `FAILED`, `PROCESSING_AUDIO`, `AUDIO_READY`; `null` = chưa gen. Không thêm lại `TEXT_READY`.
- Lỗi mới: `CHAPTER_AUDIO_PROCESSING(4014, "Chương đang được tạo audio, vui lòng đợi", HttpStatus.CONFLICT)`.
- `speed` null → 1.0. Validation khoảng 0.5–2.0 đã nằm ở `ChapterGenerateAudioRequest`, không đổi.
- Không thêm field vào `Chapter`.
- Config RabbitMQ đặt trong `application-dev.yaml` (`application.yaml` chỉ chọn profile), biến môi trường `RABBITMQ_HOST` (mặc định `localhost`), `RABBITMQ_PORT` (`5672`), `RABBITMQ_USERNAME` (`guest`), `RABBITMQ_PASSWORD` (`guest`).
- Build bằng `mvn` (không dùng `./mvnw`, vì `.mvn/wrapper` không có trong repo).
- **Không commit:** working tree đang có nhiều thay đổi khác chưa commit của user; user tự commit.

## Review Focus

1. **Worker publish kết quả thiếu `content_type: application/json`:** converter không parse được, message phải vào `tts.results.dlq` chứ không lặp lại vô hạn. Kiểm tra bằng thủ công ở Task 3 (publish không có content type).
2. **Kết quả `success=true` nhưng `audioPath` rỗng/null:** không được set `AUDIO_READY` với `audioUrl` null; coi là thất bại → `FAILED`. Test ở Task 2.
3. **Kết quả thiếu `chapterId`:** phải vào DLQ (ném `IllegalArgumentException`), không bị bỏ qua lặng lẽ. Test ở Task 2.
4. **Hai lần gọi API liên tiếp cho cùng chapter:** lần 2 nhận 409 và không có job thứ 2. Test ở Task 1 (unit, `markProcessingAudio` trả false), và thủ công ở Task 3.
5. **Queue `tts.results` đã tồn tại trên broker mà không có tham số DLX** (do khai báo tay trước đó): RabbitMQ báo `PRECONDITION_FAILED` khi khởi động. Không sửa trong code; Task 3 dùng broker mới, và ghi chú lại cho user.

---

### Task 1: Gửi job generate audio qua RabbitMQ

Sau task này project compile lại được. Hiện đang lỗi vì `TextToSpeechService` đã bị xóa và `ChapterStatus.TEXT_READY` không còn.

**Files:**
- Modify: `pom.xml` — thêm `org.springframework.boot:spring-boot-starter-amqp` (không ghi version)
- Modify: `src/main/resources/application-dev.yaml` — `spring.rabbitmq.host/port/username/password` theo Global Constraints
- Modify: `src/main/resources/application-test.yaml` — `spring.rabbitmq.listener.simple.auto-startup: false`, `spring.rabbitmq.listener.direct.auto-startup: false`
- Modify: `.env.example` — thêm khối `# RabbitMQ` với 4 biến trên
- Modify: `src/main/java/com/backend/domain/adapter/messageQueue/MessageQueue.java`
- Modify: `src/main/java/com/backend/domain/dto/request/book/chapter/GenerateAudioRequest.java` — thêm `private Long chapterId;` (đứng đầu)
- Modify: `src/main/java/com/backend/domain/valueobject/BusinessError.java` — thêm `CHAPTER_AUDIO_PROCESSING` ngay dưới `CHAPTER_NOT_FOUND`
- Modify: `src/main/java/com/backend/domain/model/Chapter.java` — comment `// seconds` cạnh `duration`
- Modify: `src/main/java/com/backend/domain/adapter/repository/ChapterRepository.java`
- Modify: `src/main/java/com/backend/infrastructure/persistence/repository/JpaChapterUpdateRepository.java`
- Modify: `src/main/java/com/backend/infrastructure/persistence/adapter/ChapterRepositoryAdapter.java`
- Modify: `src/main/java/com/backend/infrastructure/messageQueue/RabbitMqConfig.java`
- Modify: `src/main/java/com/backend/infrastructure/messageQueue/RabbitMQ.java`
- Modify: `src/main/java/com/backend/infrastructure/ocr/OcrServiceImpl.java:221` — xóa dòng `.status(ChapterStatus.TEXT_READY)` (và import nếu thừa)
- Modify: `src/main/java/com/backend/application/service/impl/ChapterServiceImpl.java`
- Test: `src/test/java/com/backend/application/service/ChapterServiceImplTest.java` (mới)

**Interfaces:**
- Produces:
  - `MessageQueue#void send(GenerateAudioRequest job)`
  - `ChapterRepository#boolean markProcessingAudio(Long id, Double speed)`: true nếu đã chuyển sang `PROCESSING_AUDIO`, false nếu chapter đang `PROCESSING_AUDIO` hoặc không tồn tại.
  - `RabbitMqConfig` hằng `public static final String EXCHANGE = "tts"`, `JOB_QUEUE = "tts.jobs"`.
  - Bean `MessageConverter` dùng Jackson 3. Spring AMQP 4: `org.springframework.amqp.support.converter.JacksonJsonMessageConverter`; nếu tên khác trong jar thì dùng class Jackson 3 tương ứng, không dùng `Jackson2JsonMessageConverter`.
  - `BusinessError.CHAPTER_AUDIO_PROCESSING`

- [ ] **Step 1: Viết test `ChapterServiceImplTest`**

`@ExtendWith(MockitoExtension.class)`. `@Mock` `ChapterRepository`, `BookRepository`, `StorageService`, `MessageQueue`; `@InjectMocks ChapterServiceImpl`.

Fixture:
- `Book` id 1, voice `Voice(id 5, exampleAudio "voice-example/abc.wav")`.
- `Chapter` id 12, rawText "Nội dung".
- Request `ChapterGenerateAudioRequest(bookId 1, chapterId 12, speed …)`.

| Test | Arrange | Assert |
|---|---|---|
| `generateAudio_bookNotFound_throwsAndDoesNotSend` | `bookRepository.findById(1)` → empty | `BusinessException` với `BOOK_NOT_FOUND`; `verifyNoInteractions(messageQueue)` |
| `generateAudio_chapterNotFound_throwsAndDoesNotSend` | chapter → empty | `CHAPTER_NOT_FOUND`; không gọi `send` |
| `generateAudio_alreadyProcessing_throwsConflict` | `markProcessingAudio(12, 1.5)` → false | `CHAPTER_AUDIO_PROCESSING`; không gọi `send` |
| `generateAudio_sendsJob` | speed 1.5, mark → true | `ArgumentCaptor<GenerateAudioRequest>`: chapterId 12, text "Nội dung", exampleVoiceUrl "voice-example/abc.wav", speed 1.5 |
| `generateAudio_nullSpeed_defaultsToOne` | speed null, mark(12, 1.0) → true | `markProcessingAudio(12, 1.0)` được gọi; job speed 1.0 |
| `generateAudio_bookWithoutVoice_sendsEmptyVoiceUrl` | book.voice null | job exampleVoiceUrl `""` |

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `mvn -q test -Dtest=ChapterServiceImplTest`
Expected: COMPILATION ERROR (`markProcessingAudio`, `MessageQueue.send(GenerateAudioRequest)`, `CHAPTER_AUDIO_PROCESSING` chưa có).

- [ ] **Step 3: Domain**
  - `MessageQueue.send`, `GenerateAudioRequest.chapterId`, `BusinessError.CHAPTER_AUDIO_PROCESSING`.
  - Comment `duration` trong `Chapter`.
  - `ChapterRepository.markProcessingAudio`.

- [ ] **Step 4: Persistence**
  - `JpaChapterUpdateRepository`: thêm `@Modifying int markProcessingAudio(@Param("id") Long id, @Param("speed") Double speed, @Param("processing") ChapterStatus processing)`, với JPQL:
    ```
    UPDATE JpaChapterEntity c SET c.status = :processing, c.readingSpeed = :speed
    WHERE c.id = :id AND (c.status IS NULL OR c.status <> :processing)
    ```
  - `ChapterRepositoryAdapter.markProcessingAudio`: `@Transactional`, trả `jpa... (id, speed, ChapterStatus.PROCESSING_AUDIO) == 1`.

- [ ] **Step 5: Infrastructure**
  - `RabbitMqConfig` (`@Configuration`): `DirectExchange` `tts` (durable); `Queue` `tts.jobs` (durable); `Binding` jobs → exchange với key `tts.jobs`; bean `MessageConverter`. Boot tự gắn converter vào `RabbitTemplate` và listener factory.
  - `RabbitMQ.send`: `rabbitTemplate.convertAndSend(EXCHANGE, JOB_QUEUE, job)`. Log info chapterId.
  - Bỏ `@Service` → `@Component` (đây là adapter, không phải service).

- [ ] **Step 6: Service và OCR**
  - `ChapterServiceImpl`:
    - thay field `TextToSpeechService textToSpeechService` bằng `MessageQueue messageQueue`;
    - `generateAudio` thêm `@Transactional` và làm theo thứ tự: load book → load chapter → `speed = requireNonNullElse(req.getSpeed(), 1.0)` → `if (!markProcessingAudio(...)) throw CHAPTER_AUDIO_PROCESSING` → build `GenerateAudioRequest` (thêm chapterId) → `messageQueue.send(job)`;
    - xóa `chapter.setAudioUrl`/`chapterRepository.update` cũ.
  - `OcrServiceImpl`: xóa `.status(ChapterStatus.TEXT_READY)`.

- [ ] **Step 7: Chạy test, xác nhận pass**

Run: `mvn -q test -Dtest=ChapterServiceImplTest`
Expected: 6 test PASS.

- [ ] **Step 8: Chạy toàn bộ build**

Run: `mvn -q verify`
Expected: exit 0; `BackendApplicationTests.contextLoads` pass mà không cần RabbitMQ thật.

---

### Task 2: Nhận kết quả từ `tts.results`

**Files:**
- Create: `src/main/java/com/backend/domain/dto/request/book/chapter/GenerateAudioResult.java`
- Create: `src/main/java/com/backend/infrastructure/messageQueue/AudioResultListener.java`
- Modify: `src/main/java/com/backend/application/service/ChapterService.java`
- Modify: `src/main/java/com/backend/application/service/impl/ChapterServiceImpl.java`
- Modify: `src/main/java/com/backend/infrastructure/messageQueue/RabbitMqConfig.java`
- Modify: `src/main/resources/application-dev.yaml` — `spring.rabbitmq.listener.simple.default-requeue-rejected: false`
- Test: `src/test/java/com/backend/application/service/ChapterServiceImplTest.java`

**Interfaces:**
- Consumes: `RabbitMqConfig.EXCHANGE`, bean `MessageConverter` (Task 1); `ChapterRepository.findById`, `update` (đã có).
- Produces:
  - `GenerateAudioResult`: Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` giống `GenerateAudioRequest`, với field `Long chapterId`, `boolean success`, `String audioPath`, `Integer duration`, `String error`.
  - `ChapterService#void handleAudioResult(GenerateAudioResult result)`
  - `RabbitMqConfig` hằng `RESULT_QUEUE = "tts.results"`, `DEAD_LETTER_EXCHANGE = "tts.dlx"`, `RESULT_DLQ = "tts.results.dlq"`.

- [ ] **Step 1: Thêm test vào `ChapterServiceImplTest`**

Fixture: chapter 12 status `PROCESSING_AUDIO`, audioUrl cũ "books/1/audio/old.wav", duration 100.

| Test | Input | Assert |
|---|---|---|
| `handleAudioResult_success_marksReady` | success, audioPath "books/1/audio/12.wav", duration 314 | `update` được gọi với chapter: audioUrl "books/1/audio/12.wav", duration 314, status `AUDIO_READY` |
| `handleAudioResult_failure_marksFailedKeepsOldAudio` | success false, error "OOM" | status `FAILED`, audioUrl "books/1/audio/old.wav", duration 100 |
| `handleAudioResult_successWithoutPath_marksFailed` | success true, audioPath null | status `FAILED`, audioUrl giữ cũ |
| `handleAudioResult_chapterMissing_ignored` | `findById(12)` → empty | không gọi `update`, không ném exception |
| `handleAudioResult_notProcessing_ignored` | chapter status `AUDIO_READY` | không gọi `update` |
| `handleAudioResult_nullChapterId_throws` | chapterId null | `IllegalArgumentException`; không gọi `findById` |

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `mvn -q test -Dtest=ChapterServiceImplTest`
Expected: COMPILATION ERROR (`GenerateAudioResult`, `handleAudioResult` chưa có).

- [ ] **Step 3: `GenerateAudioResult`, `ChapterService.handleAudioResult`, `ChapterServiceImpl.handleAudioResult`**

`@Transactional`. Thứ tự xử lý:
1. chapterId null → `IllegalArgumentException`.
2. Không tìm thấy chapter → log warn, return.
3. Status khác `PROCESSING_AUDIO` → log warn, return.
4. `success && audioPath` không blank → set audioUrl, duration, `AUDIO_READY`.
5. Ngược lại → `FAILED` và log error chapterId + error.
6. `chapterRepository.update(chapter)`.

- [ ] **Step 4: Chạy test, xác nhận pass**

Run: `mvn -q test -Dtest=ChapterServiceImplTest`
Expected: 12 test PASS.

- [ ] **Step 5: Hạ tầng nhận**
  - `RabbitMqConfig`:
    - `DirectExchange` `tts.dlx`.
    - `Queue` `tts.results.dlq` (durable), bind với `tts.dlx` key `tts.results`.
    - `Queue` `tts.results` dùng `QueueBuilder.durable(RESULT_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build()`, bind với `tts` key `tts.results`.
  - `AudioResultListener` (`@Component`, `@RequiredArgsConstructor`): `@RabbitListener(queues = RabbitMqConfig.RESULT_QUEUE) void onResult(GenerateAudioResult result)` → `chapterService.handleAudioResult(result)`. Kiểu payload được suy ra từ tham số nên worker không cần header `__TypeId__`.
  - `application-dev.yaml`: `default-requeue-rejected: false`.

- [ ] **Step 6: Chạy toàn bộ build**

Run: `mvn -q verify`
Expected: exit 0.

---

### Task 3: Kiểm thử thủ công với RabbitMQ thật

Không viết code. Cần `.env` của user (DB dev, GCS) để chạy app.

- [ ] **Step 1: Chạy broker mới**

Run: `docker run -d --name da-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:4-management`
Expected: UI `http://localhost:15672` (guest/guest).

- [ ] **Step 2: Khởi động app (profile dev)**
Expected:
- Log không có lỗi AMQP.
- UI có exchange `tts`, `tts.dlx`; queue `tts.jobs`, `tts.results` (có arg `x-dead-letter-exchange=tts.dlx`), `tts.results.dlq`.

- [ ] **Step 3: Gửi job**

Gọi `POST /api/books/{bookId}/chapters/{chapterId}/audio` (token admin) với body `{"bookId":…, "chapterId":…, "speed":1.0}` cho một chapter có `status` null.
Expected:
- 200.
- DB: chapter `PROCESSING_AUDIO`, `reading_speed` 1.0.
- `tts.jobs` có 1 message; payload đúng định dạng job.

- [ ] **Step 4: Gọi lại cùng chapter**
Expected: 409, code 4014; `tts.jobs` vẫn 1 message.

- [ ] **Step 5: Publish kết quả thành công** (UI → exchange `tts`, routing key `tts.results`, property `content_type=application/json`)

```json
{"chapterId": <id>, "success": true, "audioPath": "books/<bookId>/audio/<id>.wav", "duration": 314, "error": null}
```
Expected: DB chapter `AUDIO_READY`, `audio_url` và `duration` đúng.

- [ ] **Step 6: Kết quả thất bại**

Đặt lại chapter sang `PROCESSING_AUDIO` bằng Step 3 (status đang là `AUDIO_READY` nên gọi được), rồi publish `{"chapterId": <id>, "success": false, "error": "test"}`.
Expected: `FAILED`, audio cũ giữ nguyên.

- [ ] **Step 7: Message hỏng**

Publish `not json` và một message không có `content_type`.
Expected: cả hai nằm trong `tts.results.dlq`; app không log lặp vô hạn.

- [ ] **Step 8: Dọn dẹp**
  - `docker rm -f da-rabbit`.
  - Báo user các chapter test đã đổi status.

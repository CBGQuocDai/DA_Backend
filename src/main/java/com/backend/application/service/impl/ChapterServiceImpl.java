package com.backend.application.service.impl;

import com.backend.domain.valueobject.UploadType;
import com.backend.application.service.FileManagementService;
import com.backend.application.dto.request.book.chapter.ChapterUploadAudioRequest;
import com.backend.application.service.ChapterService;
import com.backend.application.adapter.content.ChapterContent;
import com.backend.application.adapter.content.ChapterContentParser;
import com.backend.application.adapter.messaging.MessageQueue;
import com.backend.application.adapter.realtime.AdminEventPublisher;
import com.backend.application.adapter.repository.BookRepository;
import com.backend.application.adapter.repository.ChapterRepository;
import com.backend.application.dto.response.chapter.ChapterResponse;
import com.backend.application.mapper.ResponseMapper;
import com.backend.application.dto.request.book.chapter.ChapterCreateRequest;
import com.backend.application.dto.request.book.chapter.ChapterGenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.ChapterUpdateRequest;
import com.backend.application.adapter.messaging.GenerateAudioRequest;
import com.backend.application.dto.request.book.chapter.GenerateAudioResult;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.Book;
import com.backend.domain.model.Chapter;
import com.backend.domain.valueobject.BusinessError;
import com.backend.domain.valueobject.ChapterStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final BookRepository bookRepository;
    private final ResponseMapper responseMapper;
    private final MessageQueue messageQueue;
    private final ChapterContentParser chapterContentParser;
    private final FileManagementService fileManagementService;
    private final AdminEventPublisher adminEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByBookId(Long bookId) {
        return chapterRepository.findByBookId(bookId).stream().map(chapter -> {
            ChapterResponse response = responseMapper.toChapterResponse(chapter);
            response.setAudioUrl(responseMapper.signUrl(chapter.getAudioUrl()));
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public ChapterResponse createChapter(Long bookId, ChapterCreateRequest req) {
        // Khóa sách để các yêu cầu đổi thứ tự chương được xử lý lần lượt.
        bookRepository.findBookOverviewById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        int last = chapterRepository.findMaxChapterOrder(bookId).orElse(0);
        int order = Math.min(req.getChapterOrder(), last + 1);
        if (order <= last) {
            chapterRepository.shiftOrders(bookId, order, last, 1);
        }
        Chapter chapter = Chapter.builder()
                .bookId(bookId)
                .title(req.getTitle().trim())
                .chapterOrder(order)
                .rawText("")
                .readingSpeed(1D)
                .build();
        return responseMapper.toChapterResponse(chapterRepository.save(chapter));
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        chapterRepository.deleteById(chapterId);
    }

    @Override
    @Transactional
    public void uploadAudio(Long bookId, Long chapterId,
            ChapterUploadAudioRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow(
                () -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND));
        if (!Objects.equals(chapter.getBookId(), bookId)) {
            throw new BusinessException(BusinessError.CHAPTER_NOT_FOUND);
        }
        if (chapter.getStatus() == ChapterStatus.PROCESSING_AUDIO) {
            throw new BusinessException(BusinessError.CHAPTER_AUDIO_PROCESSING);
        }
        var type = UploadType.AUDIO;
        fileManagementService.verifyUpload(request.getAudioPath(), type);
        fileManagementService.moveToPermanent(request.getAudioPath(), type);
        chapter.setAudioUrl(type.permanentPath(request.getAudioPath()));
        chapter.setDuration(request.getDuration());
        chapter.setStatus(ChapterStatus.AUDIO_READY);
        chapterRepository.update(chapter);
    }

    @Override
    @Transactional
    public void generateAudio(ChapterGenerateAudioRequest req) {
        Book book = bookRepository.findById(req.getBookId()).orElseThrow(
                () -> new BusinessException(BusinessError.BOOK_NOT_FOUND)
        );
        Chapter chapter = chapterRepository.findById(req.getChapterId())
                .filter(candidate -> Objects.equals(candidate.getBookId(), req.getBookId())).orElseThrow(
                () -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND)
        );
        if (chapter.getRawText() == null || chapter.getRawText().isBlank()) {
            throw new BusinessException(BusinessError.CHAPTER_TEXT_EMPTY);
        }

        double readingSpeed = Objects.requireNonNullElse(req.getSpeed(), 1.0);
        if (chapter.getStatus() == ChapterStatus.PROCESSING_AUDIO) {
            throw new BusinessException(BusinessError.CHAPTER_AUDIO_PROCESSING);
        }

        String jobId = UUID.randomUUID().toString();
        chapter.setStatus(ChapterStatus.PROCESSING_AUDIO);
        chapter.setReadingSpeed(readingSpeed);
        chapter.setAudioJobId(jobId);
        chapterRepository.update(chapter);

        log.info("Queueing audio for book {} with voiceId {} and chapter {} at speed {}",
                book.getId(), book.getVoice() != null ? book.getVoice().getId() : null, chapter.getId(), readingSpeed);
        String text = chapter.getTitle() +" - " + chapter.getRawText().replace(Chapter.PAGE_BREAK, " ");
        GenerateAudioRequest job = GenerateAudioRequest.builder()
                .chapterId(chapter.getId())
                .jobId(jobId)
                .exampleVoiceUrl(book.getVoice() != null ? book.getVoice().getExampleAudio() : "")
                .exampleVoiceText(book.getVoice() != null ? book.getVoice().getExampleTranscript() : null)
                .text(text)
                .speed(readingSpeed)
                .build();
        messageQueue.send(job);
    }

    @Override
    @Transactional
    public void update(Long bookId, Long chapterId, ChapterUpdateRequest req) {
        if (Objects.nonNull(req.getChapterOrder())) {
            bookRepository.findBookOverviewById(bookId)
                    .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        }
        Chapter chapter = chapterRepository.findById(chapterId)
                .filter(candidate -> Objects.equals(candidate.getBookId(), bookId)).orElseThrow(
                () -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND)
        );
        String contentJson = req.getContentJson();
        if (contentJson != null && !contentJson.isBlank()) {
            // Lấy văn bản từ tài liệu biên tập để hai trường nội dung nhất quán.
            chapter.setRawText(chapterContentParser.extractText(contentJson));
            chapter.setContentJson(chapterContentParser.withImageUrls(contentJson, path -> null));
        } else if (Objects.nonNull(req.getRawText())) {
            // Văn bản mới không còn tương ứng với định dạng của tài liệu cũ.
            chapter.setRawText(req.getRawText());
            chapter.setContentJson(null);
        }
        if(Objects.nonNull(req.getTitle())) {
            chapter.setTitle(req.getTitle());
        }
        if (Objects.nonNull(req.getChapterOrder())) {
            moveChapter(bookId, chapter, req.getChapterOrder());
        }
        chapterRepository.update(chapter);
    }

    @Override
    @Transactional
    public ChapterResponse mergeChapters(Long bookId, List<Long> chapterIds) {
        // Khóa sách để việc đổi thứ tự chương được xử lý lần lượt
        bookRepository.findBookOverviewById(bookId)
                .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
        LinkedHashSet<Long> ids = new LinkedHashSet<>(chapterIds);
        if (ids.size() < 2) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }

        // Theo thứ tự chương, không theo thứ tự id gửi lên; thứ tự có thể có chỗ hở nên so theo vị trí
        List<Chapter> bookChapters = chapterRepository.findByBookId(bookId);
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < bookChapters.size(); index++) {
            if (ids.contains(bookChapters.get(index).getId())) {
                positions.add(index);
            }
        }
        if (positions.size() != ids.size()) {
            throw new BusinessException(BusinessError.CHAPTER_NOT_FOUND);
        }
        if (positions.getLast() - positions.getFirst() != positions.size() - 1) {
            throw new BusinessException(BusinessError.CHAPTER_MERGE_NOT_CONSECUTIVE);
        }

        // Đọc lại từng chương kèm khóa ghi để không chạy song song với việc tạo audio
        List<Chapter> merging = positions.stream()
                .map(index -> chapterRepository.findById(bookChapters.get(index).getId())
                        .orElseThrow(() -> new BusinessException(BusinessError.CHAPTER_NOT_FOUND)))
                .toList();
        List<ChapterContent> parts = new ArrayList<>();
        List<String> cancelledJobs = new ArrayList<>();
        for (Chapter chapter : merging) {
            parts.add(new ChapterContent(parts.isEmpty() ? null : chapter.getTitle(),
                    chapter.getRawText(), chapter.getContentJson()));
            if (chapter.getStatus() == ChapterStatus.PROCESSING_AUDIO && chapter.getAudioJobId() != null) {
                cancelledJobs.add(chapter.getAudioJobId());
            }
        }
        ChapterContent merged = chapterContentParser.merge(parts);

        Chapter kept = merging.getFirst();
        kept.setRawText(merged.rawText());
        kept.setContentJson(merged.contentJson());
        // Nội dung đã đổi nên audio cũ không còn khớp
        kept.setAudioUrl(null);
        kept.setDuration(null);
        kept.setStatus(null);
        kept.setAudioJobId(null);
        chapterRepository.update(kept);

        Chapter last = merging.getLast();
        merging.subList(1, merging.size()).forEach(chapter -> chapterRepository.deleteById(chapter.getId()));
        int maxOrder = chapterRepository.findMaxChapterOrder(bookId).orElse(last.getChapterOrder());
        if (maxOrder > last.getChapterOrder()) {
            chapterRepository.shiftOrders(bookId, last.getChapterOrder() + 1, maxOrder, -(merging.size() - 1));
        }

        afterCommit(() -> cancelledJobs.forEach(messageQueue::cancel));
        log.info("Merged chapters {} of book {} into chapter {}", ids, bookId, kept.getId());
        return responseMapper.toChapterResponse(kept);
    }

    /** Dịch các chương nằm giữa vị trí cũ và mới để giữ thứ tự liên tục. */
    private void moveChapter(Long bookId, Chapter chapter, int requestedOrder) {
        int current = chapter.getChapterOrder();
        int target = Math.min(requestedOrder, chapterRepository.findMaxChapterOrder(bookId).orElse(current));
        if (target < current) {
            chapterRepository.shiftOrders(bookId, target, current - 1, 1);
        } else if (target > current) {
            chapterRepository.shiftOrders(bookId, current + 1, target, -1);
        }
        chapter.setChapterOrder(target);
    }

    @Override
    @Transactional
    public void handleAudioResult(GenerateAudioResult result) {
        if (result.getChapterId() == null) {
            throw new IllegalArgumentException("TTS result is missing chapterId");
        }
        Chapter chapter = chapterRepository.findById(result.getChapterId()).orElse(null);
        if (chapter == null) {
            log.warn("TTS result for unknown chapter {}, ignoring", result.getChapterId());
            return;
        }
        if (chapter.getStatus() != ChapterStatus.PROCESSING_AUDIO) {
            log.warn("TTS result for chapter {} in status {}, ignoring", chapter.getId(), chapter.getStatus());
            return;
        }
        // Worker bản cũ không gửi jobId: vẫn nhận kết quả, chỉ mất khả năng phát hiện job đã bị hủy
        if (result.getJobId() != null && !result.getJobId().equals(chapter.getAudioJobId())) {
            // Job đã bị hủy (gộp chương) hoặc đã bị thay bằng một lần tạo audio mới
            log.warn("TTS result of job {} for chapter {} is stale, ignoring", result.getJobId(), chapter.getId());
            return;
        }
        chapter.setAudioJobId(null);
        if (result.isSuccess() && result.getAudioPath() != null && !result.getAudioPath().isBlank()) {
            chapter.setAudioUrl(result.getAudioPath());
            chapter.setDuration(result.getDuration());
            chapter.setStatus(ChapterStatus.AUDIO_READY);
        } else {
            log.error("TTS failed for chapter {}: {}", chapter.getId(), result.getError());
            chapter.setStatus(ChapterStatus.FAILED);
        }
        chapterRepository.update(chapter);
        afterCommit(() -> adminEventPublisher.chapterStatusChanged(chapter.getBookId(), chapter.getId(),
                chapter.getStatus(), chapter.getAudioUrl(), chapter.getDuration()));
    }

    // Chạy sau khi commit: trang admin không đọc lại phải trạng thái cũ, và giao dịch rollback thì không có gì bị gửi đi
    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}

package com.backend.application.service.impl;

import com.backend.application.adapter.repository.*;
import com.backend.application.dto.request.progress.ListenProgressRequest;
import com.backend.application.dto.response.progress.BookProgressResponse;
import com.backend.application.dto.response.progress.BookProgressResponse.ChapterProgress;
import com.backend.application.dto.response.progress.BookProgressResponse.Resume;
import com.backend.application.service.ListenProgressService;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.model.ListenProgress;
import com.backend.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListenProgressServiceImpl implements ListenProgressService {

    private static final int PREVIEW_CHAPTERS = 3;
    private final ListenProgressRepository progressRepository;
    private final CustomerRepository customerRepository;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final CustomerBookRepository customerBookRepository;

    // Lấy tiến độ nghe tổng thể của cuốn sách:
    // danh sách chương đã nghe và xác định vị trí tiếp tục phát
    @Override
    @Transactional(readOnly = true)
    public BookProgressResponse getProgress(Long customerId, Long bookId) {
        validateIds(customerId, bookId);
        customerRepository.findById(customerId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        requirePublishedBook(bookId);
        List<PlaybackChapter> chapters = chapterRepository.findPlaybackChaptersByBookId(bookId);
        boolean owned = customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
        return response(customerId, bookId, chapters, owned);
    }

    // Lưu tiến độ nghe chương: kiểm tra quyền nghe (mua/nghe thử 3 chương), khóa giao dịch chống xung đột, cập nhật đọc xong sách
    @Override
    @Transactional
    public BookProgressResponse saveProgress(Long customerId, Long bookId,
        Long chapterId, ListenProgressRequest request) {
        validateIds(customerId, bookId);
        if (chapterId == null || chapterId <= 0 || request == null
            || request.getLastPlayAt() == null
            || request.getLastPlayAt() < 0) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        // tránh xung đột khi tạo mới hoặc cập nhật hoàn thành sách.
        customerRepository.findByIdForUpdate(customerId)
            .orElseThrow(() -> new BusinessException(BusinessError.USER_NOT_FOUND));
        requirePublishedBook(bookId);
        var chapters = chapterRepository.findPlaybackChaptersByBookId(bookId);
        int index = -1;
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).id().equals(chapterId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new BusinessException(BusinessError.CHAPTER_NOT_FOUND);
        }
        boolean owned = customerBookRepository.existsByCustomerIdAndBookId(customerId, bookId);
        if (!owned && index >= PREVIEW_CHAPTERS) {
            throw new BusinessException(BusinessError.FORBIDDEN);
        }
        var chapter = chapters.get(index);
        if (chapter.status() == ChapterStatus.PROCESSING_AUDIO) {
            throw new BusinessException(BusinessError.CHAPTER_AUDIO_PROCESSING);
        }
        if (!chapter.isPlayable() || request.getLastPlayAt() > chapter.duration()) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        var progress = progressRepository.findByCustomerIdAndChapterId(customerId, chapterId)
            .orElseGet(() -> ListenProgress.builder().customerId(customerId).bookId(bookId)
                .chapterId(chapterId).isComplete(false).build());
        boolean previouslyComplete = Boolean.TRUE.equals(progress.getIsComplete());
        if (Boolean.TRUE.equals(request.getIsComplete()) && !previouslyComplete
            && request.getLastPlayAt() < chapter.duration() - 1) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
        // Tua lại cập nhật vị trí nghe mới; nghe lại không làm mất trạng thái hoàn thành trước đó.
        progress.setLastPlayAt(request.getLastPlayAt());
        progress.setIsComplete(previouslyComplete || Boolean.TRUE.equals(request.getIsComplete()));
        progress.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        progressRepository.save(progress);
        var result = response(customerId, bookId, chapters, owned);
        if (owned) {
            customerBookRepository.updateReadComplete(customerId, bookId, result.isBookComplete());
        }
        return result;
    }

    // Tổng hợp dữ liệu phản hồi tiến độ sách và kiểm tra điều kiện hoàn thành sách
    private BookProgressResponse response(Long customerId, Long bookId,
        List<PlaybackChapter> chapters, boolean owned) {

        var saved = progressRepository.findByCustomerIdAndBookId(customerId, bookId).stream()
            .collect(Collectors.toMap(ListenProgress::getChapterId, Function.identity()));

        var positions = new ArrayList<ChapterProgress>();
        for (int i = 0; i < chapters.size(); i++) {
            var chapter = chapters.get(i);
            var progress = saved.get(chapter.id());
            if (progress == null || !chapter.isPlayable() || (!owned && i >= PREVIEW_CHAPTERS)) {
                continue;
            }
            positions.add(new ChapterProgress(chapter.id(), i,
                Math.min(progress.getLastPlayAt(), chapter.duration()),
                Boolean.TRUE.equals(progress.getIsComplete()), progress.getUpdatedAt()));
        }
        int completed = (int) positions.stream().filter(ChapterProgress::isComplete).count();
        return new BookProgressResponse(bookId, !positions.isEmpty(), positions, chapters.size(),
            completed,
            owned && !chapters.isEmpty() && completed == chapters.size(),
            resume(chapters, positions, owned));
    }

    // Xác định chương và vị trí giây gần nhất để khách hàng nghe tiếp (Resume Playback)
    private Resume resume(List<PlaybackChapter> chapters, List<ChapterProgress> positions,
        boolean owned) {
        var latest = positions.stream().max(Comparator.comparing(ChapterProgress::updatedAt)
            .thenComparingInt(ChapterProgress::chapterIndex)).orElse(null);
        if (latest == null) {
            return null;
        }
        int index = latest.chapterIndex();
        // Giữ trạng thái hoàn thành khi nghe lại. Chỉ tự động chuyển chương tiếp theo khi vị trí nghe ở cuối chương.
        boolean atEnd =
            latest.isComplete() && latest.lastPlayAt() >= chapters.get(index).duration() - 1;
        int next = index + 1;
        if (atEnd && next < chapters.size() && (owned || next < PREVIEW_CHAPTERS)
            && chapters.get(next).isPlayable()) {
            index = next;
        }
        var target = chapters.get(index);
        var saved = positions.stream().filter(position -> position.chapterId().equals(target.id()))
            .findFirst().orElse(null);
        int position = saved == null ? 0 : saved.lastPlayAt();
        if (saved != null && saved.isComplete() && position >= target.duration() - 1) {
            position = 0;
        }
        return new Resume(target.id(), index, position);
    }

    // Kiểm tra tính hợp lệ của customerId và bookId
    private void validateIds(Long customerId, Long bookId) {
        if (customerId == null) {
            throw new BusinessException(BusinessError.UNAUTHORIZED);
        }
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(BusinessError.VALIDATION_FAILED);
        }
    }

    // Đảm bảo sách tồn tại và đang ở trạng thái đã xuất bản (PUBLISHED)
    private void requirePublishedBook(Long bookId) {
        bookRepository.findStatusById(bookId).filter(status -> status == BookStatus.PUBLISHED)
            .orElseThrow(() -> new BusinessException(BusinessError.BOOK_NOT_FOUND));
    }
}

package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.application.dto.response.review.ReviewSummaryResponse;
import com.backend.domain.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    /** Lưu dữ liệu và trả về đánh giá đã lưu. */
    Review save(Review review);

    /** Lấy toàn bộ đánh giá của sách, mới nhất trước. */
    List<Review> getReviewByBookId(Long bookId);
    /** Lọc đánh giá theo số sao và phân trang theo thứ tự được yêu cầu. */
    PageResponse<Review> findPage(Long bookId, Integer rating, String sort, int page, int size);
    /** Tổng hợp điểm trung bình, số lượng và phân bố số sao của sách. */
    ReviewSummaryResponse getSummary(Long bookId);
    /** Tìm đánh giá của khách hàng cho sách được chỉ định. */
    Optional<Review> findByBookIdAndCustomerId(Long bookId, Long customerId);
    /** Kiểm tra khách hàng đã đánh giá sách hay chưa. */
    boolean existsByBookIdAndCustomerId(Long bookId, Long customerId);

    /** Tổng hợp điểm trung bình, số lượng và phân bố số sao của tất cả sách. */
    ReviewSummaryResponse getOverallSummary();
}

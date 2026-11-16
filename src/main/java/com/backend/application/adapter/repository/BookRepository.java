package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Book;
import com.backend.domain.model.BookStat;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.BookStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookRepository {

    /** Tạo sách mới và lưu các liên kết tác giả, thể loại. */
    Book insert(Book book);

    /** Lấy sách theo ID kèm giọng đọc, tác giả, thể loại và chương; trả về rỗng nếu không tồn tại. */
    Optional<Book> findById(Long id);

    /** Chỉ lấy dữ liệu bảng sách, không join các entity liên quan; giữ khóa ghi đến hết giao dịch. */
    Optional<Book> findBookOverviewById(Long id);

    /** Lọc và phân trang sách theo tên, thể loại, tác giả, trạng thái, kèm điểm đánh giá. */
    PageResponse<BookStat> findAllWithStats(String bookName, Long categoryId, Long authorId,
                                            BookStatus status, int page, int size);

    /** Cập nhật dữ liệu của sách hiện có. */
    void update(Book book);
    /** Xóa sách theo ID. */
    void deleteById(Long id);

    /** Lấy danh sách sách theo các ID được chỉ định. */
    List<Book> findAllById(List<Long> bookIds);

    /** Lấy thống kê lượt mua trong kỳ và kỳ trước, sắp xếp lượt mua giảm dần. */
    List<BookStat> findBestsellers(BestsellerPeriod.Window window, int limit);
}

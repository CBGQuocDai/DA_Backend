package com.backend.application.adapter.repository;

import com.backend.domain.model.BookFavorite;
import java.util.List;


public interface BookFavoriteRepository {

    /** Lưu dữ liệu và trả về liên kết yêu thích đã lưu. */
    BookFavorite save(BookFavorite bookFavorite);

    /** Kiểm tra liên kết giữa khách hàng và sách đã tồn tại hay chưa. */
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);

    /** Xóa sách khỏi danh sách yêu thích của khách hàng. */
    void deleteByCustomerIdAndBookId(Long customerId, Long bookId);

    /** Lấy ID các sách được khách hàng yêu thích. */
    List<Long> findBookIdsByCustomerId(Long customerId);
}

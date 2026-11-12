package com.backend.application.adapter.repository;

import com.backend.domain.model.CustomerBook;
import com.backend.domain.valueobject.BestsellerPeriod;
import com.backend.domain.valueobject.PeriodCount;

import java.util.List;

public interface CustomerBookRepository {
    /** Kiểm tra liên kết giữa khách hàng và sách đã tồn tại hay chưa. */
    boolean existsByCustomerIdAndBookId(Long customerId, Long bookId);
    /** Lưu dữ liệu và trả về quyền sở hữu sách đã lưu. */
    CustomerBook save(CustomerBook customerBook);
    /** Lấy danh sách sách mà khách hàng sở hữu. */
    List<CustomerBook> findByCustomerId(Long customerId);

    /** Đếm lượt sở hữu sách, gồm mua và miễn phí, trong kỳ và kỳ trước. */
    PeriodCount countPurchases(BestsellerPeriod.Window window);

    /** Đếm khách hàng đang sở hữu ít nhất một sách. */
    long countBuyers();
}

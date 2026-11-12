package com.backend.application.adapter.repository;

import com.backend.domain.model.Payment;
import com.backend.domain.valueobject.MonthlyTotal;
import com.backend.domain.valueobject.PaymentStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    /** Tìm giao dịch thanh toán theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Payment> findById(Long id);
    /** Tìm giao dịch theo mã thanh toán. */
    Optional<Payment> findByPaymentCode(String paymentCode);
    /** Tìm giao dịch mới nhất của khách hàng cho sách với trạng thái được chỉ định. */
    Optional<Payment> findLatestByCustomerIdAndBookIdAndStatus(Long customerId, Long bookId, PaymentStatus status);
    /** Lưu dữ liệu và trả về giao dịch thanh toán đã lưu. */
    Payment save(Payment payment);

    /** Tổng hợp thanh toán thành công theo tháng từ from, theo múi giờ zone; bỏ qua tháng trống. */
    List<MonthlyTotal> sumPaidByMonth(Instant from, ZoneId zone);
}

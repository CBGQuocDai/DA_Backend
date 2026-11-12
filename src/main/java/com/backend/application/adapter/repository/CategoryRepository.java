package com.backend.application.adapter.repository;

import com.backend.application.dto.response.PageResponse;
import com.backend.domain.model.Category;
import com.backend.domain.model.CategoryStat;
import com.backend.domain.valueobject.BestsellerPeriod;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    /** Lấy danh sách toàn bộ thể loại. */
    List<Category> findAll();

    /** Tìm theo tên hoặc mô tả, mới nhất trước; từ khóa trống lấy tất cả. */
    PageResponse<Category> search(String keyword, int page, int size);

    /** Tìm thể loại theo ID; trả về Optional rỗng nếu không tồn tại. */
    Optional<Category> findById(Long id);

    /** Lưu dữ liệu và trả về thể loại đã lưu. */
    Category save(Category category);

    /** Cập nhật thể loại hiện có từ domain entity và trả về dữ liệu đã cập nhật. */
    Category update(Category category);

    /** Xóa thể loại theo ID. */
    void deleteById(Long id);

    /** Kiểm tra thể loại có đang được sách nào sử dụng hay không. */
    boolean isUsedByAnyBook(Long id);

    /** Kiểm tra tên không phân biệt hoa thường; excludeId bỏ qua thể loại đang sửa, null khi tạo. */
    boolean existsByName(String name, Long excludeId);

    /** Thống kê lượt mua theo thể loại trong kỳ và kỳ trước; sách thuộc nhiều thể loại được tính vào từng thể loại. */
    List<CategoryStat> findBestsellers(BestsellerPeriod.Window window);
}

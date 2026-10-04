package com.moneybook.backend.board.repository.impl;
import com.moneybook.backend.entity.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoardCategoryJpaRepository extends JpaRepository<BoardCategory,Long> {
 List<BoardCategory> findByDeletedFalseAndActiveTrueOrderByDisplayOrderAscCategoryUidAsc();
 List<BoardCategory> findByDeletedFalseOrderByDisplayOrderAscCategoryUidAsc();
}

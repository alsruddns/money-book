package com.moneybook.backend.board.repository.impl;

import com.moneybook.backend.entity.BoardPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardPostSearchRepository {

    Page<BoardPost> search(Long categoryUid, String keyword, Long viewerUid,
                           boolean superAdmin, Pageable pageable);
}

package com.dolphin.demo.repository;

import com.dolphin.demo.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    /* 여행지 상세페이지의 리뷰를 불러올 때 */
    List<Comment> findAllByPlaceIdByCreatedAtDesc(Long place_id);

    /* 마이페이지의 내가 쓴 리뷰를 불러올 때 */
    List<Comment> findAllByMemberId(Long member_id);

}

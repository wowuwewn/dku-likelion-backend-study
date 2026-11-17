package com.ll.demo03;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

// Surl 엔티티의 저장과 조회를 담당한다.
public interface SurlRepository extends JpaRepository<Surl, Long> {
    @EntityGraph(attributePaths = "author")
    List<Surl> findByAuthorOrderByIdDesc(Member author);
}

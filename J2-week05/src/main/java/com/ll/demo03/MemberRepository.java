package com.ll.demo03;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // username으로 회원을 찾는다.
    // 해당 회원이 없을 수도 있으므로 Optional<Member>를 반환한다.
    Optional<Member> findByUsername(String username);
}
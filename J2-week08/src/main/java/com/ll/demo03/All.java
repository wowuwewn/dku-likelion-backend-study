package com.ll.demo03;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class All {

    @Lazy
    @Autowired
    private All self;

    private final MemberService memberService;

    @Bean
    @Order(3)
    public ApplicationRunner initAll() {
        return args -> {
            self.work1();
        };
    }

    @Transactional
    public void work1() {
        log.debug("initAll started");

        // 일부 회원만 있는 기존 DB도 보존하면서 빠진 초기 회원만 추가한다.
        joinIfMissing("system", "시스템");
        joinIfMissing("admin", "관리자");
        joinIfMissing("user1", "유저 1");
        joinIfMissing("user2", "유저 2");
    }

    private void joinIfMissing(String username, String nickname) {
        if (memberService.findByUsername(username).isEmpty()) {
            // 강의용 초기 회원의 예시 암호이며 실제 서비스 인증에는 사용하지 않는다.
            memberService.join(username, "1234", nickname);
        }
    }
}

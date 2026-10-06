package com.ll.demo03;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Profile("!prod")
@Configuration
@RequiredArgsConstructor
public class NotProd {
    // Spring 객체를 통해 호출해야 work1의 트랜잭션이 적용된다.
    @Lazy
    @Autowired
    private NotProd self;

    private final MemberService memberService;
    private final ArticleService articleService;

    @Bean
    public ApplicationRunner initNotProd() {
        return args -> {
            self.work1();
            self.work2();
        };
    }

    @Transactional
    public void work1() {
        // 기존 DB를 보존하고 재실행 시 샘플 데이터 생성을 건너뛴다.
        if (articleService.count() > 0) return;

        Member member1 = memberService.join("user1", "1234", "유저 1").getData();
        Member member2 = memberService.join("user2", "1234", "유저 2").getData();

        // 강의처럼 각 회원이 작성한 게시물 2개씩을 연결해 저장한다.
        Article article1 = articleService.write(member1, "제목 1", "내용 1").getData();
        Article article2 = articleService.write(member1, "제목 2", "내용 2").getData();
        Article article3 = articleService.write(member2, "제목 1", "내용 1").getData();
        Article article4 = articleService.write(member2, "제목 2", "내용 2").getData();
    }

    @Transactional
    public void work2() {
    }
}

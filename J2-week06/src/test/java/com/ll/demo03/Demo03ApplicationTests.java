package com.ll.demo03;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class Demo03ApplicationTests {
    @Autowired private ApplicationContext context;
    @Autowired private Environment environment;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MemberService memberService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ArticleService articleService;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private All all;
    @Autowired private NotProd notProd;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void contextLoads() throws Exception {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
        assertThat(environment.getProperty("server.port")).isEqualTo("8090");
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:db_test");
        }
        assertThat(jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class)).contains("MEMBER", "ARTICLE", "SURL");
    }

    @Test
    @Transactional
    void runnersCreateMembersBeforeArticles() {
        List<ApplicationRunner> runners = context.getBeanProvider(ApplicationRunner.class)
                .orderedStream().toList();
        assertThat(runners).containsExactly(context.getBean("initAll", ApplicationRunner.class),
                context.getBean("initNotProd", ApplicationRunner.class));
        assertThat(memberRepository.findAll()).extracting(Member::getUsername)
                .containsExactlyInAnyOrder("system", "admin", "user1", "user2");
        assertThat(articleService.findAll()).hasSize(4)
                .extracting(article -> article.getAuthor().getUsername())
                .containsExactlyInAnyOrder("user1", "user1", "user2", "user2");
    }

    @Test
    @Transactional
    void repeatedInitializationDoesNotDuplicateData() {
        long members = memberService.count();
        long articles = articleService.count();
        all.work1();
        notProd.work1();
        all.work1();
        notProd.work1();
        assertThat(memberService.count()).isEqualTo(members);
        assertThat(articleService.count()).isEqualTo(articles);
    }

    @Test
    @Transactional
    void partialMembersAreCompletedWithoutChangingExistingMember() {
        // 이 삭제는 @ActiveProfiles("test")의 메모리 DB 안에서만 수행하고 자동 롤백한다.
        articleRepository.deleteAll();
        memberRepository.deleteAll();
        Member existing = memberService.join("user1", "existing-test-password", "기존 회원").getData();

        all.work1();
        notProd.work1();

        Member preserved = memberService.findByUsername("user1").orElseThrow();
        assertThat(preserved.getId()).isEqualTo(existing.getId());
        assertThat(preserved.getPassword()).isEqualTo("existing-test-password");
        assertThat(preserved.getNickname()).isEqualTo("기존 회원");
        assertThat(memberService.count()).isEqualTo(4);
        assertThat(articleService.count()).isEqualTo(4);
    }

    @Test
    @Transactional
    void missingMembersAreAddedEvenWhenArticlesAlreadyExist() {
        articleRepository.deleteAll();
        memberRepository.deleteAll();
        Member existing = memberService.join("user1", "test-password", "기존 회원").getData();
        Article article = articleService.write(existing, "기존 게시물", "보존할 내용").getData();

        all.work1();
        notProd.work1();

        assertThat(memberService.count()).isEqualTo(4);
        assertThat(articleService.findAll()).extracting(Article::getId).containsExactly(article.getId());
    }

    @Test
    void memberAndArticleWritesRollBackTogetherOnFailure() {
        long members = memberService.count();
        long articles = articleService.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Member member = memberService.join("rollback-test", "test-password", "롤백 확인").getData();
            articleService.write(member, "롤백 게시물", "저장되지 않아야 함");
            throw new IllegalStateException("rollback verification");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback verification");

        assertThat(memberService.findByUsername("rollback-test")).isEmpty();
        assertThat(memberService.count()).isEqualTo(members);
        assertThat(articleService.count()).isEqualTo(articles);
    }

}

package com.ll.demo03;

import com.ll.demo03.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor

// 기본적으로 조회 전용 트랜잭션을 적용한다.
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;

    // 게시물 개수를 조회한다.
    public long count() {
        return articleRepository.count();
    }

    // 작성자와 게시물을 함께 연결해 저장하므로 일반 트랜잭션을 사용한다.
    @Transactional
    public RsData<Article> write(Member author, String title, String body) {
        Article article = Article.builder()
                .author(author)
                .title(title)
                .body(body)
                .build();

        articleRepository.save(article);

        return RsData.of(
                "%d번 게시물이 작성되었습니다."
                        .formatted(article.getId()),
                article
        );
    }

    // 게시물을 삭제하므로 일반 트랜잭션을 사용한다.
    @Transactional
    public void delete(Article article) {
        articleRepository.delete(article);
    }

    // 게시물 하나를 조회한다.
    public Optional<Article> findById(long id) {
        return articleRepository.findById(id);
    }

    // 모든 게시물을 조회한다.
    public List<Article> findAll() {
        return articleRepository.findAll();
    }
}
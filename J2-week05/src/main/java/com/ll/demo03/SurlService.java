package com.ll.demo03;

import com.ll.demo03.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurlService {
    private final SurlRepository surlRepository;

    public List<Surl> findAll() {
        return surlRepository.findAll();
    }

    // 단축 URL 생성은 DB를 변경하므로 쓰기 트랜잭션에서 수행한다.
    @Transactional
    public RsData<Surl> add(Member author, String body, String url) {
        Surl surl = Surl.builder()
                .author(author)
                .body(body)
                .url(url)
                .build();

        surlRepository.save(surl);

        return RsData.of("%d번 URL이 생성되었습니다.".formatted(surl.getId()), surl);
    }

    public Optional<Surl> findById(long id) {
        return surlRepository.findById(id);
    }

    // 조회한 엔티티의 변경을 트랜잭션 종료 시 DB에 반영한다.
    @Transactional
    public void increaseCount(Surl surl) {
        surl.increaseCount();
    }
}

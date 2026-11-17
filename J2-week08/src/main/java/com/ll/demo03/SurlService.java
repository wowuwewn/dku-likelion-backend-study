package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.Empty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurlService {
    private final SurlRepository surlRepository;
    private final AuthService authService;

    // 목록도 전체 공개하지 않는다. 강의처럼 본인 자료를 ID 역순으로 반환한다.
    public List<SurlDto> findByAuthorOrderByIdDesc(Member author) {
        return surlRepository.findByAuthorOrderByIdDesc(author).stream().map(SurlDto::new).toList();
    }

    @Transactional
    public RsData<SurlDto> add(Member author, String body, String url) {
        Surl surl = Surl.builder().author(author).body(body).url(url).build();
        surlRepository.save(surl);
        return RsData.of("%d번 SURL이 생성되었습니다.".formatted(surl.getId()), new SurlDto(surl));
    }

    public SurlDto get(Member actor, long id) {
        Surl surl = findSurl(id);
        authService.checkCanGetSurl(actor, surl);
        // OSIV 없이도 LAZY 작성자에 접근하도록 트랜잭션 안에서 DTO를 완성한다.
        return new SurlDto(surl);
    }

    @Transactional
    public RsData<SurlDto> modify(Member actor, long id, String body, String url) {
        Surl surl = findSurl(id);
        authService.checkCanModifySurl(actor, surl);
        surl.setBody(body);
        surl.setUrl(url);
        // save 없이 관리 엔티티의 변경을 flush한다. Auditing 수정일도 DTO에 반영한다.
        surlRepository.flush();
        return RsData.of("%d번 SURL이 수정되었습니다.".formatted(id), new SurlDto(surl));
    }

    @Transactional
    public RsData<Empty> delete(Member actor, long id) {
        Surl surl = findSurl(id);
        authService.checkCanDeleteSurl(actor, surl);
        surlRepository.delete(surl);
        return RsData.OK;
    }

    // 공유 단축 링크는 공개한다. 조회와 조회수 증가를 같은 쓰기 트랜잭션에서 처리한다.
    @Transactional
    public String getRedirectUrl(long id) {
        Surl surl = findSurl(id);
        surl.increaseCount();
        return surl.getUrl();
    }

    private Surl findSurl(long id) {
        return surlRepository.findById(id).orElseThrow(GlobalException.E404::new);
    }
}

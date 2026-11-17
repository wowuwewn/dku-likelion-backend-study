package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    // 인증은 Rq, 인가는 여기서 담당한다. 프록시 여부와 무관하게 DB ID를 비교한다.
    public void checkCanGetSurl(Member actor, Surl surl) {
        if (actor == null || actor.getId() == null || surl == null || surl.getAuthor() == null
                || !actor.getId().equals(surl.getAuthor().getId())) {
            throw new GlobalException("403-1", "권한이 없습니다.");
        }
    }

    public void checkCanModifySurl(Member actor, Surl surl) {
        checkCanGetSurl(actor, surl);
    }

    public void checkCanDeleteSurl(Member actor, Surl surl) {
        checkCanGetSurl(actor, surl);
    }
}

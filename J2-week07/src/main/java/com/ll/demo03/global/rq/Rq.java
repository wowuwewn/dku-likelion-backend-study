package com.ll.demo03.global.rq;

import com.ll.demo03.Member;
import com.ll.demo03.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

// 요청마다 회원 정보를 얻는 창구를 별도 객체로 제공한다.
@Component
@RequestScope
@RequiredArgsConstructor
public class Rq {
    private final MemberService memberService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public void setStatusCode(int statusCode) {
        response.setStatus(statusCode);
    }

    public String getCurrentUrlPath() {
        // 쿼리 문자열에는 민감한 값이 있을 수 있으므로 경로만 사용한다.
        return request.getRequestURI();
    }

    public Member getMember() {
        // 06-20 강의의 임시 로그인 가정: 1번 회원의 프록시를 반환한다.
        return memberService.getReferenceById(1L);
    }
}

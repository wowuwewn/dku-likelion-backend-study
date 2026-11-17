package com.ll.demo03.global.rq;

import com.ll.demo03.Member;
import com.ll.demo03.MemberService;
import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.standard.dto.util.Ut;
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
    private Member member;
    private GlobalException authenticationFailure;

    public void setStatusCode(int statusCode) {
        response.setStatus(statusCode);
    }

    public String getCurrentUrlPath() {
        // 쿼리 문자열에는 민감한 값이 있을 수 있으므로 경로만 사용한다.
        return request.getRequestURI();
    }

    public Member getMember() {
        // 09-15: 성공과 인증 실패만 이 요청에 캐시한다. DB 장애 등은 캐시하지 않는다.
        if (member != null) return member;
        if (authenticationFailure != null) throw authenticationFailure;
        try {
            String username = request.getParameter("actorUsername");
            String password = request.getParameter("actorPassword");
            if (Ut.str.isBlank(username))
                throw new GlobalException("401-1", "인증정보(아이디)를 입력해주세요.");
            if (Ut.str.isBlank(password))
                throw new GlobalException("401-2", "인증정보(비밀번호)를 입력해주세요.");
            Member candidate = memberService.findByUsername(username)
                    .orElseThrow(() -> new GlobalException("401-3", "인증정보가 올바르지 않습니다."));
            if (!password.equals(candidate.getPassword()))
                throw new GlobalException("401-4", "인증정보가 올바르지 않습니다.");
            member = candidate;
            return member;
        } catch (GlobalException ex) {
            authenticationFailure = ex;
            throw ex;
        }
    }
}

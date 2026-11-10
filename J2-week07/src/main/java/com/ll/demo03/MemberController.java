package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.util.Ut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

// 08-02: 기존 가입 서비스를 HTTP 요청과 연결한다.
@Controller
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 08-08: 생성 요청은 POST로 받는다. 최종 JSON API는 /api/v1/members다.
    @PostMapping("/join")
    @ResponseBody
    public RsData<MemberJoinRespBody> join(String username, String password, String nickname) {
        if (Ut.str.isBlank(username)) {
            throw new GlobalException("400-username", "아이디를 입력해주세요.");
        }
        if (Ut.str.isBlank(password)) {
            throw new GlobalException("400-password", "비밀번호를 입력해주세요.");
        }
        if (Ut.str.isBlank(nickname)) {
            throw new GlobalException("400-nickname", "닉네임을 입력해주세요.");
        }
        RsData<Member> joinRs = memberService.join(username, password, nickname);
        return joinRs.newDataOf(MemberJoinRespBody.from(joinRs.getData()));
    }
}

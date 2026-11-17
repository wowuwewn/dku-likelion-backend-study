package com.ll.demo03;

import com.ll.demo03.global.rsData.RsData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class ApiV1MemberController {
    private final MemberService memberService;

    public record MemberJoinReqBody(
            @NotBlank(message = "아이디를 입력해주세요.") String username,
            @NotBlank(message = "비밀번호를 입력해주세요.") String password,
            @NotBlank(message = "닉네임을 입력해주세요.") String nickname
    ) {}

    @PostMapping(consumes = "application/json")
    public RsData<MemberJoinRespBody> join(@RequestBody @Valid MemberJoinReqBody reqBody) {
        RsData<Member> joinRs = memberService.join(reqBody.username(), reqBody.password(), reqBody.nickname());
        return joinRs.newDataOf(MemberJoinRespBody.from(joinRs.getData()));
    }
}

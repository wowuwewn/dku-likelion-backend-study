package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor

// 기본적으로 회원 조회 기능은 읽기 전용 트랜잭션을 사용한다.
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    // username으로 회원을 조회한다.
    private Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    // 회원가입은 회원을 저장하므로 일반 트랜잭션을 사용한다.
    @Transactional
    public RsData<Member> join(
            String username,
            String password,
            String nickname
    ) {
        findByUsername(username).ifPresent(ignored -> {
            throw new GlobalException(
                    "400-1",
                    "%s(은)는 이미 존재하는 아이디입니다."
                            .formatted(username)
            );
        });

        Member member = Member.builder()
                .username(username)
                .password(password)
                .nickname(nickname)
                .build();

        memberRepository.save(member);

        return RsData.of(
                "회원가입이 완료되었습니다.",
                member
        );
    }
    // ID로 회원의 프록시를 얻고, 다른 필드에 접근할 때 실제 데이터를 조회한다.
    public Member getReferenceById(long id) {
        return memberRepository.getReferenceById(id);
    }
}

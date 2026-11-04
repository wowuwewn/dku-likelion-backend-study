package com.ll.demo03;

import com.ll.demo03.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
@Setter
// 회원도 공통 ID와 날짜 매핑을 상속받는다.
public class Member extends BaseTime {
    // 현재 프로젝트의 회원 아이디 중복 제한은 유지한다.
    @Column(unique = true)
    private String username;

    private String password;
    private String nickname;
}

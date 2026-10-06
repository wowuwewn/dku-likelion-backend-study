package com.ll.demo03;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.demo03.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

// 단축 URL을 메모리 대신 DB에 저장하고 공통 ID와 날짜를 상속받는다.
@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
@Setter
public class Surl extends BaseTime {
    // 작성자는 회원 외래키로 저장하되 JSON 응답에서는 제외한다.
    @ManyToOne
    @JsonIgnore
    private Member author;

    private String body;
    private String url;

    @Setter(AccessLevel.NONE)
    private long count;

    // 단축 URL로 이동한 횟수를 증가시킨다.
    public void increaseCount() {
        count++;
    }
}

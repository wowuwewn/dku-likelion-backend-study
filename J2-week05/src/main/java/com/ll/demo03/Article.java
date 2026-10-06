package com.ll.demo03;

import com.ll.demo03.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Getter
@Setter
// ID와 날짜 필드는 BaseTime에서 상속받는다.
public class Article extends BaseTime {
    private String title;

    // 게시물 내용은 긴 문자열을 저장할 수 있는 TEXT 컬럼을 사용한다.
    @Column(columnDefinition = "TEXT")
    private String body;

    // 여러 게시물이 한 회원을 작성자로 참조하며 DB에는 author_id가 저장된다.
    @ManyToOne
    private Member author;
}

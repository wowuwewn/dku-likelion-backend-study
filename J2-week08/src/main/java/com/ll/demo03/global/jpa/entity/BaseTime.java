package com.ll.demo03.global.jpa.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 상속받는 엔티티의 생성일과 수정일을 JPA Auditing으로 기록한다.
@MappedSuperclass
@Getter
@Setter(AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTime extends BaseEntity {
    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    // 필요할 때 수정 시간을 현재 시간으로 갱신한다.
    public void setModified() {
        setModifyDate(LocalDateTime.now());
    }
}

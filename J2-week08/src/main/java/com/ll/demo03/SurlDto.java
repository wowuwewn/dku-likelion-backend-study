package com.ll.demo03;

import java.time.LocalDateTime;

// 09-04: 연관 엔티티를 외부에 넘기지 않고 공개할 값만 복사한다.
public record SurlDto(Long id, LocalDateTime createDate, LocalDateTime modifyDate,
                      Long authorId, String authorName, String body, String url, long count) {
    public SurlDto(Surl surl) {
        this(surl.getId(), surl.getCreateDate(), surl.getModifyDate(),
                surl.getAuthor() == null ? null : surl.getAuthor().getId(),
                surl.getAuthor() == null ? null : surl.getAuthor().getNickname(),
                surl.getBody(), surl.getUrl(), surl.getCount());
    }
}

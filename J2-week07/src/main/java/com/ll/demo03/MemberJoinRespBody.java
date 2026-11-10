package com.ll.demo03;

// 08-15: DB 엔티티 대신 공개할 필드만 응답 계약으로 정한다.
public record MemberJoinRespBody(MemberItem item) {
    public record MemberItem(Long id, String username, String nickname) {}

    public static MemberJoinRespBody from(Member member) {
        return new MemberJoinRespBody(new MemberItem(
                member.getId(), member.getUsername(), member.getNickname()));
    }
}

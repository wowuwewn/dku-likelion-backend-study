package com.ll.demo03.global.exceptions;

import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.Empty;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

    // 예외가 발생했을 때 결과 코드와 메시지를 함께 보관한다.
    private final RsData<Empty> rsData;

    // 기본 오류 정보를 사용한다.
    public GlobalException() {
        this("400-0", "에러");
    }

    // 메시지만 전달받은 경우 기본 오류 코드를 사용한다.
    public GlobalException(String msg) {
        this("400-0", msg);
    }

    // 결과 코드와 메시지를 받아 예외 객체를 만든다.
    public GlobalException(String resultCode, String msg) {
        super("resultCode=" + resultCode + ",msg=" + msg);

        this.rsData = RsData.of(resultCode, msg);
    }

    // 데이터를 찾을 수 없는 상황에서 사용할 404 예외
    public static class E404 extends GlobalException {

        public E404() {
            super("404-0", "데이터를 찾을 수 없습니다.");
        }
    }
}
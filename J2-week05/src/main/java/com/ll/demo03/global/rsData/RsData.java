package com.ll.demo03.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ll.demo03.standard.dto.Empty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import static lombok.AccessLevel.PRIVATE;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PRIVATE)
@Getter
public class RsData<T> {

    // 기본적인 성공 결과를 표현한다.
    public static final RsData<Empty> OK =
            of("200-1", "성공", new Empty());

    // 기본적인 실패 결과를 표현한다.
    public static final RsData<Empty> FAIL =
            of("500-1", "실패", new Empty());

    // 작업 결과를 구분하기 위한 코드
    @NonNull
    String resultCode;

    // HTTP 상태 코드를 저장한다.
    @NonNull
    int statusCode;

    // 작업 결과에 대한 설명 메시지를 저장한다.
    @NonNull
    String msg;

    // 실제 반환할 데이터를 저장한다.
    @NonNull
    T data;

    // 메시지만 전달받아 성공 결과를 만든다.
    public static RsData<Empty> of(String msg) {
        return of("200-1", msg, new Empty());
    }

    // 실제 데이터만 전달받아 성공 결과를 만든다.
    public static <T> RsData<T> of(T data) {
        return of("200-1", "성공", data);
    }

    // 메시지와 실제 데이터를 함께 전달받아 성공 결과를 만든다.
    public static <T> RsData<T> of(String msg, T data) {
        return of("200-1", msg, data);
    }

    // 결과 코드와 메시지를 이용해서 결과를 만든다.
    public static <T> RsData<T> of(String resultCode, String msg) {
        return of(resultCode, msg, (T) new Empty());
    }

    // 결과 코드에서 HTTP 상태 코드를 얻고 전체 결과 객체를 만든다.
    public static <T> RsData<T> of(
            String resultCode,
            String msg,
            T data
    ) {
        int statusCode =
                Integer.parseInt(resultCode.split("-", 2)[0]);

        RsData<T> tRsData =
                new RsData<>(resultCode, statusCode, msg, data);

        return tRsData;
    }

    // 상태 코드가 성공 범위인지 확인한다.
    @NonNull
    @JsonIgnore
    public boolean isSuccess() {
        return getStatusCode() >= 200
                && getStatusCode() < 400;
    }

    // 성공하지 않았다면 실패로 판단한다.
    @NonNull
    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }

    // 기존 결과 정보는 유지하면서 실제 데이터만 교체한다.
    public <T> RsData<T> newDataOf(T data) {
        return new RsData<>(
                resultCode,
                statusCode,
                msg,
                data
        );
    }
}
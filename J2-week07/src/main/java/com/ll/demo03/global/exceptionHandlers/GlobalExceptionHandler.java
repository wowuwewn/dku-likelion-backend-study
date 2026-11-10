package com.ll.demo03.global.exceptionHandlers;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.global.rq.Rq;
import com.ll.demo03.standard.dto.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final Rq rq;

    public record ErrorData(String path) {}

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<RsData<ErrorData>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(400, "400-JSON", "JSON 요청본문을 확인해주세요.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<RsData<ErrorData>> handleException(Exception ex) {
        // Spring MVC가 정한 404·405·415 등의 상태와 Allow 헤더를 보존한다.
        if (ex instanceof ErrorResponse frameworkError) {
            int status = frameworkError.getStatusCode().value();
            String msg = switch (status) {
                case 404 -> "요청 경로를 찾을 수 없습니다.";
                case 405 -> "지원하지 않는 HTTP 메서드입니다.";
                case 415 -> "지원하지 않는 Content-Type입니다.";
                default -> "요청을 처리할 수 없습니다.";
            };
            return ResponseEntity.status(status).headers(frameworkError.getHeaders())
                    .body(RsData.of(status + "-1", msg, new ErrorData(rq.getCurrentUrlPath())));
        }
        if (ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            return error(400, "400-TYPE", "요청 값의 형식을 확인해주세요.");
        }
        // 예외 메시지·본문·SQL·스택은 응답과 로그에 담지 않는다.
        log.error("요청 처리 실패: path={}, type={}", rq.getCurrentUrlPath(), ex.getClass().getSimpleName());
        return error(500, "500-1", "서버 오류가 발생했습니다.");
    }

    private ResponseEntity<RsData<ErrorData>> error(int status, String resultCode, String msg) {
        return ResponseEntity.status(status)
                .body(RsData.of(resultCode, msg, new ErrorData(rq.getCurrentUrlPath())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleValidationException(MethodArgumentNotValidException ex) {
        // 여러 필드가 틀리면 이름순 첫 오류를 반환해 응답을 일정하게 유지한다.
        var error = ex.getBindingResult().getFieldErrors().stream()
                .sorted(java.util.Comparator.comparing(org.springframework.validation.FieldError::getField))
                .findFirst().orElseThrow();
        return handleGlobalException(new GlobalException("400-" + error.getCode(),
                error.getField() + " : " + error.getDefaultMessage()));
    }

    @ExceptionHandler(GlobalException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleGlobalException(GlobalException ex) {
        RsData<Empty> rsData = ex.getRsData();
        return ResponseEntity.status(rsData.getStatusCode()).body(rsData);
    }
}

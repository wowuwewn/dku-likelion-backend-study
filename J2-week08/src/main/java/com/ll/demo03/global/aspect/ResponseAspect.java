package com.ll.demo03.global.aspect;

import com.ll.demo03.global.rq.Rq;
import com.ll.demo03.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ResponseAspect {
    private final Rq rq;

    // JSON Controller의 반환값만 처리한다. Service의 RsData는 대상이 아니다.
    @Around("@within(org.springframework.web.bind.annotation.RestController) || "
            + "@annotation(org.springframework.web.bind.annotation.ResponseBody)")
    public Object handleResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        // 예외가 나면 이 줄에 도달하지 않고 ControllerAdvice로 전달된다.
        if (result instanceof RsData<?> rsData) {
            rq.setStatusCode(rsData.getStatusCode());
        }
        // HTML·List·ResponseEntity 등은 각자의 기존 응답 처리를 따른다.
        return result;
    }
}

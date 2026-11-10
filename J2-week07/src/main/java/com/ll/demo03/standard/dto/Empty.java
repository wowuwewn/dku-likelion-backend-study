package com.ll.demo03.standard.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public class Empty {
    // 전역 Jackson 설정을 바꾸지 않고 빈 data를 {}로 직렬화한다.
    @JsonValue
    public Map<String, Object> toJson() {
        return Map.of();
    }
}

package com.ll.demo03.standard.dto.util;

public final class Ut {
    private Ut() {}

    // 강의 표기 Ut.str.isBlank(...)를 유지한다.
    public static final class str {
        private str() {}

        public static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        // 이 강의에서 hasLength는 공백을 제외한 내용이 있다는 뜻이다.
        public static boolean hasLength(String value) {
            return !isBlank(value);
        }
    }
}

package com.ll.demo03;

import com.ll.demo03.standard.dto.util.Ut;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class UtTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void blankIncludesNullAndWhitespace(String value) {
        assertThat(Ut.str.isBlank(value)).isTrue();
        assertThat(Ut.str.hasLength(value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", " 한글 "})
    void hasLengthMeansNonBlank(String value) {
        assertThat(Ut.str.isBlank(value)).isFalse();
        assertThat(Ut.str.hasLength(value)).isTrue();
    }
}

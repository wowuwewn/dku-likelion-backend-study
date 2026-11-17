package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MemberServiceTests {
    private final MemberRepository repository = mock(MemberRepository.class);
    private final MemberService service = new MemberService(repository);

    @Test
    void duplicateThrowsBeforeSave() {
        when(repository.findByUsername("user1")).thenReturn(Optional.of(Member.builder().build()));
        assertThatThrownBy(() -> service.join("user1", "example-password", "중복"))
                .isInstanceOf(GlobalException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(
                        ((GlobalException) ex).getRsData().getResultCode()).isEqualTo("400-1"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,pw,nick", "' ',pw,nick", "name,NULL,nick", "name,' ',nick", "name,pw,NULL", "name,pw,' '"}, nullValues = "NULL")
    void invalidInputThrowsWithoutRepositoryAccess(String username, String password, String nickname) {
        assertThatThrownBy(() -> service.join(username, password, nickname))
                .isInstanceOf(GlobalException.class);
        verifyNoInteractions(repository);
    }
}

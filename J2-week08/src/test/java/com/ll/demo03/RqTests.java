package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rq.Rq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RqTests {
    private final MemberService service = mock(MemberService.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Rq rq = new Rq(service, request, new MockHttpServletResponse());

    @Test
    void successfulAuthenticationCachesLookupAndPasswordCheck() {
        Member member = spy(Member.builder().username("learner").password("example-only").build());
        request.setParameter("actorUsername", "learner");
        request.setParameter("actorPassword", "example-only");
        when(service.findByUsername("learner")).thenReturn(Optional.of(member));
        assertThat(rq.getMember()).isSameAs(member);
        assertThat(rq.getMember()).isSameAs(member);
        verify(service, times(1)).findByUsername("learner");
        verify(member, times(1)).getPassword();
    }

    @ParameterizedTest
    @CsvSource({"missing,401-3", "wrong,401-4"})
    void failedAuthenticationIsAlsoCached(String mode, String code) {
        request.setParameter("actorUsername", "learner");
        request.setParameter("actorPassword", "incorrect-example");
        Member member = spy(Member.builder().password("example-only").build());
        when(service.findByUsername("learner")).thenReturn(
                mode.equals("missing") ? Optional.empty() : Optional.of(member));
        Throwable first = catchThrowable(rq::getMember);
        assertThat(first).isInstanceOf(GlobalException.class);
        assertThat(((GlobalException) first).getRsData().getResultCode()).isEqualTo(code);
        // 실패 후 파라미터가 바뀌어도 이 요청의 인증 결과가 성공으로 바뀌지 않는다.
        request.setParameter("actorPassword", "example-only");
        assertThat(catchThrowable(rq::getMember)).isSameAs(first);
        verify(service, times(1)).findByUsername("learner");
        verify(member, times(mode.equals("missing") ? 0 : 1)).getPassword();
    }

    @Test
    void missingCredentialsFailWithoutDatabaseAccess() {
        Throwable first = catchThrowable(rq::getMember);
        assertThat(first).isInstanceOf(GlobalException.class);
        assertThat(catchThrowable(rq::getMember)).isSameAs(first);
        verifyNoInteractions(service);
    }

    @Test
    void infrastructureFailureIsNotCachedAsAuthenticationFailure() {
        request.setParameter("actorUsername", "learner");
        request.setParameter("actorPassword", "example-only");
        when(service.findByUsername("learner")).thenThrow(new IllegalStateException("db unavailable"));
        assertThatThrownBy(rq::getMember).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(rq::getMember).isInstanceOf(IllegalStateException.class);
        verify(service, times(2)).findByUsername("learner");
    }
}

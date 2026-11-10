package com.ll.demo03;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:db_member_api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberApiTests {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;
    @org.springframework.boot.test.mock.mockito.SpyBean MemberService memberService;
    @Autowired ApiV1MemberController controller;
    @Autowired SurlRepository surlRepository;
    @Autowired ArticleService articleService;

    @Test
    void previousUrlFeaturesStillWork() throws Exception {
        var response = mvc.perform(get("/add").param("body", "regression")
                        .param("url", "https://example.com/chapter08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andReturn().getResponse();
        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response.getContentAsString()).path("data").path("id").asLong();
        mvc.perform(get("/all")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.body == 'regression')]").isNotEmpty());
        mvc.perform(get("/g/{id}", id)).andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/chapter08"));
        assertThat(surlRepository.findById(id).orElseThrow().getCount()).isEqualTo(1);
        mvc.perform(get("/g/9223372036854775807")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-0"));
        mvc.perform(get("/g/not-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-TYPE"));
    }

    @Test
    void htmlEscapesStoredTitle() throws Exception {
        articleService.write(memberRepository.findByUsername("user1").orElseThrow(),
                "<script>alert('test')</script>", "본문");
        mvc.perform(get("/article/list"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))));
    }

    @Test
    void aspectUsesReturnedStatusInsteadOfHardcodedSuccess() throws Exception {
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(controller)).isTrue();
        var result = com.ll.demo03.global.rsData.RsData.of("202-1", "접수 테스트",
                Member.builder().username("aspect-probe").nickname("상태").build());
        org.mockito.Mockito.doReturn(result).when(memberService)
                .join("aspect-probe", "example-password", "상태");
        mvc.perform(post("/api/v1/members").contentType("application/json")
                        .content("""
                                {"username":"aspect-probe","password":"example-password","nickname":"상태"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.statusCode").value(202))
                .andExpect(jsonPath("$.resultCode").value("202-1"));
    }

    @Test
    void malformedJsonIsDifferentFromValidationFailure() throws Exception {
        for (String body : new String[]{"{", "", "null"}) {
            mvc.perform(post("/api/v1/members").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.resultCode").value("400-JSON"))
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.data.path").value("/api/v1/members"))
                    .andExpect(jsonPath("$.data.trace").doesNotExist());
        }
    }

    @Test
    void frameworkErrorsKeepTheirHttpStatus() throws Exception {
        mvc.perform(get("/api/v1/members"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(jsonPath("$.statusCode").value(405));
        mvc.perform(post("/api/v1/members").contentType("text/plain").content("test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.statusCode").value(415));
        mvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void unexpectedExceptionDoesNotExposeInternalDetails() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("secret-password SQL jdbc:internal"))
                .when(memberService).join("runtime-failure", "example-password", "오류");
        mvc.perform(post("/api/v1/members?token=secret-token").contentType("application/json")
                        .content("""
                                {"username":"runtime-failure","password":"example-password","nickname":"오류"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resultCode").value("500-1"))
                .andExpect(jsonPath("$.statusCode").value(500))
                .andExpect(jsonPath("$.msg").value("서버 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data.path").value("/api/v1/members"))
                .andExpect(jsonPath("$.data.trace").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @ParameterizedTest
    @CsvSource({"username,missing", "username,blank", "username,null", "username,empty",
            "password,missing", "password,blank", "password,null", "password,empty",
            "nickname,missing", "nickname,blank", "nickname,null", "nickname,empty"})
    void requiredFieldsAreValidatedBeforeService(String field, String mode) throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var body = mapper.createObjectNode().put("username", "invalid-member")
                .put("password", "example-password").put("nickname", "검증");
        if (mode.equals("missing")) body.remove(field);
        else if (mode.equals("null")) body.putNull(field);
        else body.put(field, mode.equals("empty") ? "" : " \t\n");
        long count = memberRepository.count();
        mvc.perform(post("/api/v1/members").contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-NotBlank"))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.startsWith(field + " : ")))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(org.springframework.web.bind.MethodArgumentNotValidException.class));
        assertThat(memberRepository.count()).isEqualTo(count);
    }

    @Test
    void jsonBodyIsMappedAndStored() throws Exception {
        mvc.perform(post("/api/v1/members").contentType("application/json")
                        .content("""
                                {"username":"json-member","password":"example-password","nickname":"JSON 회원"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.msg").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.item.username").value("json-member"))
                .andExpect(jsonPath("$.data.item.nickname").value("JSON 회원"))
                .andExpect(jsonPath("$.data.item.length()").value(3))
                .andExpect(jsonPath("$.data.item.id").isNumber())
                .andExpect(jsonPath("$..password").isEmpty());
        assertThat(memberRepository.findByUsername("json-member")).isPresent();
        mvc.perform(get("/api/v1/members")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void articlesHaveHtmlAndJsonRepresentations() throws Exception {
        mvc.perform(get("/article/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>게시물 목록</h1>")));
        mvc.perform(get("/article/listJson"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].author").doesNotExist());
    }

    @Test
    void duplicateIsHandledCentrally() throws Exception {
        long count = memberRepository.count();
        mvc.perform(post("/join").param("username", "user1")
                        .param("password", "example-password").param("nickname", "중복"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.msg").value("user1(은)는 이미 존재하는 아이디입니다."))
                .andExpect(jsonPath("$.data").isEmpty());
        assertThat(memberRepository.count()).isEqualTo(count);
    }

    @Test
    void formJoinStoresMemberWithoutExposingPassword() throws Exception {
        mvc.perform(post("/join").param("username", "form-member")
                        .param("password", "example-password").param("nickname", "폼 회원"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.item.username").value("form-member"))
                .andExpect(jsonPath("$..password").isEmpty());
        Member member = memberRepository.findByUsername("form-member").orElseThrow();
        assertThat(member.getNickname()).isEqualTo("폼 회원");
        assertThat(member.getPassword()).isEqualTo("example-password");
    }

    @Test
    void getCannotCreateMember() throws Exception {
        long count = memberRepository.count();
        mvc.perform(get("/join").param("username", "get-member"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(memberRepository.count()).isEqualTo(count);
    }
}

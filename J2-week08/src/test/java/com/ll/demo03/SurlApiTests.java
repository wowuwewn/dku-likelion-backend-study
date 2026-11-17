package com.ll.demo03;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.demo03.global.rq.Rq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 테스트 트랜잭션을 걸지 않는다. 실제 서비스 커밋과 OSIV=false의 경계를 검증한다.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:db_surl_api;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SurlApiTests.ProbeConfig.class)
class SurlApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @SpyBean SurlRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @SpyBean MemberService memberService;

    @BeforeEach
    void cleanSurls() {
        repository.deleteAll();
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, String username) {
        return request.param("actorUsername", username).param("actorPassword", "1234");
    }

    private long add(String username, String body) throws Exception {
        var result = mvc.perform(asUser(post("/api/v1/surls"), username)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new ApiV1SurlController.SurlAddReqBody(body, "https://example.com/original"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.msg", matchesPattern("[0-9]+번 SURL이 생성되었습니다\\.")))
                .andExpect(jsonPath("$.data.item.authorName").value(username.equals("user1") ? "유저 1" : "유저 2"))
                .andExpect(jsonPath("$.data.item.author").doesNotExist())
                .andExpect(jsonPath("$..password").isEmpty()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/item/id").asLong();
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,NULL,401-1", "user1,NULL,401-2", "NULL,1234,401-1",
            "' ',1234,401-1", "user1,' ',401-2", "unknown-example,1234,401-3", "user1,wrong-example,401-4"}, nullValues = "NULL")
    void invalidAuthentication(String username, String password, String code) throws Exception {
        var request = get("/api/v1/surls");
        if (username != null) request.param("actorUsername", username);
        if (password != null) request.param("actorPassword", password);
        mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value(code))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(content().string(not(containsString("wrong-example"))));
    }

    @Test
    void registrationStoresAuthenticatedAuthorAndReturnsOnlyDto() throws Exception {
        long id = add("user1", "등록 확인");
        assertThat(jdbc.queryForObject("select m.username from surl s join member m on m.id=s.author_id where s.id=?",
                String.class, id)).isEqualTo("user1");
        mvc.perform(asUser(get("/api/v1/surls/{id}", id), "user1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.item.length()").value(8))
                .andExpect(jsonPath("$.data.item.body").value("등록 확인"))
                .andExpect(jsonPath("$.data.item.createDate").isString())
                .andExpect(jsonPath("$..password").isEmpty())
                .andExpect(content().string(not(containsString("1234"))));
    }

    @Test
    void listContainsOnlyOwnSurlsInDescendingOrderWithOsivDisabled() throws Exception {
        assertThat(environment.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        long first = add("user1", "첫째");
        add("user2", "다른 회원");
        long last = add("user1", "둘째");
        mvc.perform(asUser(get("/api/v1/surls"), "user1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(last))
                .andExpect(jsonPath("$.data.items[1].id").value(first))
                .andExpect(jsonPath("$.data.items[*].authorName", everyItem(is("유저 1"))));
    }

    @Test
    void emptyListIsSuccessful() throws Exception {
        mvc.perform(asUser(get("/api/v1/surls"), "user2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void dirtyCheckingCommitsModificationAndAuditingWithoutSave() throws Exception {
        long id = add("user1", "수정 전");
        clearInvocations(repository);
        var originalDate = jdbc.queryForObject("select modify_date from surl where id=?", java.time.LocalDateTime.class, id);
        mvc.perform(asUser(put("/api/v1/surls/{id}", id), "user1").contentType("application/json")
                        .content("{\"body\":\"수정 후\",\"url\":\"https://example.com/changed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.item.body").value("수정 후"))
                .andExpect(jsonPath("$.msg").value(id + "번 SURL이 수정되었습니다."));
        assertThat(jdbc.queryForObject("select body from surl where id=?", String.class, id)).isEqualTo("수정 후");
        assertThat(jdbc.queryForObject("select url from surl where id=?", String.class, id)).isEqualTo("https://example.com/changed");
        verify(repository, never()).save(any());
        assertThat(jdbc.queryForObject("select modify_date from surl where id=?", java.time.LocalDateTime.class, id))
                .isAfterOrEqualTo(originalDate);
        mvc.perform(asUser(get("/api/v1/surls/{id}", id), "user1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.item.body").value("수정 후"));
    }

    @Test
    void ownerCanDelete() throws Exception {
        long id = add("user1", "삭제");
        mvc.perform(asUser(delete("/api/v1/surls/{id}", id), "user1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        assertThat(repository.existsById(id)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void otherMemberCannotReadModifyOrDelete(String method) throws Exception {
        long id = add("user1", "보호할 자료");
        var request = switch (method) {
            case "PUT" -> put("/api/v1/surls/{id}", id).contentType("application/json")
                    .content("{\"body\":\"탈취\",\"url\":\"https://example.com/other\"}");
            case "DELETE" -> delete("/api/v1/surls/{id}", id);
            default -> get("/api/v1/surls/{id}", id);
        };
        mvc.perform(asUser(request, "user2")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
        assertThat(jdbc.queryForObject("select body from surl where id=?", String.class, id)).isEqualTo("보호할 자료");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void nonexistentSurlReturns404(String method) throws Exception {
        var request = switch (method) {
            case "PUT" -> put("/api/v1/surls/9223372036854775807").contentType("application/json")
                    .content("{\"body\":\"없음\",\"url\":\"https://example.com\"}");
            case "DELETE" -> delete("/api/v1/surls/9223372036854775807");
            default -> get("/api/v1/surls/9223372036854775807");
        };
        mvc.perform(asUser(request, "user1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIST", "GET", "POST", "PUT", "DELETE", "ADD", "SHORTEN", "ALL"})
    void everyProtectedRouteRequiresCredentials(String route) throws Exception {
        long id = add("user1", "원본");
        var request = switch (route) {
            case "GET" -> get("/api/v1/surls/{id}", id);
            case "POST" -> post("/api/v1/surls").contentType("application/json")
                    .content("{\"body\":\"새 자료\",\"url\":\"https://example.com\"}");
            case "PUT" -> put("/api/v1/surls/{id}", id).contentType("application/json")
                    .content("{\"body\":\"변경\",\"url\":\"https://example.com\"}");
            case "DELETE" -> delete("/api/v1/surls/{id}", id);
            case "ADD" -> get("/add").param("body", "새 자료").param("url", "https://example.com");
            case "SHORTEN" -> get("/s/title/https://example.com");
            case "ALL" -> get("/all");
            default -> get("/api/v1/surls");
        };
        mvc.perform(request).andExpect(status().isUnauthorized());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select body from surl where id=?", String.class, id)).isEqualTo("원본");
    }

    @ParameterizedTest
    @CsvSource({"POST,body", "POST,url", "PUT,body", "PUT,url"})
    void writeValidatesRequiredFields(String method, String field) throws Exception {
        long id = add("user1", "그대로");
        var body = mapper.createObjectNode().put("body", "변경").put("url", "https://example.com");
        body.put(field, " ");
        var request = method.equals("POST") ? post("/api/v1/surls") : put("/api/v1/surls/{id}", id);
        mvc.perform(asUser(request, "user1").contentType("application/json").content(body.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.resultCode").value("400-NotBlank"));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select body from surl where id=?", String.class, id)).isEqualTo("그대로");
    }

    @Test
    void legacyListCannotExposeAnotherMembersData() throws Exception {
        add("user1", "개인 자료");
        mvc.perform(asUser(get("/all"), "user2")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actorPassword", "%61ctorPassword"})
    void legacyShortenStripsCredentialsButPreservesOriginalQueryAndPublicRedirect(String passwordKey) throws Exception {
        // String 기반 MockMvc builder는 경로의 //를 정규화하므로 원문 URI를 전달한다.
        var result = mvc.perform(get(java.net.URI.create("/s/search/https://example.com/search?q=spring&actorUsername=user1&" + passwordKey + "=1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://example.com/search?q=spring"))
                .andExpect(content().string(not(containsString("1234"))))
                .andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
        mvc.perform(get("/g/{id}", id)).andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/search?q=spring"));
        assertThat(jdbc.queryForObject("select count from surl where id=?", Long.class, id)).isEqualTo(1L);
    }

    @Test
    void legacyAuthorlessSurlDoesNotGrantOwnership() throws Exception {
        long id = repository.save(Surl.builder().body("작성자 없음").url("https://example.com").build()).getId();
        mvc.perform(asUser(get("/api/v1/surls/{id}", id), "user1"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    void requestScopeCachesWithinRequestAndSeparatesUsersAcrossRequests() throws Exception {
        clearInvocations(memberService);
        mvc.perform(asUser(get("/test/auth-repeat"), "user1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.same").value(true));
        mvc.perform(asUser(get("/test/auth-repeat"), "user2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("user2"));
        mvc.perform(get("/test/auth-repeat")).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/auth-repeat").param("actorUsername", "user1").param("actorPassword", "bad-example"))
                .andExpect(status().isUnauthorized());
        mvc.perform(asUser(get("/test/auth-repeat"), "user1")).andExpect(status().isOk());
        verify(memberService, times(3)).findByUsername("user1");
        verify(memberService, times(1)).findByUsername("user2");
    }

    // 반복 호출을 관찰하기 위한 테스트 전용 경로. 운영 애플리케이션에는 등록되지 않는다.
    @TestConfiguration
    static class ProbeConfig {
        @Bean AuthProbe authProbe(Rq rq) { return new AuthProbe(rq); }
    }

    @RestController
    static class AuthProbe {
        private final Rq rq;
        AuthProbe(Rq rq) { this.rq = rq; }
        public record Result(String username, boolean same) {}
        @GetMapping("/test/auth-repeat")
        public Result repeat() {
            Member first = rq.getMember();
            return new Result(first.getUsername(), first == rq.getMember());
        }
    }
}

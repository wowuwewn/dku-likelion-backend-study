package com.ll.demo03;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// MockMvc 밖의 실제 Tomcat·HTTP 소켓·트랜잭션 커밋까지 확인한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:db_member_http;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MemberHttpTests {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void realHttpRequestCommitsMemberAndRejectsDuplicate() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            URI uri = URI.create("http://localhost:" + port + "/api/v1/members");
            var request = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {"username":"http-member","password":"example-password","nickname":"HTTP 회원"}
                            """)).build();
            long before = jdbc.queryForObject("select count(*) from member", Long.class);
            var success = client.send(request, HttpResponse.BodyHandlers.ofString());
            var body = mapper.readTree(success.body());
            assertThat(success.statusCode()).isEqualTo(201);
            assertThat(body.path("statusCode").asInt()).isEqualTo(201);
            assertThat(body.at("/data/item/username").asText()).isEqualTo("http-member");
            assertThat(success.body()).doesNotContain("password", "example-password");
            assertThat(jdbc.queryForObject("select nickname from member where username = ?",
                    String.class, "http-member")).isEqualTo("HTTP 회원");
            assertThat(jdbc.queryForObject("select count(*) from member", Long.class)).isEqualTo(before + 1);

            var duplicate = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(duplicate.statusCode()).isEqualTo(400);
            assertThat(mapper.readTree(duplicate.body()).path("resultCode").asText()).isEqualTo("400-1");
            assertThat(jdbc.queryForObject("select count(*) from member", Long.class)).isEqualTo(before + 1);
        }
    }
}

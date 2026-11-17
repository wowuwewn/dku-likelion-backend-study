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

// 실제 Tomcat 소켓 → 인증 → 서비스 트랜잭션 → DB 커밋 → JSON 응답을 검증한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:db_surl_http;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class SurlHttpTests {
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private HttpResponse<String> send(HttpClient client, String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (body == null) request.method(method, HttpRequest.BodyPublishers.noBody());
        else request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void joinedMemberCanUseCrudAndEveryHttpRequestAuthenticatesIndependently() throws Exception {
        String credentials = "?actorUsername=http-learner&actorPassword=example-only";
        try (HttpClient client = HttpClient.newHttpClient()) {
            var joined = send(client, "POST", "/api/v1/members",
                    "{\"username\":\"http-learner\",\"password\":\"example-only\",\"nickname\":\"HTTP learner\"}");
            assertThat(joined.statusCode()).isEqualTo(201);
            assertThat(joined.body()).doesNotContain("password", "example-only");

            var added = send(client, "POST", "/api/v1/surls" + credentials,
                    "{\"body\":\"HTTP original\",\"url\":\"https://example.com/original\"}");
            assertThat(added.statusCode()).isEqualTo(200);
            long id = mapper.readTree(added.body()).at("/data/item/id").asLong();
            assertThat(id).isPositive();
            String path = "/api/v1/surls/" + id;
            assertThat(send(client, "GET", path + credentials, null).statusCode()).isEqualTo(200);
            // 같은 HttpClient의 이전 요청에서 인증됐어도 다음 요청에 자동 로그인되지 않는다.
            assertThat(send(client, "GET", path, null).statusCode()).isEqualTo(401);
            assertThat(send(client, "GET", path + "?actorUsername=http-learner&actorPassword=bad-example", null).statusCode())
                    .isEqualTo(401);
            assertThat(send(client, "GET", path + "?actorUsername=user2&actorPassword=1234", null).statusCode()).isEqualTo(403);
            assertThat(send(client, "GET", "/api/v1/surls" + credentials, null).body()).contains("HTTP original");

            var changed = send(client, "PUT", path + credentials,
                    "{\"body\":\"HTTP changed\",\"url\":\"https://example.com/changed\"}");
            assertThat(changed.statusCode()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select body from surl where id=?", String.class, id)).isEqualTo("HTTP changed");
            var redirect = send(client, "GET", "/g/" + id, null);
            assertThat(redirect.statusCode()).isEqualTo(302);
            assertThat(redirect.headers().firstValue("Location")).contains("https://example.com/changed");
            assertThat(jdbc.queryForObject("select count from surl where id=?", Long.class, id)).isEqualTo(1);
            assertThat(send(client, "DELETE", path + credentials, null).statusCode()).isEqualTo(200);
            assertThat(jdbc.queryForObject("select count(*) from surl where id=?", Long.class, id)).isZero();
            assertThat(send(client, "GET", path + credentials, null).statusCode()).isEqualTo(404);
        }
    }
}

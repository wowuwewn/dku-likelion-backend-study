package com.ll.demo03;

import com.ll.demo03.global.rq.Rq;
import com.ll.demo03.global.rsData.RsData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class SurlController {
    private final SurlService surlService;
    private final Rq rq;

    // 이전 경로도 동일한 인증·목록 정책을 사용해 우회를 막는다.
    @GetMapping("/all")
    @ResponseBody
    public List<SurlDto> getAll() {
        return surlService.findByAuthorOrderByIdDesc(rq.getMember());
    }

    public record LegacyAddReq(@NotBlank String body, @NotBlank String url) {}

    @GetMapping("/add")
    @ResponseBody
    public RsData<SurlDto> add(@Valid @ModelAttribute LegacyAddReq req) {
        return surlService.add(rq.getMember(), req.body(), req.url());
    }

    @GetMapping("/s/{body}/**")
    @ResponseBody
    public RsData<SurlDto> add(@PathVariable String body, HttpServletRequest req) {
        Member member = rq.getMember();
        String url = req.getRequestURI().split("/", 4)[3];
        if (req.getQueryString() != null) url += "?" + req.getQueryString();
        // 원본 검색어는 보존하되 인증 파라미터는 저장·응답·리다이렉트하지 않는다.
        var builder = UriComponentsBuilder.fromUriString(url);
        for (String key : builder.build(true).getQueryParams().keySet()) {
            String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
            if (decodedKey.equals("actorUsername") || decodedKey.equals("actorPassword"))
                builder.replaceQueryParam(key);
        }
        url = builder.build(true).toUriString();
        return surlService.add(member, body, url);
    }

    @GetMapping("/g/{id}")
    public String go(@PathVariable long id) {
        return "redirect:" + surlService.getRedirectUrl(id);
    }
}

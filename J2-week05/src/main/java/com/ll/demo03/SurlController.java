package com.ll.demo03;

import com.ll.demo03.global.exceptions.GlobalException;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.global.rq.Rq;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SurlController {
    // 컨트롤러의 리스트 대신 서비스가 DB 저장과 조회를 담당한다.
    private final SurlService surlService;
    private final Rq rq;

    @GetMapping("/all")
    @ResponseBody
    public List<Surl> getAll() {
        return surlService.findAll();
    }

    @GetMapping("/add")
    @ResponseBody
    public RsData<Surl> add(String body, String url) {
        Member member = rq.getMember();

        // 강의의 프록시 실습: ID 접근과 username 접근의 SQL 실행 시점을 비교한다.
        System.out.println("before get id");
        member.getId();
        System.out.println("after get id");

        System.out.println("before get username");
        member.getUsername();
        System.out.println("after get username");

        return surlService.add(member, body, url);
    }

    @GetMapping("/s/{body}/**")
    @ResponseBody
    public RsData<Surl> add(
            @PathVariable String body,
            HttpServletRequest req
    ) {
        Member member = rq.getMember();

        // 경로 뒤의 원본 URL과 쿼리 문자열을 함께 저장한다.
        String url = req.getRequestURI();

        if (req.getQueryString() != null) {
            url += "?" + req.getQueryString();
        }

        String[] urlBits = url.split("/", 4);
        url = urlBits[3];

        return surlService.add(member, body, url);
    }

    @GetMapping("/g/{id}")
    public String go(@PathVariable long id) {
        Surl surl = surlService.findById(id).orElseThrow(GlobalException.E404::new);

        // 이동 횟수 변경을 서비스에 맡긴 뒤 원본 URL로 리다이렉트한다.
        surlService.increaseCount(surl);

        return "redirect:" + surl.getUrl();
    }
}

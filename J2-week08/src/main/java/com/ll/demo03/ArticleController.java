package com.ll.demo03;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

// 08-06: 같은 게시물 정보를 HTML과 JSON으로 각각 표현한다.
@Controller
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    public record ArticleListItem(Long id, String title) {}

    @GetMapping(value = "/article/listJson", produces = "application/json")
    @ResponseBody
    public List<ArticleListItem> showListJson() {
        // 게시물 목록에 필요 없는 회원 엔티티·비밀번호는 응답에 넣지 않는다.
        return articleService.findAll().stream()
                .map(article -> new ArticleListItem(article.getId(), article.getTitle())).toList();
    }

    @GetMapping(value = "/article/list", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String showList() {
        StringBuilder html = new StringBuilder("<h1>게시물 목록</h1><ul>");
        for (Article article : articleService.findAll()) {
            html.append("<li>").append(article.getId()).append(" | ")
                    .append(HtmlUtils.htmlEscape(article.getTitle())).append("</li>");
        }
        return html.append("</ul>").toString();
    }
}

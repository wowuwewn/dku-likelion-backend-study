package com.ll.demo03;

import com.ll.demo03.global.rq.Rq;
import com.ll.demo03.global.rsData.RsData;
import com.ll.demo03.standard.dto.Empty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/surls")
@RequiredArgsConstructor
public class ApiV1SurlController {
    private final SurlService surlService;
    private final Rq rq;

    public record SurlAddReqBody(@NotBlank String body, @NotBlank String url) {}
    public record SurlModifyReqBody(@NotBlank String body, @NotBlank String url) {}
    public record SurlAddRespBody(SurlDto item) {}
    public record SurlGetRespBody(SurlDto item) {}
    public record SurlGetItemsRespBody(List<SurlDto> items) {}
    public record SurlModifyRespBody(SurlDto item) {}

    @PostMapping(consumes = "application/json")
    public RsData<SurlAddRespBody> add(@RequestBody @Valid SurlAddReqBody reqBody) {
        var result = surlService.add(rq.getMember(), reqBody.body(), reqBody.url());
        return result.newDataOf(new SurlAddRespBody(result.getData()));
    }

    @GetMapping("/{id}")
    public RsData<SurlGetRespBody> get(@PathVariable long id) {
        return RsData.of(new SurlGetRespBody(surlService.get(rq.getMember(), id)));
    }

    @GetMapping
    public RsData<SurlGetItemsRespBody> getItems() {
        return RsData.of(new SurlGetItemsRespBody(surlService.findByAuthorOrderByIdDesc(rq.getMember())));
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    public RsData<SurlModifyRespBody> modify(@PathVariable long id,
                                            @RequestBody @Valid SurlModifyReqBody reqBody) {
        var result = surlService.modify(rq.getMember(), id, reqBody.body(), reqBody.url());
        return result.newDataOf(new SurlModifyRespBody(result.getData()));
    }

    @DeleteMapping("/{id}")
    public RsData<Empty> delete(@PathVariable long id) {
        return surlService.delete(rq.getMember(), id);
    }
}

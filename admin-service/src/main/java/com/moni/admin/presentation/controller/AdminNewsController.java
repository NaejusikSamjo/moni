package com.moni.admin.presentation.controller;

import com.moni.admin.application.service.AdminNewsService;
import com.moni.admin.infrastructure.client.dto.request.NewsCreateRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final AdminNewsService adminNewsService;

    @ModelAttribute("successMsg")
    public String successMsg() { return null; }

    @ModelAttribute("errorMsg")
    public String errorMsg() { return null; }

    @GetMapping
    public String news() {
        return "admin/news";
    }

    @GetMapping("/register")
    public String newsRegister() {
        return "admin/news-register";
    }

    @PostMapping("/fetch")
    public String fetchAllNews(RedirectAttributes redirectAttributes) {
        try {
            long start = System.currentTimeMillis();
            adminNewsService.fetchAllNews();
            long elapsed = (System.currentTimeMillis() - start) / 1000;
            redirectAttributes.addFlashAttribute("successMsg", "기업 뉴스 수집이 완료되었습니다. (소요 시간: " + elapsed + "초)");
        } catch (FeignException e) {
            log.error("[fetchAllNews] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 수집 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[fetchAllNews] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 수집 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/news";
    }

    @PostMapping("/market/fetch")
    public String fetchMarketNews(RedirectAttributes redirectAttributes) {
        try {
            long start = System.currentTimeMillis();
            adminNewsService.fetchMarketNews();
            long elapsed = (System.currentTimeMillis() - start) / 1000;
            redirectAttributes.addFlashAttribute("successMsg", "시장 뉴스 수집이 완료되었습니다. (소요 시간: " + elapsed + "초)");
        } catch (FeignException e) {
            log.error("[fetchMarketNews] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 수집 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[fetchMarketNews] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 수집 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/news";
    }

    @PostMapping("/register")
    public String registerNews(
            @RequestParam String ticker,
            @RequestParam String title,
            @RequestParam String companyName,
            @RequestParam String content,
            @RequestParam String source,
            @RequestParam String url,
            @RequestParam String publishedAt,
            RedirectAttributes redirectAttributes) {
        try {
            NewsCreateRequest request = NewsCreateRequest.builder()
                    .ticker(ticker)
                    .title(title)
                    .companyName(companyName)
                    .content(content)
                    .source(source)
                    .url(url)
                    .publishedAt(LocalDateTime.parse(publishedAt))
                    .build();
            adminNewsService.createNews(request);
            redirectAttributes.addFlashAttribute("successMsg", "뉴스가 등록되었습니다.");
        } catch (FeignException e) {
            log.error("[registerNews] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 등록 중 오류가 발생했습니다. (ticker/회사명 불일치 또는 중복)");
        } catch (Exception e) {
            log.error("[registerNews] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "뉴스 등록 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/news/register";
    }
}

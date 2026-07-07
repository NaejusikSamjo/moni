package com.moni.admin.presentation.controller;

import com.moni.admin.application.service.AdminStockService;
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

@Slf4j
@Controller
@RequestMapping("/admin/stocks")
@RequiredArgsConstructor
public class AdminStockController {

    private static final String STOCK_DOWNLOAD_CONFIRM_PHRASE = "확인합니다";

    private final AdminStockService adminStockService;

    @ModelAttribute("successMsg")
    public String successMsg() { return null; }

    @ModelAttribute("errorMsg")
    public String errorMsg() { return null; }

    @GetMapping
    public String stocks() {
        return "admin/stocks";
    }

    @PostMapping("/download")
    public String downloadStocks(
            @RequestParam String confirmation,
            RedirectAttributes redirectAttributes) {
        if (!STOCK_DOWNLOAD_CONFIRM_PHRASE.equals(confirmation)) {
            redirectAttributes.addFlashAttribute("errorMsg", "확인 문구가 올바르지 않습니다.");
            return "redirect:/admin/stocks";
        }
        try {
            long start = System.currentTimeMillis();
            adminStockService.downloadStocks();
            long elapsed = (System.currentTimeMillis() - start) / 1000;
            redirectAttributes.addFlashAttribute("successMsg", "주식 데이터 다운로드가 완료되었습니다. (소요 시간: " + elapsed + "초)");
        } catch (FeignException e) {
            log.error("[downloadStocks] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "주식 데이터 다운로드 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[downloadStocks] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "주식 데이터 다운로드 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/stocks";
    }
}

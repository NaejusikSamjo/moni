package com.moni.admin.presentation.controller;

import com.moni.admin.application.service.AdminFacadeService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;


@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

    private final AdminFacadeService adminFacadeService;

    @ModelAttribute("successMsg")
    public String successMsg() { return null; }

    @ModelAttribute("errorMsg")
    public String errorMsg() { return null; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            var page = adminFacadeService.getUsers(Pageable.ofSize(1));
            model.addAttribute("totalUsers", page.getTotalElements());
        } catch (FeignException e) {
            model.addAttribute("totalUsers", "-");
        }
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Model model) {
        model.addAttribute("page", adminFacadeService.getUsers(pageable));
        return "admin/users";
    }

    @GetMapping("/users/deleted")
    public String deletedUsers(
            @PageableDefault(sort = "deletedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Model model) {
        model.addAttribute("page", adminFacadeService.getDeletedUsers(pageable));
        return "admin/deleted-users";
    }

    @PostMapping("/users/{userId}/suspend")
    public String suspend(
            @PathVariable UUID userId,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        if (reason == null || reason.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMsg", "정지 사유를 입력해주세요.");
            return "redirect:/admin/users";
        }
        try {
            adminFacadeService.suspendUser(userId, reason);
            redirectAttributes.addFlashAttribute("successMsg", "계정이 정지되었습니다.");
        } catch (FeignException e) {
            log.error("[suspend] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[suspend] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/unsuspend")
    public String unsuspend(
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes) {
        try {
            adminFacadeService.unsuspendUser(userId);
            redirectAttributes.addFlashAttribute("successMsg", "계정 정지가 해제되었습니다.");
        } catch (FeignException e) {
            log.error("[unsuspend] {} status={} msg={}", e.getClass().getSimpleName(), e.status(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("[unsuspend] unexpected exception", e);
            redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/delete")
    public String delete(
            @PathVariable UUID userId,
            RedirectAttributes redirectAttributes) {
        try {
            adminFacadeService.deleteUser(userId);
            redirectAttributes.addFlashAttribute("successMsg", "사용자가 삭제되었습니다.");
        } catch (FeignException e) {
            redirectAttributes.addFlashAttribute("errorMsg", "처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users";
    }

}
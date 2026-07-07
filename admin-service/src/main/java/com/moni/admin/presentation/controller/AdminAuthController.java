package com.moni.admin.presentation.controller;

import com.moni.admin.application.service.AdminUserService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminUserService adminUserService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String logout, Model model) {
        if (logout != null) {
            model.addAttribute("logoutMsg", "로그아웃 되었습니다.");
        }
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            var page = adminUserService.getUsers(Pageable.ofSize(1));
            model.addAttribute("totalUsers", page.getTotalElements());
        } catch (FeignException e) {
            model.addAttribute("totalUsers", "-");
        }
        return "admin/dashboard";
    }
}

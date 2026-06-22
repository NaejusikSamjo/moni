package com.moni.user.admin.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SuspendRequest {

    @NotBlank(message = "정지 사유는 필수입니다.")
    private String reason;
}

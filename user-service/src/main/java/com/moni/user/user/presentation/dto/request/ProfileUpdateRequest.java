package com.moni.user.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class ProfileUpdateRequest {

    @NotBlank(message = "프로필 값은 비어 있을 수 없습니다.")
    @Pattern(
            regexp = "^(https://cdn\\.moni\\.my/[^\\s]+|[^\\x00-\\x7F].{0,9})$",
            message = "이모지 또는 이미지 URL만 허용됩니다."
    )
    private String profile;
}

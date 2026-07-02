package com.moni.user.user.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class ProfileUpdateRequest {

    @Pattern(
            regexp = "^(https://cdn\\.moni\\.my/[^\\s]+|[^\\x00-\\x7F].{0,9})$",
            message = "이모지 또는 이미지 URL만 허용됩니다."
    )
    private String profile;
}

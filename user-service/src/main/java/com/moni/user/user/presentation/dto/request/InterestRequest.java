package com.moni.user.user.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class InterestRequest {

    @NotEmpty(message = "관심사는 1개 이상 입력해야 합니다.")
    private List<String> categories;
}
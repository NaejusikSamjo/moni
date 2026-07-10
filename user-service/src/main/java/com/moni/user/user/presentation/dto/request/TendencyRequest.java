package com.moni.user.user.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TendencyRequest {

    @NotNull(message = "설문 점수는 필수입니다.")
    @Min(value = 0, message = "점수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "점수는 100 이하여야 합니다.")
    private Integer score;
}
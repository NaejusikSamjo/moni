package com.moni.ai.presentation.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class NewsSearchReqDto {
    @Size(max = 6, message = "ticker는 6자 이내여야 합니다.")
    private String ticker;

    @Size(max = 50, message = "companyName은 50자 이내여야 합니다.")
    private String companyName;

    @Size(max = 50, message = "keyword는 50자 이내여야 합니다.")
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "date는 오늘 이전 날짜여야 합니다.")
    private LocalDate date;

    public LocalDate getDate() {
        return date != null ? date : LocalDate.now();
    }
}

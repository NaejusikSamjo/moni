package com.moni.user.user.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private String presignedUrl;
    private String s3Url;

    public static PresignedUrlResponse of(String presignedUrl, String s3Url) {
        return PresignedUrlResponse.builder()
                .presignedUrl(presignedUrl)
                .s3Url(s3Url)
                .build();
    }
}

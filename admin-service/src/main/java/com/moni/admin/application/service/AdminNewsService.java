package com.moni.admin.application.service;

import com.moni.admin.infrastructure.client.AiAdminClient;
import com.moni.admin.infrastructure.client.dto.request.NewsCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminNewsService {

    private final AiAdminClient aiAdminClient;

    public void fetchAllNews() {
        aiAdminClient.fetchAllNews();
    }

    public void fetchMarketNews() {
        aiAdminClient.fetchMarketNews();
    }

    public void createNews(NewsCreateRequest request) {
        aiAdminClient.createNews(request);
    }
}

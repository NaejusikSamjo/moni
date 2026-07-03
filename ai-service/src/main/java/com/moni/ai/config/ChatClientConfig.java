package com.moni.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final VectorStore vectorStore;

    @Value("classpath:prompts/ai-system-prompt.st")
    private Resource systemPromptResource;

    @Primary
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) throws IOException {


        QuestionAnswerAdvisor qnaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequestDecision()).build();

        return builder
                .defaultAdvisors(qnaAdvisor)
                .build();
    }

    @Bean(name = "portfolioChatClient")
    public ChatClient portfolioChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    private SearchRequest searchRequestDecision(){
        return SearchRequest.builder()
                .similarityThreshold(0.8d)
                .topK(10)
                .build();
    }
}

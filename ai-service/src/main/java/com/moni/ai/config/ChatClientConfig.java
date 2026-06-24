package com.moni.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private final VectorStore vectorStore;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder){


        QuestionAnswerAdvisor qnaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequestDecision()).build();

        return builder
                .defaultSystem(
                        """
                        당신은 주식 투자 분석 전문가입니다.
                        제공된 뉴스 컨텍스트를 기반으로 기업 이슈를 분석하고 요약해주세요.
                        컨텍스트에 없는 내용은 답변하지 마세요.
                        
                        다음 규칙을 따르십시오:
                        1. 반드시 사용자가 질문한 기업({ticker})에 대한 정보만 분석하세요
                        2. 다른 기업의 정보가 컨텍스트에 포함되어있어도, 그 기업 중심으로 말하지 마세요.
                        3. 해당 기업({ticker})과 직접적으로 관련된 뉴스만 사용하세요
                        4. 만약 문맥 상 답이 명확하지 않다면, 그냥 모른다고 말하세요.
                        5. "맥락에 따라..." 또는 "제공된 정보에 따르면.."과 같은 표현은 피하세요
                        6. 제공된 맥락 정보도 같이 알려주세요
                        7. 해당 기업, 뉴스에 대한 평가를 POSITIVE, NEGATIVE, NEUTRAL 3가지 중에 하나로 평가해줘
                        """
                )
                .defaultAdvisors(qnaAdvisor)
                .build();
    }

    private SearchRequest searchRequestDecision(){
        return SearchRequest.builder()
                .similarityThreshold(0.8d)
                .topK(10)
                .build();
    }
}

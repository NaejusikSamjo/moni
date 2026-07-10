package com.moni.payment.application.service;

import com.moni.common.response.paging.PageRes;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import com.moni.payment.presentation.dto.PaymentHistoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentQueryService 테스트")
class PaymentQueryServiceTest {

    @Mock
    private PaymentJpaRepository paymentJpaRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Test
    void 결제_내역_조회_시_findByUserIdPaged를_호출하고_PageRes를_반환한다() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        given(paymentJpaRepository.findByUserIdPaged(userId, pageable))
                .willReturn(new PageImpl<>(Collections.emptyList()));

        PageRes<PaymentHistoryResponse> result = paymentQueryService.execute(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        then(paymentJpaRepository).should().findByUserIdPaged(userId, pageable);
    }
}

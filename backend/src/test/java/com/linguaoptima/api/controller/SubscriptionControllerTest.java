package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.UpgradeRequest;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.dto.response.SubscriptionResponse;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.service.SubscriptionService;
import com.linguaoptima.api.service.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UsageService usageService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testGetSubscriptionAndUsage() {
        SubscriptionResponse subRes = SubscriptionResponse.builder().tier(SubscriptionTier.FREE).build();
        when(subscriptionService.getSubscription(user)).thenReturn(subRes);
        assertEquals(HttpStatus.OK, subscriptionController.getMySubscription(user).getStatusCode());

        UsageResponse usageRes = UsageResponse.builder().weekEvaluations(2).build();
        when(usageService.getUsage(user)).thenReturn(usageRes);
        assertEquals(HttpStatus.OK, subscriptionController.getUsage(user).getStatusCode());
    }

    @Test
    void testUpgradeAndDowngrade() {
        UpgradeRequest req = UpgradeRequest.builder().targetTier(SubscriptionTier.PREMIUM).paymentToken("tok").build();
        PaymentResultResponse payRes = PaymentResultResponse.builder().success(true).build();
        when(subscriptionService.upgrade(user, SubscriptionTier.PREMIUM, "tok")).thenReturn(payRes);

        assertEquals(HttpStatus.OK, subscriptionController.upgrade(user, req).getStatusCode());

        ResponseEntity<Void> downRes = subscriptionController.downgrade(user);
        assertEquals(HttpStatus.OK, downRes.getStatusCode());
        verify(subscriptionService).downgrade(user);
    }
}

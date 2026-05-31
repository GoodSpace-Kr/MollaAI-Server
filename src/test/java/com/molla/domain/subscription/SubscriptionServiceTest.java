package com.molla.domain.subscription;

import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionServiceTest {

    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DailyUsageCalculator dailyUsageCalculator = mock(DailyUsageCalculator.class);

    private final SubscriptionService subscriptionService = new SubscriptionService(
            subscriptionRepository,
            userRepository,
            dailyUsageCalculator
    );

    @Test
    void ensureDemoSubscriptionCreatesFreeSubscriptionWhenNoneExists() {
        User user = User.createByPhone("01012345678");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(subscriptionRepository.existsActiveByUserId(user.getId())).thenReturn(false);

        subscriptionService.ensureDemoPremiumSubscription(user.getId());

        var captor = forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getPlanType()).isEqualTo("premium");
        assertThat(saved.getDailyLimitMinutes()).isEqualTo(30);
        assertThat(saved.getExpiresAt()).isNull();
        assertThat(saved.getStatus()).isEqualTo("active");
    }

    @Test
    void ensureDemoSubscriptionSkipsWhenActiveSubscriptionExists() {
        User user = User.createByPhone("01012345678");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(subscriptionRepository.existsActiveByUserId(user.getId())).thenReturn(true);

        subscriptionService.ensureDemoPremiumSubscription(user.getId());

        verify(subscriptionRepository, never()).save(org.mockito.ArgumentMatchers.any(Subscription.class));
    }
}

package dev.rubasace.linkedin.games.ldrbot.metrics;

import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroupRepository;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Configuration
public class BusinessMetricsConfiguration {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramUserRepository telegramUserRepository;
    private final MeterRegistry meterRegistry;

    BusinessMetricsConfiguration(final TelegramGroupRepository telegramGroupRepository,
                                  final TelegramUserRepository telegramUserRepository,
                                  final MeterRegistry meterRegistry) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramUserRepository = telegramUserRepository;
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    private void registerGauges() {
        meterRegistry.gauge(MetricsConstants.GROUPS_ACTIVE, this, cfg -> cfg.telegramGroupRepository.countByActiveTrue());
        meterRegistry.gauge(MetricsConstants.USERS_TOTAL, this, cfg -> cfg.telegramUserRepository.count());
    }
}

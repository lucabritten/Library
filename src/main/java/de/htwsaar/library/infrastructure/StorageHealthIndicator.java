package de.htwsaar.library.infrastructure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
class StorageHealthIndicator implements HealthIndicator {

    @Override
    public @Nullable Health health() {
        long freeMb = new File("/tmp").getFreeSpace() / 1_048_576;
        return freeMb > 100
                ? Health.up().withDetail("freeMb", freeMb).build()
                : Health.down().withDetail("freeMb", freeMb).build();

    }
}

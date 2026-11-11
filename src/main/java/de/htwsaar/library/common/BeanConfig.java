package de.htwsaar.library.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;


@Configuration
public class BeanConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Europe/Berlin"));
    }
}

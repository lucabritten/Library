package de.htwsaar.library.loan;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoanMetricsTest {

    @Mock
    LoanRepo repo;

    SimpleMeterRegistry registry;

    LoanMetrics metrics;

    @BeforeEach
    void setup() {
        registry = new SimpleMeterRegistry();
        metrics = new LoanMetrics(registry, repo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"succeeded", "loan-limit-exceeded", "copy-not-available"})
    void knownCreatedOutcomes_areRegisteredWithZero(String outcome) {
        assertThat(registry.get("library.loans.created")
                .tag("outcome", outcome)
                .counter().count()).isEqualTo(0.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"late", "on_time"})
    void knownReturnedOutcomes_areRegisteredWithZero(String returnTime) {
        assertThat(registry.get("library.loans.returned")
                .tag("return_time", returnTime)
                .counter().count()).isEqualTo(0.0);
    }

    @Test
    void lendFailed_incrementsOnlyThatOutcome() {
        metrics.lendFailed("loan-limit-exceeded");
        assertThat(created("loan-limit-exceeded")).isEqualTo(1.0);
        assertThat(created("succeeded")).isEqualTo(0.0);
    }

    @Test
    void returned_overdue_countsLate() {
        metrics.returned(true);
        metrics.returned(true);
        metrics.returned(true);
        metrics.returned(false);
        metrics.returned(true);

        assertThat(registry.get("library.loans.returned").tag("return_time", "late").counter().count())
                .isEqualTo(4.0);

        assertThat(registry.get("library.loans.returned").tag("return_time", "on_time").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void openLoans_readsFromRepo() {
        when(repo.countByReturnedAtIsNull())
                .thenReturn(987L);

        assertThat(registry.get("library.loans.open").gauge().value())
                .isEqualTo(987);
    }

    private double created(String outcome) {
        return getCountFor("library.loans.created", outcome);
    }

    private double deleted(String returnTime) {
        return getCountFor("library.loans.returned", returnTime);
    }

    private double getCountFor(String path, String value) {
        return registry.get(path)
                .tag("outcome", value)
                .counter()
                .count();
    }

}

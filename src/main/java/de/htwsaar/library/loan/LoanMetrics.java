package de.htwsaar.library.loan;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class LoanMetrics {

    private final MeterRegistry meterRegistry;

    private final static String CREATED_COUNTER = "library.loans.created";
    private final static String RETURN_COUNTER = "library.loans.returned";
    private final static String OPEN_LOANS_GAUGE = "library.loans.open";

    private final LoanRepo loanRepo;

    LoanMetrics(MeterRegistry meterRegistry, LoanRepo loanRepo) {
        this.meterRegistry = meterRegistry;
        this.loanRepo = loanRepo;
        buildOpenLoans();

        for (String outcome : List.of("succeeded", "loan-limit-exceeded", "copy-not-available")) {
            Counter.builder(CREATED_COUNTER).tag("outcome", outcome).register(meterRegistry);
        }
        for (String returnTime : List.of("late", "on_time")) {
            Counter.builder(RETURN_COUNTER).tag("return_time", returnTime).register(meterRegistry);
        }
    }

    public void lendSucceeded() {
        Counter.builder(CREATED_COUNTER)
                .tag("outcome","succeeded")
                .register(meterRegistry)
                .increment();
    }

    public void lendFailed(String reason) {
        Counter.builder(CREATED_COUNTER)
                .tag("outcome", reason)
                .register(meterRegistry)
                .increment();

    }

    public void returned(boolean overdue) {
        String returnTime = overdue ? "late" : "on_time";
        Counter.builder(RETURN_COUNTER)
                .tag("return_time", returnTime)
                .register(meterRegistry)
                .increment();
    }

    private void buildOpenLoans() {
        Gauge.builder(
            OPEN_LOANS_GAUGE,
            loanRepo,
            LoanRepo::countByReturnedAtIsNull)
            .register(meterRegistry);
    }
}

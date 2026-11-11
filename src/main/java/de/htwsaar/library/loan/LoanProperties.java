package de.htwsaar.library.loan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Period;

@Validated
@ConfigurationProperties(prefix = "library.loans")
public record LoanProperties (
    @Min(1)
    @Max(10)
    @DefaultValue("5")
    int maxActiveLoans,

    @DefaultValue("P30D")
    Period loanPeriod
) {}

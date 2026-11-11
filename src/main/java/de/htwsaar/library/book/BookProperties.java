package de.htwsaar.library.book;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "library.books")
public record BookProperties (
    @DefaultValue("true")
    boolean normalizeIsbn
) {}

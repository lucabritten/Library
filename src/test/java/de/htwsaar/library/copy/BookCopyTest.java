package de.htwsaar.library.copy;

import de.htwsaar.library.common.Exceptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Consumer;

import static de.htwsaar.library.TestData.copyIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BookCopyTest {

    enum Action {
        LOAN(BookCopy::markLoaned),
        RETURN(BookCopy::markReturned),
        DAMAGE(BookCopy::markDamaged),
        REPAIR(BookCopy::markRepaired),
        WITHDRAW(BookCopy::markWithdrawn);

        final Consumer<BookCopy> apply;
        Action(Consumer<BookCopy> apply) { this.apply = apply; }
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @CsvSource({
            "AVAILABLE, LOAN,     LOANED",
            "LOANED,    RETURN,   AVAILABLE",
            "AVAILABLE, DAMAGE,   DAMAGED",
            "DAMAGED,   REPAIR,   AVAILABLE",
            "AVAILABLE, WITHDRAW, WITHDRAWN",
            "DAMAGED,   WITHDRAW, WITHDRAWN"
    })
    void allowedTransition_changesStatus(CopyStatus from, Action action, CopyStatus expected) {
        BookCopy copy = copyIn(from);

        action.apply.accept(copy);

        assertThat(copy.getStatus()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} --{1}--> verboten")
    @CsvSource({
            "AVAILABLE, RETURN",   "AVAILABLE, REPAIR",
            "LOANED,    LOAN",     "LOANED,    DAMAGE",   "LOANED, WITHDRAW", "LOANED, REPAIR",
            "DAMAGED,   LOAN",     "DAMAGED,   RETURN",   "DAMAGED, DAMAGE",
            "WITHDRAWN, LOAN",     "WITHDRAWN, RETURN",   "WITHDRAWN, DAMAGE",
            "WITHDRAWN, REPAIR",   "WITHDRAWN, WITHDRAW"
    })
    void forbiddenTransition_throwsAndKeepsStatus(CopyStatus from, Action action) {
        BookCopy copy = copyIn(from);

        assertThatThrownBy(() -> action.apply.accept(copy))
                .isInstanceOf(Exceptions.ConflictException.class);
        assertThat(copy.getStatus()).isEqualTo(from);
    }
}

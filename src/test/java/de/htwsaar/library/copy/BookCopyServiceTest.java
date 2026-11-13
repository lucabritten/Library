package de.htwsaar.library.copy;

import de.htwsaar.library.author.Author;
import de.htwsaar.library.book.Book;
import de.htwsaar.library.book.BookExceptions;
import de.htwsaar.library.book.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.htwsaar.library.TestData.copyIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookCopyServiceTest {

    @Mock
    BookCopyRepo copyRepo;

    @Mock
    BookService bookService;

    @InjectMocks
    BookCopyService copyService;

    private static final String INVENTORY_NUMBER = "BC-000001";

    @Test
    void require_noCopyWithNumber_throwsNotFound() {
        when(copyRepo.findByInventoryNumber(INVENTORY_NUMBER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> copyService.findByInventoryNumber(INVENTORY_NUMBER))
                .isInstanceOf(BookCopyExceptions.BookCopyNotFoundException.class);
    }

    @Test
    void create_noBookWithId_throwsBookNotFound() {
        when(bookService.require(123L))
                .thenThrow(BookExceptions.BookNotFoundException.class);

        assertThatThrownBy(() -> copyService.create(new BookCopyDto.CreateBookCopyRequest(123L)))
                .isInstanceOf(BookExceptions.BookNotFoundException.class);
        verify(copyRepo, never()).saveAndFlush(any());
    }

    @ParameterizedTest(name = "Sequenz {0} → {1}")
    @CsvSource({
            "1,       BC-000001",
            "100,     BC-000100",
            "999999,  BC-999999",
            "1000000, BC-1000000"
    })
    void create_formatsInventoryNumberFromSequence(long sequence, String expected) {
        Book book = new Book("Momo", "9783522202008", new Author("Michael", "Ende"));
        when(bookService.require(123L)).thenReturn(book);
        when(copyRepo.nextInventoryNumber()).thenReturn(sequence);
        when(copyRepo.saveAndFlush(any(BookCopy.class))).then(AdditionalAnswers.returnsFirstArg());

        BookCopyDto.BookCopyResponse response = copyService.create(new BookCopyDto.CreateBookCopyRequest(123L));

        assertThat(response.inventoryNumber()).isEqualTo(expected);
    }

    @Test
    void withdrawCopy_noCopyWithInventoryNumber_throwsNotFound() {
        when(copyRepo.findByInventoryNumber("BC-000001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> copyService.withdrawCopy("BC-000001"))
                .isInstanceOf(BookCopyExceptions.BookCopyNotFoundException.class);
    }

    @ParameterizedTest(name = "CopyStatus: {0}")
    @EnumSource(value = CopyStatus.class, names = {"AVAILABLE", "DAMAGED"}, mode = EnumSource.Mode.EXCLUDE)
    void withDrawCopy_copyNotAvailable_throwsNotAvailable(CopyStatus status) {
        when(copyRepo.findByInventoryNumber(any()))
                .thenReturn(Optional.of(copyIn(status)));

        assertThatThrownBy(() -> copyService.withdrawCopy("BC-000001"))
                .isInstanceOf(BookCopyExceptions.CopyNotAvailableException.class);
    }

    @Test
    void withDrawCopy_availableCopy_isMarked() {
        BookCopy copy = copyIn(CopyStatus.AVAILABLE);
        when(copyRepo.findByInventoryNumber("BC-000001"))
                .thenReturn(Optional.of(copy));

        copyService.withdrawCopy("BC-000001");

        assertThat(copy.getStatus()).isEqualTo(CopyStatus.WITHDRAWN);
    }
}

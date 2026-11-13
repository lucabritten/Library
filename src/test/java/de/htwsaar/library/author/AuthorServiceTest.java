package de.htwsaar.library.author;

import de.htwsaar.library.common.Exceptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceTest {

    @Mock
    AuthorRepo authorRepo;

    @InjectMocks
    AuthorService authorService;

    private final Pageable page = PageRequest.of(0,10);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", ""})
    void search_withoutName_usesFindAll(String lastName) {
        when(authorRepo.findAll(page)).thenReturn(Page.empty());

        authorService.search(lastName, page);

        verify(authorRepo).findAll(page);
        verify(authorRepo, never()).findAllByLastNameContainingIgnoreCase(any(), any());
    }

    @Test
    void search_withName_usesFilteredQuery() {
        String lastName = "Meyer";
        when(authorRepo.findAllByLastNameContainingIgnoreCase(lastName, page))
                .thenReturn(Page.empty());

        authorService.search(lastName, page);

        verify(authorRepo).findAllByLastNameContainingIgnoreCase(lastName, page);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        long id = 123L;
        when(authorRepo.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.delete(id))
                .isInstanceOf(Exceptions.NotFoundException.class);

        verify(authorRepo, never()).delete(any());
    }

    @Test
    void delete_authorWithBooks_throwsAuthorInUse() {
        long id = 123L;
        when(authorRepo.findById(id))
                .thenReturn(Optional.of(new Author("Fritz", "Walter")));
        doThrow(DataIntegrityViolationException.class).when(authorRepo).flush();

        assertThatThrownBy(() -> authorService.delete(id))
                .isInstanceOf(AuthorExceptions.AuthorInUseException.class);
    }

    @Test
    void require_noAuthorWithId_throwsAuthorNotFound() {
        long id = 123L;
        when(authorRepo.findById(id))
                .thenThrow(AuthorExceptions.AuthorNotFoundException.class);

        assertThatThrownBy(() -> authorService.require(id))
                .isInstanceOf(AuthorExceptions.AuthorNotFoundException.class);
    }

    @Test
    void findById_noAuthorWithId_throwsAuthorNotFound() {
        long id = 123L;
        when(authorRepo.findById(id))
                .thenThrow(AuthorExceptions.AuthorNotFoundException.class);

        assertThatThrownBy(() -> authorService.findById(id))
                .isInstanceOf(AuthorExceptions.AuthorNotFoundException.class);
    }
}



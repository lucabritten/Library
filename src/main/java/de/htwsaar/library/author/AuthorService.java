package de.htwsaar.library.author;

import de.htwsaar.library.author.AuthorDtos.*;
import de.htwsaar.library.author.AuthorExceptions.*;
import de.htwsaar.library.common.Dtos.PageResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuthorService {

    private final static Logger logger =
            LoggerFactory.getLogger(AuthorService.class);

    private final AuthorRepo authorRepo;

    public AuthorService(AuthorRepo authorRepo) {
        this.authorRepo = authorRepo;
    }

    public AuthorResponse findById(Long id) {
        return AuthorResponse.from(require(id));
    }

    public PageResponse<AuthorResponse> search(String lastName, Pageable pageable) {
        Page<Author> authorPage;
        if (StringUtils.hasText(lastName))
            authorPage = authorRepo.findAllByLastNameContainingIgnoreCase(lastName, pageable);
        else
            authorPage = authorRepo.findAll(pageable);

        return PageResponse.from(authorPage.map(AuthorResponse::from));
    }

    @Transactional
    public AuthorResponse create(CreateAuthorRequest req) {
        // Add validation author already exists?
            Author saved = authorRepo.save(new Author(req.firstName(), req.lastName()));

            return AuthorResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Author author = require(id);

        try {
            authorRepo.delete(author);
            authorRepo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AuthorInUseException(author.getId());
        }
    }

    //Schnittstellen-Methode zu BookService
    public Author require(Long id) {
        return authorRepo.findById(id)
                .orElseThrow(() -> {
                    logger.info("Author with id={} not found.", id);
                    return new AuthorExceptions.AuthorNotFoundException(id);
                });
    }
}

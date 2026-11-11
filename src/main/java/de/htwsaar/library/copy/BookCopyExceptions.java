package de.htwsaar.library.copy;

import de.htwsaar.library.common.Exceptions;

public class BookCopyExceptions {

    public static class CopyNotAvailableException extends Exceptions.ConflictException {
        public CopyNotAvailableException(String message) {
            super("copy-not-available",message);
        }

        public CopyNotAvailableException(Long id, String inventoryNumber) {
            this("Copy with id=" + id + " and inventory_number=" + inventoryNumber + " not available.");
        }
    }

    public static class BookCopyNotFoundException extends Exceptions.NotFoundException {
        public BookCopyNotFoundException(String inventoryNumber) {
            super("Book with inventory-number=" + inventoryNumber + " not found.");
        }
    }

    public static class DamagedBookReturnException extends Exceptions.ConflictException {
        public DamagedBookReturnException(String inventoryNumber) {
            super("damaged-book-return", "Book with inventory-number= "+ inventoryNumber + " current status damaged cannot be returned");
        }
    }
}

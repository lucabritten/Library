ALTER TABLE book_copy DROP CONSTRAINT book_copy_book_id_fkey;
ALTER TABLE book_copy ADD CONSTRAINT book_copy_book_id_fkey
    FOREIGN KEY (book_id) REFERENCES books(id);
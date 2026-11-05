CREATE TABLE BOOKS(
    NAME varchar(100) NOT NULL,
    AUTHOR varchar(100) NOT NULL,
    PRIMARY KEY (NAME)
);

INSERT INTO BOOKS (NAME, AUTHOR) VALUES
                                     ('Harry Potter und der Stein der Weisen', 'J.K. Rowling'),
                                     ('Harry Potter und die Kammer des Schreckens', 'J.K. Rowling'),
                                     ('Der Hobbit', 'J.R.R. Tolkien'),
                                     ('Der Herr der Ringe: Die Gefährten', 'J.R.R. Tolkien'),
                                     ('Shining', 'Stephen King'),
                                     ('Es', 'Stephen King'),
                                     ('A Game of Thrones', 'George R.R. Martin'),
                                     ('A Clash of Kings', 'George R.R. Martin'),
                                     ('Mord im Orient-Express', 'Agatha Christie'),
                                     ('Tod auf dem Nil', 'Agatha Christie'),
                                     ('Illuminati', 'Dan Brown'),
                                     ('Da Vinci Code – Sakrileg', 'Dan Brown'),
                                     ('Stolz und Vorurteil', 'Jane Austen'),
                                     ('Verstand und Gefühl', 'Jane Austen'),
                                     ('Die Tribute von Panem – Tödliche Spiele', 'Suzanne Collins'),
                                     ('Die Tribute von Panem – Catching Fire', 'Suzanne Collins'),
                                     ('Percy Jackson – Diebe im Olymp', 'Rick Riordan'),
                                     ('Percy Jackson – Im Bann des Zyklopen', 'Rick Riordan'),
                                     ('Momo', 'Michael Ende'),
                                     ('Die unendliche Geschichte', 'Michael Ende');
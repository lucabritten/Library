CREATE SEQUENCE author_seq INCREMENT BY 50;
CREATE SEQUENCE book_seq INCREMENT BY 50;

CREATE TABLE AUTHORS(
                        ID BIGINT PRIMARY KEY,
                        FIRST_NAME VARCHAR(100) NOT NULL,
                        LAST_NAME VARCHAR(200) NOT NULL
);

CREATE TABLE BOOKS(
                      ID     BIGINT PRIMARY KEY,
                      TITLE  VARCHAR(300) NOT NULL,
                      ISBN   VARCHAR(13)  NOT NULL UNIQUE,
                      AUTHOR_ID BIGINT NOT NULL REFERENCES AUTHORS(ID)
);

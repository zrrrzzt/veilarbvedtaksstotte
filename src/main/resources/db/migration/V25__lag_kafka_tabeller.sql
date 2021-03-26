CREATE SEQUENCE KAFKA_CONSUMER_RECORD_ID_SEQ;

CREATE TABLE KAFKA_CONSUMER_RECORD
(
    ID               BIGINT       NOT NULL PRIMARY KEY,
    TOPIC            VARCHAR(100) NOT NULL,
    PARTITION        INTEGER      NOT NULL,
    RECORD_OFFSET    BIGINT       NOT NULL,
    RETRIES          INTEGER      NOT NULL DEFAULT 0,
    LAST_RETRY       TIMESTAMP,
    KEY              BYTEA        NOT NULL,
    VALUE            BYTEA        NOT NULL,
    HEADERS_JSON     TEXT,
    RECORD_TIMESTAMP BIGINT,
    CREATED_AT       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (TOPIC, PARTITION, RECORD_OFFSET)
);

CREATE SEQUENCE KAFKA_PRODUCER_RECORD_ID_SEQ;

CREATE TABLE KAFKA_PRODUCER_RECORD
(
    ID           BIGINT                              NOT NULL PRIMARY KEY,
    TOPIC        VARCHAR(100)                        NOT NULL,
    KEY          BYTEA                               NOT NULL,
    VALUE        BYTEA                               NOT NULL,
    HEADERS_JSON TEXT,
    CREATED_AT   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

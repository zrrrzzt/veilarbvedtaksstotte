CREATE TABLE UTRULLING (
    ENHET_ID                        VARCHAR(4) NOT NULL,
    CREATED_AT                      TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE(ENHET_ID)
);

CREATE INDEX UTRULLING_ENHET_ID_IDX ON UTRULLING(ENHET_ID);

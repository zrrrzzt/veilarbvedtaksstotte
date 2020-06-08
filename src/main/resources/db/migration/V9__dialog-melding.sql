CREATE TABLE DIALOG_MELDING (
    ID                      BIGSERIAL PRIMARY KEY NOT NULL,
    VEDTAK_ID               BIGINT NOT NULL,
    MELDING                 VARCHAR(1000),
    OPPRETTET               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    OPPRETTET_AV_IDENT      VARCHAR(20),
    FOREIGN KEY (VEDTAK_ID) REFERENCES VEDTAK(ID)
);

CREATE TABLE VEDTAK (
    ID                      BIGSERIAL PRIMARY KEY NOT NULL,
    AKTOR_ID                VARCHAR(20) NOT NULL,
    HOVEDMAL                VARCHAR(30),
    INNSATSGRUPPE           VARCHAR(40),
    VEILEDER_IDENT          VARCHAR(20) NOT NULL,
    VEILEDER_ENHET_ID       VARCHAR(4) NOT NULL,
    SIST_OPPDATERT          TIMESTAMP NOT NULL,
    BEGRUNNELSE             VARCHAR(4000),
    STATUS                  VARCHAR(10) NOT NULL,
    DOKUMENT_ID             VARCHAR(20),
    JOURNALPOST_ID          VARCHAR(20),
    GJELDENDE               BOOLEAN NOT NULL DEFAULT false,
    UTKAST_OPPRETTET        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    VEILEDER_ENHET_NAVN     VARCHAR(256),
    BESLUTTER_NAVN          VARCHAR(256),
    SENDT_TIL_BESLUTTER     BOOLEAN NOT NULL DEFAULT false
);


CREATE TABLE VEDTAK_SENDT_KAFKA_FEIL (
    ID                      BIGSERIAL PRIMARY KEY NOT NULL,
    VEDTAK_SENDT            TIMESTAMP NOT NULL,
    INNSATSGRUPPE           VARCHAR(40) NOT NULL,
    AKTOR_ID                VARCHAR(20) NOT NULL,
    ENHET_ID                VARCHAR(4) NOT NULL,
    VEDTAK_ID               INT NOT NULL,
    FOREIGN KEY (VEDTAK_ID) REFERENCES VEDTAK(ID)
);

CREATE TYPE OYBLIKKSBILDE_TYPE AS enum (
    'CV_OG_JOBBPROFIL',
    'REGISTRERINGSINFO',
    'EGENVURDERING'
);

CREATE TABLE OYBLIKKSBILDE (
    VEDTAK_ID               BIGINT NOT NULL,
    OYBLIKKSBILDE_TYPE      OYBLIKKSBILDE_TYPE NOT NULL,
    JSON                    JSON,
    FOREIGN KEY (VEDTAK_ID) REFERENCES VEDTAK(ID)
);

CREATE TABLE KILDE (
    VEDTAK_ID               BIGINT NOT NULL,
    TEKST                   VARCHAR(200) NOT NULL,
    FOREIGN KEY (VEDTAK_ID) REFERENCES VEDTAK(ID)
);

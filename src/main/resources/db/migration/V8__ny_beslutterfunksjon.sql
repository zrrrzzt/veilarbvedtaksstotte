ALTER TABLE VEDTAK
    DROP COLUMN SENDT_TIL_BESLUTTER;

ALTER TABLE VEDTAK
    DROP COLUMN BESLUTTER_NAVN;

ALTER TABLE VEDTAK
    ADD COLUMN BESLUTTER_IDENT VARCHAR(32);

ALTER TABLE VEDTAK
    ADD COLUMN BESLUTTER_PROSESS_STARTET BOOLEAN DEFAULT false;

ALTER TABLE VEDTAK
    ADD COLUMN GODKJENT_AV_BESLUTTER BOOLEAN DEFAULT false;
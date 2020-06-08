package no.nav.veilarbvedtaksstotte.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus;
import no.nav.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.domain.enums.VedtakStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class Vedtak {
    long id;
    String aktorId;
    Hovedmal hovedmal;
    Innsatsgruppe innsatsgruppe;
    VedtakStatus vedtakStatus;
    LocalDateTime sistOppdatert;
    LocalDateTime utkastOpprettet;
    String begrunnelse;
    String veilederIdent;
    String veilederNavn;
    String oppfolgingsenhetId;
    String oppfolgingsenhetNavn;
    String beslutterIdent;
    String beslutterNavn;
    boolean gjeldende;
    List<String> opplysninger;
    String journalpostId;
    String dokumentInfoId;
    BeslutterProsessStatus beslutterProsessStatus;

    @JsonIgnore
    boolean sender;
}
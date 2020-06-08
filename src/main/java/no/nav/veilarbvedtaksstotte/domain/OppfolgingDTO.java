package no.nav.veilarbvedtaksstotte.domain;

import lombok.Data;

import java.util.List;

@Data
public class OppfolgingDTO {

    private String servicegruppe;

    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;

}
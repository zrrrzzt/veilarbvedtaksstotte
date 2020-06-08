package no.nav.veilarbvedtaksstotte.service;

import lombok.SneakyThrows;
import no.nav.veilarbvedtaksstotte.client.api.DokumentClient;
import no.nav.veilarbvedtaksstotte.client.api.SafClient;
import no.nav.veilarbvedtaksstotte.domain.*;
import no.nav.veilarbvedtaksstotte.domain.dialog.SystemMeldingType;
import no.nav.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.veilarbvedtaksstotte.repository.BeslutteroversiktRepository;
import no.nav.veilarbvedtaksstotte.repository.KilderRepository;
import no.nav.veilarbvedtaksstotte.repository.MeldingRepository;
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static no.nav.veilarbvedtaksstotte.domain.enums.BeslutterProsessStatus.GODKJENT_AV_BESLUTTER;
import static no.nav.veilarbvedtaksstotte.utils.InnsatsgruppeUtils.skalHaBeslutter;

@Service
public class VedtakService {

    private final VedtaksstotteRepository vedtaksstotteRepository;
    private final OyeblikksbildeService oyeblikksbildeService;
    private final KilderRepository kilderRepository;
    private final MeldingRepository meldingRepository;
    private final BeslutteroversiktRepository beslutteroversiktRepository;
    private final AuthService authService;
    private final DokumentClient dokumentClient;
    private final SafClient safClient;
    private final VeilederService veilederService;
    private final MalTypeService malTypeService;
    private final VedtakStatusEndringService vedtakStatusEndringService;
    private final MetricsService metricsService;
    private final TransactionTemplate transactor;

    @Autowired
    public VedtakService(
            VedtaksstotteRepository vedtaksstotteRepository,
            KilderRepository kilderRepository,
            OyeblikksbildeService oyeblikksbildeService,
            MeldingRepository meldingRepository,
            BeslutteroversiktRepository beslutteroversiktRepository,
            AuthService authService,
            DokumentClient dokumentClient,
            SafClient safClient,
            VeilederService veilederService,
            MalTypeService malTypeService,
            VedtakStatusEndringService vedtakStatusEndringService,
            MetricsService metricsService,
            TransactionTemplate transactor
    ) {
        this.vedtaksstotteRepository = vedtaksstotteRepository;
        this.kilderRepository = kilderRepository;
        this.oyeblikksbildeService = oyeblikksbildeService;
        this.meldingRepository = meldingRepository;
        this.beslutteroversiktRepository = beslutteroversiktRepository;
        this.authService = authService;
        this.dokumentClient = dokumentClient;
        this.safClient = safClient;
        this.veilederService = veilederService;
        this.malTypeService = malTypeService;
        this.vedtakStatusEndringService = vedtakStatusEndringService;
        this.metricsService = metricsService;
        this.transactor = transactor;
    }

    @SneakyThrows
    public DokumentSendtDTO sendVedtak(String fnr) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();

        Vedtak vedtak = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);

        authService.sjekkAnsvarligVeileder(vedtak);

        Vedtak gjeldendeVedtak = vedtaksstotteRepository.hentGjeldendeVedtak(aktorId);
        validerUtkastForUtsending(vedtak, gjeldendeVedtak);

        long vedtakId = vedtak.getId();

        oyeblikksbildeService.lagreOyeblikksbilde(fnr, vedtakId);

        DokumentSendtDTO dokumentSendt = sendDokument(vedtak, fnr);

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(aktorId);
            vedtaksstotteRepository.ferdigstillVedtak(vedtakId, dokumentSendt);
            beslutteroversiktRepository.slettBruker(vedtakId);
        });

        vedtakStatusEndringService.vedtakSendt(vedtak, fnr);

        return dokumentSendt;
    }

    private DokumentSendtDTO sendDokument(Vedtak vedtak, String fnr) {
        // Oppdaterer vedtak til "sender" tilstand for å redusere risiko for dupliserte utsendelser av dokument.
        vedtaksstotteRepository.oppdaterSender(vedtak.getId(), true);
        try {
            metricsService.rapporterSendDokument();
            SendDokumentDTO sendDokumentDTO = lagDokumentDTO(vedtak, fnr);
            return dokumentClient.sendDokument(sendDokumentDTO);
        } catch (Exception e) {
            vedtaksstotteRepository.oppdaterSender(vedtak.getId(), false);
            throw e;
        }
    }

    public void lagUtkast(String fnr) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        String aktorId = authKontekst.getAktorId();
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(aktorId);

        if (utkast != null) {
            throw new IllegalStateException(format("Kan ikke lage nytt utkast, bruker med aktorId %s har allerede et aktivt utkast", aktorId));
        }

        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        String oppfolgingsenhetId = authKontekst.getOppfolgingsenhet();

        vedtaksstotteRepository.opprettUtkast(aktorId, innloggetVeilederIdent, oppfolgingsenhetId);
        vedtakStatusEndringService.utkastOpprettet(vedtaksstotteRepository.hentUtkast(aktorId));

        Vedtak nyttUtkast = vedtaksstotteRepository.hentUtkast(aktorId);
        meldingRepository.opprettSystemMelding(nyttUtkast.getId(), SystemMeldingType.UTKAST_OPPRETTET, innloggetVeilederIdent);
    }

    public void oppdaterUtkast(String fnr, VedtakDTO vedtakDTO) {

        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);

        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(authKontekst.getAktorId());

        authService.sjekkAnsvarligVeileder(utkast);

        oppdaterUtkastFraDto(utkast, vedtakDTO);

        transactor.executeWithoutResult((status) -> {
            vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
            kilderRepository.slettKilder(utkast.getId());
            kilderRepository.lagKilder(vedtakDTO.getOpplysninger(), utkast.getId());
        });
    }

    private void oppdaterUtkastFraDto(Vedtak utkast, VedtakDTO dto) {
        utkast.setInnsatsgruppe(dto.getInnsatsgruppe());
        utkast.setBegrunnelse(dto.getBegrunnelse());
        utkast.setOpplysninger(dto.getOpplysninger());
        if (dto.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            utkast.setHovedmal(null);
        } else {
            utkast.setHovedmal(dto.getHovedmal());
        }
    }

    public void slettUtkastForFnr(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        slettUtkast(aktorId);
    }

    public void slettUtkast(String aktorId) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(aktorId);
        long utkastId = utkast.getId();
        authService.sjekkAnsvarligVeileder(utkast);

        transactor.executeWithoutResult((status) -> {
            meldingRepository.slettMeldinger(utkastId);
            kilderRepository.slettKilder(utkastId);
            beslutteroversiktRepository.slettBruker(utkastId);
            kilderRepository.slettKilder(utkastId);
            vedtaksstotteRepository.slettUtkast(utkast.getAktorId());
        });

        vedtakStatusEndringService.utkastSlettet(utkast);
    }

    public List<Vedtak> hentVedtak(String fnr) {

        String aktorId = authService.sjekkTilgang(fnr).getAktorId();

        List<Vedtak> vedtak = vedtaksstotteRepository.hentVedtak(aktorId);

        flettInnVeilederNavn(vedtak);
        flettInnBeslutterNavn(vedtak);
        flettInnEnhetNavn(vedtak);

        return vedtak;
    }

    public byte[] produserDokumentUtkast(String fnr) {

        String aktorId = authService.sjekkTilgang(fnr).getAktorId();

        SendDokumentDTO sendDokumentDTO =
                Optional.ofNullable(vedtaksstotteRepository.hentUtkast(aktorId))
                        .map(vedtak -> lagDokumentDTO(vedtak, fnr))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fant ikke vedtak å forhandsvise for bruker"));

        return dokumentClient.produserDokumentUtkast(sendDokumentDTO);
    }

    public byte[] hentVedtakPdf(String fnr, String dokumentInfoId, String journalpostId) {
        authService.sjekkTilgang(fnr);
        return safClient.hentVedtakPdf(journalpostId, dokumentInfoId);
    }

    public boolean harUtkast(String fnr) {
        String aktorId = authService.sjekkTilgang(fnr).getAktorId();
        return vedtaksstotteRepository.hentUtkast(aktorId) != null;
    }

    public void behandleAvsluttOppfolging (KafkaAvsluttOppfolging melding ) {
        if (vedtaksstotteRepository.hentUtkast(melding.getAktorId()) != null) {
            slettUtkast(melding.getAktorId());
        }
        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(melding.getAktorId());
    }

    public void behandleOppfolgingsbrukerEndring(KafkaOppfolgingsbrukerEndring endring) {
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(endring.getAktorId());

        if (utkast != null && !utkast.getOppfolgingsenhetId().equals(endring.getOppfolgingsenhetId())) {
            vedtaksstotteRepository.oppdaterUtkastEnhet(utkast.getId(), endring.getOppfolgingsenhetId());
        }
    }

    public void taOverUtkast(String fnr) {
        AuthKontekst authKontekst = authService.sjekkTilgang(fnr);
        Vedtak utkast = vedtaksstotteRepository.hentUtkastEllerFeil(authKontekst.getAktorId());
        String innloggetVeilederIdent = authService.getInnloggetVeilederIdent();
        Veileder veileder = veilederService.hentVeileder(innloggetVeilederIdent);

        if (innloggetVeilederIdent.equals(utkast.getVeilederIdent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Veileder er allerede ansvarlig for utkast");
        }

        vedtaksstotteRepository.oppdaterUtkastVeileder(utkast.getId(), innloggetVeilederIdent);
        beslutteroversiktRepository.oppdaterVeileder(utkast.getId(), veileder.getNavn());
        vedtakStatusEndringService.tattOverForVeileder(utkast, innloggetVeilederIdent);
        meldingRepository.opprettSystemMelding(utkast.getId(), SystemMeldingType.TATT_OVER_SOM_VEILEDER, innloggetVeilederIdent);
    }

    private void flettInnVeilederNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            Veileder veileder = veilederService.hentVeileder(v.getVeilederIdent());
            v.setVeilederNavn(veileder != null ? veileder.getNavn() : null);
        });
    }

    private void flettInnBeslutterNavn(List<Vedtak> vedtak) {
        vedtak.stream()
                .filter(v -> v.getBeslutterIdent() != null)
                .forEach(v -> {
                    Veileder beslutter = veilederService.hentVeileder(v.getBeslutterIdent());
                    v.setBeslutterNavn(beslutter != null ? beslutter.getNavn() : null);
                });
    }

    private void flettInnEnhetNavn(List<Vedtak> vedtak) {
        vedtak.forEach(v -> {
            String enhetNavn = veilederService.hentEnhetNavn(v.getOppfolgingsenhetId());
            v.setOppfolgingsenhetNavn(enhetNavn);
        });
    }

    private SendDokumentDTO lagDokumentDTO(Vedtak vedtak, String fnr) {
        return new SendDokumentDTO()
                .setBegrunnelse(vedtak.getBegrunnelse())
                .setEnhetId(vedtak.getOppfolgingsenhetId())
                .setOpplysninger(vedtak.getOpplysninger())
                .setMalType(malTypeService.utledMalTypeFraVedtak(vedtak, fnr))
                .setBrukerFnr(fnr);
    }

    void validerUtkastForUtsending(Vedtak vedtak, Vedtak gjeldendeVedtak) {

        Innsatsgruppe innsatsgruppe = vedtak.getInnsatsgruppe();

        if (innsatsgruppe == null) {
            throw new IllegalStateException("Vedtak mangler innsatsgruppe");
        }

        boolean isGodkjentAvBeslutter = vedtak.getBeslutterProsessStatus() == GODKJENT_AV_BESLUTTER;

        if (skalHaBeslutter(innsatsgruppe)) {
            if (vedtak.getBeslutterIdent() == null) {
                throw new IllegalStateException("Vedtak kan ikke bli sendt uten beslutter");
            } else if (!isGodkjentAvBeslutter) {
                throw new IllegalStateException("Vedtak er ikke godkjent av beslutter");
            }
        }

        if (vedtak.getOpplysninger() == null || vedtak.getOpplysninger().isEmpty()) {
            throw new IllegalStateException("Vedtak mangler opplysninger");
        }

        if (vedtak.getHovedmal() == null && innsatsgruppe != Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak mangler hovedmål");
        } else if (vedtak.getHovedmal() != null && innsatsgruppe == Innsatsgruppe.VARIG_TILPASSET_INNSATS) {
            throw new IllegalStateException("Vedtak med varig tilpasset innsats skal ikke ha hovedmål");
        }

        boolean harIkkeBegrunnelse = vedtak.getBegrunnelse() == null || vedtak.getBegrunnelse().trim().isEmpty();
        boolean erStandard = innsatsgruppe == Innsatsgruppe.STANDARD_INNSATS;
        boolean erGjeldendeVedtakVarig =
                gjeldendeVedtak != null &&
                        (gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.VARIG_TILPASSET_INNSATS ||
                                gjeldendeVedtak.getInnsatsgruppe() == Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS);

        if (harIkkeBegrunnelse && erStandard && erGjeldendeVedtakVarig) {
            throw new IllegalStateException("Vedtak mangler begrunnelse siden gjeldende vedtak er varig");
        } else if (harIkkeBegrunnelse && !erStandard) {
            throw new IllegalStateException("Vedtak mangler begrunnelse");
        }

    }

}
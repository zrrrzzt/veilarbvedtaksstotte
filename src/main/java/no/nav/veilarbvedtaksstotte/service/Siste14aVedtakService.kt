package no.nav.veilarbvedtaksstotte.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.BrukerIdenter
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.ArenaVedtak.ArenaInnsatsgruppe
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak
import no.nav.veilarbvedtaksstotte.domain.vedtak.Siste14aVedtak.HovedmalMedOkeDeltakelse
import no.nav.veilarbvedtaksstotte.domain.vedtak.Vedtak
import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import no.nav.veilarbvedtaksstotte.utils.TimeUtils.toZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class Siste14aVedtakService(
    val transactor: TransactionTemplate,
    val kafkaProducerService: KafkaProducerService,
    val vedtakRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val authService: AuthService,
    val aktorOppslagClient: AktorOppslagClient,
    val arenaVedtakService: ArenaVedtakService
) {

    val log = LoggerFactory.getLogger(Siste14aVedtakService::class.java)

    fun siste14aVedtak(fnr: Fnr): Siste14aVedtak? {
        authService.sjekkTilgangTilBruker(fnr)
        val identer: BrukerIdenter = aktorOppslagClient.hentIdenter(fnr)
        return siste14aVedtakMedKilder(identer).siste14aVedtak
    }

    private data class Siste14aVedtakMedGrunnlag(
        val siste14aVedtak: Siste14aVedtak?,
        val arenaVedtak: List<ArenaVedtak>
    )

    private fun siste14aVedtakMedKilder(identer: BrukerIdenter): Siste14aVedtakMedGrunnlag {

        val sisteVedtak: Vedtak? = vedtakRepository.hentSisteVedtak(identer.aktorId.get())
        val arenaVedtakListe = arenaVedtakRepository.hentVedtakListe(identer.historiskeFnr.plus(identer.fnr))

        if (sisteVedtak == null && arenaVedtakListe.isEmpty()) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = null,
                arenaVedtak = arenaVedtakListe
            )
        }

        val sisteArenaVedtak = finnSisteArenaVedtak(arenaVedtakListe)

        // Siste vedtak fra denne løsningen
        if (
            (sisteVedtak != null && sisteArenaVedtak == null) ||
            (sisteVedtak != null && sisteArenaVedtak != null &&
                    sisteVedtak.vedtakFattet.isAfter(sisteArenaVedtak.beregnetFattetTidspunkt()))
        ) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = sisteVedtak.innsatsgruppe,
                    hovedmal = HovedmalMedOkeDeltakelse.fraHovedmal(sisteVedtak.hovedmal),
                    fattetDato = toZonedDateTime(sisteVedtak.vedtakFattet),
                    fraArena = false,
                ),
                arenaVedtak = arenaVedtakListe
            )

            // Siste vedtak fra Arena
        } else if (sisteArenaVedtak != null) {
            return Siste14aVedtakMedGrunnlag(
                siste14aVedtak = Siste14aVedtak(
                    aktorId = identer.aktorId,
                    innsatsgruppe = ArenaInnsatsgruppe.tilInnsatsgruppe(sisteArenaVedtak.innsatsgruppe),
                    hovedmal = HovedmalMedOkeDeltakelse.fraArenaHovedmal(sisteArenaVedtak.hovedmal),
                    fattetDato = toZonedDateTime(sisteArenaVedtak.fraDato.atStartOfDay()),
                    fraArena = true,
                ),
                arenaVedtak = arenaVedtakListe
            )
        }

        // Ingen vedtak
        return Siste14aVedtakMedGrunnlag(
            siste14aVedtak = null,
            arenaVedtak = arenaVedtakListe
        )
    }

    private fun finnSisteArenaVedtak(arenaVedtakListe: List<ArenaVedtak>): ArenaVedtak? {
        return arenaVedtakListe.stream().max(Comparator.comparing(ArenaVedtak::fraDato)).orElse(null)
    }

    fun behandleEndringFraArena(arenaVedtak: ArenaVedtak) {
        // Idempotent oppdatering/lagring av vedtak fra Arena
        arenaVedtakService.behandleVedtakFraArena(arenaVedtak)

        // Oppdatering som følger kan gjøres uavhengig av idempotent oppdatering/lagring over. Derfor er ikke
        // oppdateringene i samme transaksjon, og følgende oppdatering kunne f.eks. også vært kjørt uavhengig i en
        // scheduled task. Feilhåndtering her skjer indirekte via feilhåndtering av Kafka-meldinger som vil bli
        // behandlet på nytt ved feil.
        oppdaterKafkaSiste14aVedtak(arenaVedtak)
    }

    private fun oppdaterKafkaSiste14aVedtak(arenaVedtak: ArenaVedtak) {
        val identer = aktorOppslagClient.hentIdenter(arenaVedtak.fnr)
        val (siste14aVedtak, arenaVedtakListe) = siste14aVedtakMedKilder(identer)
        val fraArena = siste14aVedtak?.fraArena ?: false

        // hindrer at vi republiserer siste14aVedtak dersom eldre meldinger skulle bli konsumert:
        val sisteFraArena = finnSisteArenaVedtak(arenaVedtakListe)
        val erSisteFraArena = fraArena && sisteFraArena?.hendelseId == arenaVedtak.hendelseId

        if (erSisteFraArena) {
            kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)
        } else {
            log.info("""Publiserer ikke siste 14a vedtak basert på behandlet melding (fraArena=$fraArena, erSisteFraArena=$erSisteFraArena)
                |Behandlet melding har hendelseId=${arenaVedtak.hendelseId}
                |Siste melding har hendelseId=${sisteFraArena?.hendelseId}""".trimMargin())
        }
    }

    fun republiserKafkaSiste14aVedtak(eksernBrukerId: EksternBrukerId) {
        val identer = aktorOppslagClient.hentIdenter(eksernBrukerId)
        val (siste14aVedtak) = siste14aVedtakMedKilder(identer)
        val fraArena = siste14aVedtak?.fraArena ?: false
        kafkaProducerService.sendSiste14aVedtak(siste14aVedtak)
        log.info("Siste 14a vedtak republisert basert på vedtak fra {}.", if (fraArena) "Arena" else "vedtaksstøtte" )
    }
}
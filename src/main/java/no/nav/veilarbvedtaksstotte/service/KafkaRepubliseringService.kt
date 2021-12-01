package no.nav.veilarbvedtaksstotte.service

import no.nav.veilarbvedtaksstotte.repository.ArenaVedtakRepository
import no.nav.veilarbvedtaksstotte.repository.VedtaksstotteRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KafkaRepubliseringService(
    val vedtaksstotteRepository: VedtaksstotteRepository,
    val arenaVedtakRepository: ArenaVedtakRepository,
    val siste14aVedtakService: Siste14aVedtakService
) {

    val log: Logger = LoggerFactory.getLogger(KafkaRepubliseringService::class.java)

    fun republiserSiste14aVedtak() {

        val brukereFraVedtaksstotte = vedtaksstotteRepository.hentUnikeBrukereMedFattetVedtakPage()
        val brukereFraArena = arenaVedtakRepository.hentUnikeBrukereMedVedtak()

        val alleBrukere = brukereFraVedtaksstotte + brukereFraArena

        log.info(
            "Republiserer siste 14a vedtak for alle brukere som har vedtak i vedtaksstøtte og Arena." +
                    " Antall brukere i vedtaksstøtte=${brukereFraVedtaksstotte.size}" +
                    " Antall brukere i Arena=${brukereFraArena.size}"
        )

        alleBrukere.forEach { aktorId -> siste14aVedtakService.republiserKafkaSiste14aVedtak(aktorId) }
    }
}

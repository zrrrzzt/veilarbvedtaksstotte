package no.nav.veilarbvedtaksstotte.controller

import no.nav.common.types.identer.Fnr
import no.nav.veilarbvedtaksstotte.controller.dto.Siste14aVedtakDTO
import no.nav.veilarbvedtaksstotte.service.Siste14aVedtakService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class Siste14aVedtakController(val siste14aVedtakService: Siste14aVedtakService) {

    // TODO Skal tas bort når veilarbmaofs bruker /siste-14a-vedtak
    @GetMapping("/innsatsbehov")
    fun hentInnsatsbehov(@RequestParam("fnr") fnr: Fnr): ResponseEntity<Siste14aVedtakDTO> {
        return siste14aVedtakService.siste14aVedtak(fnr)
            ?.let { ResponseEntity(Siste14aVedtakDTO.fraSiste14aVedtak(it), HttpStatus.OK) }
            ?: ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @GetMapping("/siste-14a-vedtak")
    fun hentSiste14aVedtak(@RequestParam("fnr") fnr: Fnr): ResponseEntity<Siste14aVedtakDTO> {
        return siste14aVedtakService.siste14aVedtak(fnr)
            ?.let { ResponseEntity(Siste14aVedtakDTO.fraSiste14aVedtak(it), HttpStatus.OK) }
            ?: ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
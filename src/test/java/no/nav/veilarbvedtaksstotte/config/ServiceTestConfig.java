package no.nav.veilarbvedtaksstotte.config;

import no.nav.veilarbvedtaksstotte.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        ArenaVedtakService.class,
        AuthService.class,
        BeslutterService.class,
        BeslutteroversiktService.class,
        EnhetInfoService.class,
        MalTypeService.class,
        MeldingService.class,
        MetricsService.class,
        OyeblikksbildeService.class,
        VedtakService.class,
        VedtakHendelserService.class,
        VeilarbarenaService.class,
        VeilederService.class,
        DokumentService.class,
        DistribusjonService.class,
        UnleashService.class,
        UtrullingService.class,
        Siste14aVedtakService.class,
        KafkaProducerService.class,
        KafkaConsumerService.class,
        KafkaRepubliseringService.class,
        DvhRapporteringService.class,
        DvhRapporteringService.class,
        KafkaVedtakStatusEndringConsumer.class
})
public class ServiceTestConfig {
}

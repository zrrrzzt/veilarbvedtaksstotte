package no.nav.veilarbvedtaksstotte.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.veilarbvedtaksstotte.service.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.EnvironmentUtils.Type.PUBLIC;
import static no.nav.common.utils.EnvironmentUtils.getOptionalProperty;
import static no.nav.common.utils.EnvironmentUtils.setProperty;
import static no.nav.veilarbvedtaksstotte.config.KafkaConsumerConfig.OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME;

@Component
@Slf4j
public class OppfolgingsbrukerEndringConsumer {

    private static final String ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_OPPFOLGINGSBRUKER_TOPIC";
    private final VedtakService vedtakService;

    @Autowired
    public OppfolgingsbrukerEndringConsumer(VedtakService vedtakService, ConsumerParameters consumerParameters) {
        this.vedtakService = vedtakService;
        setProperty(ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME, consumerParameters.topic, PUBLIC);
    }

    @KafkaListener(
            topics = "${" + ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME + "}",
            containerFactory = OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME
    )
    public void consume(@Payload String kafkaMelding) {
        try {
            KafkaOppfolgingsbrukerEndring melding = fromJson(kafkaMelding, KafkaOppfolgingsbrukerEndring.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + getOptionalProperty(ENDRING_PAA_OPPFOLGINGSBRUKER_KAFKA_TOPIC_PROPERTY_NAME));
            vedtakService.behandleOppfolgingsbrukerEndring(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-melding", t);
        }
    }

    public static class ConsumerParameters {
        public final String topic;

        public ConsumerParameters(String topic) {
            this.topic = topic;
        }
    }
}

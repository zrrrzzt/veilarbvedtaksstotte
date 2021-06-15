package no.nav.veilarbvedtaksstotte.service;

import no.nav.common.kafka.producer.feilhandtering.KafkaProducerRecordStorage;
import no.nav.veilarbvedtaksstotte.config.KafkaProperties;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakSendt;
import no.nav.veilarbvedtaksstotte.domain.kafka.KafkaVedtakStatusEndring;
import no.nav.veilarbvedtaksstotte.domain.vedtak.Innsatsbehov;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;

import static no.nav.common.kafka.producer.util.ProducerUtils.toJsonProducerRecord;
import static no.nav.common.kafka.producer.util.ProducerUtils.toProducerRecord;
import static no.nav.veilarbvedtaksstotte.utils.JsonUtilsKt.toJson;

@Service
public class KafkaProducerService {
    final KafkaProducerRecordStorage<String, String> producerRecordStorage;
    final KafkaProperties kafkaProperties;

    public KafkaProducerService(
            KafkaProducerRecordStorage<String, String> producerRecordStorage,
            KafkaProperties kafkaProperties
    ) {
        this.producerRecordStorage = producerRecordStorage;
        this.kafkaProperties = kafkaProperties;
    }

    public void sendVedtakStatusEndring(KafkaVedtakStatusEndring vedtakStatusEndring) {
        ProducerRecord<String, String> producerRecord =
                toJsonProducerRecord(
                        kafkaProperties.getVedtakStatusEndringTopic(),
                        vedtakStatusEndring.getAktorId(),
                        vedtakStatusEndring
                );

        producerRecordStorage.store(producerRecord);
    }

    public void sendVedtakSendt(KafkaVedtakSendt vedtakSendt) {
        ProducerRecord<String, String> producerRecord =
                toJsonProducerRecord(
                        kafkaProperties.getVedtakSendtTopic(),
                        vedtakSendt.getAktorId(),
                        vedtakSendt
                );

        producerRecordStorage.store(producerRecord);
    }

    public void sendInnsatsbehov(Innsatsbehov innsatsbehov) {
        ProducerRecord<String, String> producerRecord =
                toProducerRecord(
                        kafkaProperties.getInnsatsbehovTopic(),
                        innsatsbehov.getAktorId().get(),
                        toJson(innsatsbehov)
                );

        producerRecordStorage.store(producerRecord);
    }
}

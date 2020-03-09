package no.nav.fo.veilarbvedtaksstotte.config;

import no.nav.fo.veilarbvedtaksstotte.domain.KafkaAvsluttOppfolging;
import no.nav.fo.veilarbvedtaksstotte.domain.KafkaOppfolgingsbrukerEndring;
import no.nav.fo.veilarbvedtaksstotte.kafka.AvsluttOppfolgingConsumer;
import no.nav.fo.veilarbvedtaksstotte.kafka.KafkaHelsesjekk;
import no.nav.fo.veilarbvedtaksstotte.kafka.OppfolgingsbrukerEndringConsumer;
import no.nav.fo.veilarbvedtaksstotte.service.VedtakService;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.HashMap;

import static no.nav.fo.veilarbvedtaksstotte.config.ApplicationConfig.KAFKA_BROKERS_URL_PROPERTY;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@EnableKafka
@Configuration
@Import({KafkaHelsesjekk.class, ServiceConfig.class})
public class KafkaConsumerConfig {

    private static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    public static final String AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME = "avsluttOppfolgingKafkaListenerContainerFactory";

    @Bean(name = AVSLUTT_OPPFOLGING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> avsluttOppfolgingKafkaListenerContainerFactory(KafkaHelsesjekk kafkaHelsesjekk) {
        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaProperties()));
        factory.getContainerProperties().setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public AvsluttOppfolgingConsumer avsluttOppfolgingConsumer(VedtakService vedtakService) {
        return new AvsluttOppfolgingConsumer(vedtakService);
    }

    public static final String OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME = "oppfolgingsbrukerEndringKafkaListenerContainerFactory";

    @Bean(name = OPPFOLGINGSBRUKER_ENDRING_CONTAINER_FACTORY_NAME)
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaOppfolgingsbrukerEndring>> oppfolgingsbrukerEndringKafkaListenerContainerFactory(KafkaHelsesjekk kafkaHelsesjekk) {
        ConcurrentKafkaListenerContainerFactory<String, KafkaOppfolgingsbrukerEndring> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaProperties()));
        factory.getContainerProperties().setErrorHandler(kafkaHelsesjekk);
        return factory;
    }

    @Bean
    public OppfolgingsbrukerEndringConsumer oppfolgingsbrukerEndringConsumer(VedtakService vedtakService) {
        return new OppfolgingsbrukerEndringConsumer(vedtakService);
    }

    private static HashMap<String, Object> kafkaProperties() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        props.put(GROUP_ID_CONFIG, "veilarbvedtaksstotte-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 5000);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }
}

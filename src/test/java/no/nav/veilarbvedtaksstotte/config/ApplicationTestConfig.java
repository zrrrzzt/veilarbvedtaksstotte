package no.nav.veilarbvedtaksstotte.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTestConfig extends ApplicationConfig {

//    @Override
//    public void startup(ServletContext servletContext) {}
//
//    @Override
//    public void configure(ApiAppConfigurator apiAppConfigurator) {}
//
//    @Bean
//    public DataSource dataSource() {
//        String dbUrl = getRequiredProperty(VEILARBVEDTAKSSTOTTE_DB_URL_PROPERTY);
//        HikariConfig config = createDataSourceConfig(dbUrl);
//        config.setUsername("postgres");
//        config.setPassword("qwerty");
//
//        DataSource source = new HikariDataSource(config);
//        DbTestUtils.testMigrate(source);
//
//        return source;
//    }
//
//    @Bean
//    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaAvsluttOppfolging>> kafkaListenerContainerFactory() {
//        HashMap<String, Object> props = new HashMap<> ();
//        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.put(GROUP_ID_CONFIG, "veilarbvedtaksstotte-test-consumer");
//        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 5000);
//        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//
//        ConcurrentKafkaListenerContainerFactory<String, KafkaAvsluttOppfolging> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));
//        return factory;
//    }
//
//    @Bean
//    public UnleashService unleashService() {
//        return mock(UnleashService.class);
//    }
//
//    @Bean
//    public AktorService aktorService() {
//        return new AktorServiceMock();
//    }
//
//    @Bean
//    public AktoerV2 aktoerV2() {
//        return mock(AktoerV2.class);
//    }
//
//    @Bean
//    public PepClient pepClient() {
//        return new PepClientMock();
//    }

}

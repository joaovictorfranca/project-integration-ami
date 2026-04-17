package com.eletra.integracao.network_grpc.configs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    ArtemisContainer artemisContainer() {
        return new ArtemisContainer(DockerImageName.parse("apache/activemq-artemis:latest"));
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:15"));
    }

//    @Bean
//    @ServiceConnection
//    PostgreSQLContainer postgresContainer() {
//        return new PostgreSQLContainer(DockerImageName.parse("postgres:15"));
//    }
//
//    // O "Pulo do Gato": Vamos forçar o parâmetro na URL
//    @Bean
//    public DynamicPropertyRegistrar dynamicPropertyRegistrar(PostgreSQLContainer postgres) {
//        return registry -> {
//            registry.add("spring.datasource.url",
//                    () -> postgres.getJdbcUrl() + "?stringtype=unspecified");
//        };
//    }
}

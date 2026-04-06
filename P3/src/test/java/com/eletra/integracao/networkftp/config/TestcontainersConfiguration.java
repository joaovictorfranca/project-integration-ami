package com.eletra.integracao.networkftp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection // Configura o Spring Artemis sozinho!
	ArtemisContainer artemisContainer() {
		return new ArtemisContainer(DockerImageName.parse("apache/activemq-artemis:latest"))
				.withUser("test")
				.withPassword("test");
	}
}
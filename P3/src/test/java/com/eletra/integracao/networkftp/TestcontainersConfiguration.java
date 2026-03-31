package com.eletra.integracao.networkftp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
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

	@Bean
	GenericContainer<?> ftpContainer() {
		return new GenericContainer<>(DockerImageName.parse("stilliard/pure-ftpd:latest"))
				.withExposedPorts(21)
				// Portas para o modo passivo (essencial para o teste não travar)
				.withEnv("FTP_USER_NAME", "test")
				.withEnv("FTP_USER_PASS", "test")
				.withEnv("FTP_USER_HOME", "/home/test")
				.withEnv("PUBLICHOST", "localhost")
				.waitingFor(Wait.forListeningPort().forPorts(21));
	}
}
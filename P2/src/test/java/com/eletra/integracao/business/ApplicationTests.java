package com.eletra.integracao.business;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApplicationTests {

	@Test
	void contextLoads() {
		// Given: O ambiente de teste está configurado com Testcontainers (Artemis)

		// When: O Spring Boot tenta inicializar o contexto completo da aplicação

		// Then: O carregamento deve ocorrer sem erros, validando Beans e configurações de infraestrutura
	}
}
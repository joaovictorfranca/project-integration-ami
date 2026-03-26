package com.eletra.integracao.converter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class ProjetoIntegracaoAmiApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void testMain() {
		ProjetoIntegracaoAmiApplicationConverter.main(new String[]{"--spring.main.web-application-type=none"});
	}
}

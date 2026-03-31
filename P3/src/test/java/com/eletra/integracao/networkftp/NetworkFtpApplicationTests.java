package com.eletra.integracao.networkftp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
// Garante que o contexto do SpringBootTest seja limpo após os testes desta classe
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NetworkFtpApplicationTests {

	@Test
	@DisplayName("GIVEN aplicação WHEN iniciar THEN deve carregar o contexto sem erros")
	void contextLoads() {
		// Apenas para garantir que o contexto sobe com as configurações padrão
	}

	@Test
	@DisplayName("GIVEN o método main WHEN executado THEN deve iniciar a aplicação com sucesso")
	void mainMethodTest() {
		/*
		 * O @SpringBootTest já iniciou um servidor FTP na porta padrão.
		 * Para testar o método main() sem erro de "Address already in use",
		 * passamos uma porta diferente via argumentos, garantindo que a
		 * segunda instância suba isolada e complete a cobertura de código.
		 */
		String[] args = {"--application.ftp.port=2122"};

		assertDoesNotThrow(() -> {
			NetworkFtpApplication.main(args);
		});
	}
}
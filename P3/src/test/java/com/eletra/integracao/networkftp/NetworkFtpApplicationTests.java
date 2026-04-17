package com.eletra.integracao.networkftp;

import com.eletra.integracao.networkftp.config.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(properties = {
		"application.ftp.port=2128" // Tiramos o 0 e voltamos para uma porta fixa
})
@Import(TestcontainersConfiguration.class)
class NetworkFtpApplicationTests {

	@Test
	@DisplayName("GIVEN aplicação WHEN iniciar THEN deve carregar o contexto sem erros")
	void contextLoads() {
	}
}
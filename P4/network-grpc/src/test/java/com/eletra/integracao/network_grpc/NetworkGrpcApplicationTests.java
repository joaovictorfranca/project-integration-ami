package com.eletra.integracao.network_grpc;

import com.eletra.integracao.network_grpc.configs.TestcontainersConfig;
import com.eletra.integracao.network_grpc.services.TransactionGrpcService;
import com.eletra.integracao.network_grpc.services.ProcessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfig.class)
@SpringBootTest(properties = {
		"spring.main.allow-bean-definition-overriding=true"
})
class NetworkGrpcApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		// Verifica se os seus serviços principais foram injetados corretamente no Spring
		Assertions.assertNotNull(applicationContext.getBean(TransactionGrpcService.class));
		Assertions.assertNotNull(applicationContext.getBean(ProcessService.class));
	}
}
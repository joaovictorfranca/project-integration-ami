package com.eletra.integracao.networkftp;

import com.eletra.integracao.networkftp.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestNetworkFtpApplication {

	public static void main(String[] args) {
		SpringApplication.from(NetworkFtpApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

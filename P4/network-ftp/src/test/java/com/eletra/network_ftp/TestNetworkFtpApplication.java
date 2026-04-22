package com.eletra.network_ftp;

import org.springframework.boot.SpringApplication;

public class TestNetworkFtpApplication {

	public static void main(String[] args) {
		SpringApplication.from(NetworkFtpApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package com.eletra.business;

import org.springframework.boot.SpringApplication;

public class TestBusinessApplication {

	public static void main(String[] args) {
		SpringApplication.from(BusinessApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

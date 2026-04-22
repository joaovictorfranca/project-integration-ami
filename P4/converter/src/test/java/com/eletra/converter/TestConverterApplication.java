package com.eletra.converter;

import org.springframework.boot.SpringApplication;

public class TestConverterApplication {

	public static void main(String[] args) {
		SpringApplication.from(ConverterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

package com.eletra.integracao.converter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonCsvConfig {
    // Apenas os mappers, sem o jacksonJmsMessageConverter
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public CsvMapper csvMapper() {
        return new CsvMapper();
    }
}
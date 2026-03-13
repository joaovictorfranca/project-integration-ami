package com.eletra.integracao.converter.service;

import com.eletra.integracao.converter.dto.MessageDTO;
import com.eletra.integracao.converter.exception.ConversionException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;


@RequiredArgsConstructor
@Service
public class MessageConverterService {

    private final CsvMapper csvMapper;
    private final JmsTemplate jmsTemplate;

    public String convertToCsv(MessageDTO input) {
        try {
            // Definimos o esquema baseado na ordem pedida: user, time, message
            CsvSchema schema = CsvSchema.builder()
                    .addColumn("user")
                    .addColumn("time")
                    .addColumn("message")
                    .setUseHeader(true)
                    .setQuoteChar('"') // Garante que mensagens com vírgula fiquem entre aspas
                    .build();

            // Mapeamos o input para as colunas do CSV
            Map<String, String> data = Map.of(
                    "user", input.username(),
                    "time", input.sentAt(),
                    "message", input.message()
            );

            return csvMapper.writer(schema).writeValueAsString(data).trim();
        } catch (Exception e) {
            throw new ConversionException("Erro ao converter mensagem para CSV", e);
        }
    }

    public void convertAndSend(MessageDTO input) {
        String csvResult = convertToCsv(input);
        System.out.println(csvResult);
        jmsTemplate.convertAndSend("training-converter.send_as_csv", csvResult);
    }
}

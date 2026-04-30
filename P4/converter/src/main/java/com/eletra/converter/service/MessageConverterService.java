package com.eletra.converter.service;

import com.eletra.converter.dto.MessageDTO;
import com.eletra.converter.exception.ConversionException;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.model.enums.ProcessStatus;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class MessageConverterService {

    private final CsvMapper csvMapper;
    private final JmsTemplate jmsTemplate;
    private final ProcessAndTicket processAndTicket;

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

    public void convertAndSend(MessageDTO input, UUID ticketId) {

        ProcessEntity process = processAndTicket.createMyProcess("",ticketId);

        try {
            String csvResult = convertToCsv(input);

            log.info("Processando mensagem para o FTP: {}", csvResult);

            processAndTicket.updateProcess(process, ProcessStatus.SUCCESS,csvResult);

            System.out.println(csvResult);
            jmsTemplate.convertAndSend("training-converter.send_as_csv", process.getId().toString());

        } catch (Exception e) {

            log.error("DTO validation/conversion error for process {}, marking as ERROR: {}", process.getId(), e.getMessage());

            processAndTicket.updateProcess(process, ProcessStatus.ERROR, input.toString());

            throw e;
        }
    }
}

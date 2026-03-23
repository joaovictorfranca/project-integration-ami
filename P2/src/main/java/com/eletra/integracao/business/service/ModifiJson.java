package com.eletra.integracao.business.service;

import com.eletra.integracao.business.dto.ReceivedMessageDTO;
import com.eletra.integracao.business.dto.SentMessageDTO;
import com.eletra.integracao.business.producer.BusinessProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
//import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Log4j2
@RequiredArgsConstructor
@Service
public class ModifiJson {

    private final BusinessProducer producer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // Formato de saída para o P1
    private final DateTimeFormatter outFmt = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public void execute(String json) throws Exception {
        // 1. Converte String JSON para Objeto
        ReceivedMessageDTO receivedDto = objectMapper.readValue(json, ReceivedMessageDTO.class);

        // 2. Valida os campos obrigatórios
        verifyFormat(receivedDto);

        // 3. Transforma para o DTO de saída (Regra: ID vira Username)
        SentMessageDTO sentDto = new SentMessageDTO(
                receivedDto.getUser().getId(),
                outFmt.format(Instant.now(clock)),
                formatSentAt(receivedDto.getLog().getSentAt()),
                receivedDto.getLog().getMessage()
        );

        // 4. Converte de volta para String para enviar
        String outputJson = objectMapper.writeValueAsString(sentDto);

        log.info("Processando mensagem para a Converter: {}", outputJson);

        producer.send(outputJson);

    }

    private String formatSentAt(String raw) {
        // Formato que vem na mensagem original (ex: 03-17-2026T14:00:00.000Z)
        DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("MM-dd-uuuu'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);

        Instant inst = inFmt.parse(raw, Instant::from);
        return outFmt.format(inst);
    }

    private void verifyFormat(ReceivedMessageDTO dto) throws Exception {
//        if (dto.getUser() == null || dto.getLog() == null || dto.getUser().getId() == null) {
//            throw new Exception("Mensagem incompleta: User ou Log ausentes.");
//        }
//        if (dto.getLog().getSentAt() == null || dto.getLog().getMessage() == null) {
//            throw new Exception("Mensagem incompleta: Campos do Log ausentes.");
//        }
        if (dto.getUser() == null) {
            throw new Exception("User is missing in received message");
        }
        if (dto.getLog() == null) {
            throw new Exception("Log is missing in received message");
        }
        if (dto.getUser().getId() == null || dto.getUser().getId().isEmpty()) {
            throw new Exception("Invalid user ID in received message");
        }
        if (dto.getLog().getSentAt() == null || dto.getLog().getSentAt().isEmpty()) {
            throw new Exception("Invalid sentAt in received message log");
        }
        if (dto.getLog().getMessage() == null || dto.getLog().getMessage().isEmpty()) {
            throw new Exception("Invalid message content in received message log");
        }
    }
}

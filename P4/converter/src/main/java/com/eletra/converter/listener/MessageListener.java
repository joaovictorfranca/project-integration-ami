package com.eletra.converter.listener;

import com.eletra.converter.dto.MessageDTO;
import com.eletra.converter.service.MessageConverterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;


@Log4j2
@Component
@RequiredArgsConstructor
public class MessageListener {

    private final MessageConverterService converterService;
    private final ObjectMapper objectMapper; // Injetado do config acima

    @JmsListener(destination = "training-converter.send_as_json")
    public void onMessage(String jsonBruto) { // Recebe String!
        try {

            System.out.println("Recebendo a Mensagem ...\n\n");

            log.info("JSON bruto recebido: {}", jsonBruto);

            // Converte a String manualmente para o seu Record
            MessageDTO message = objectMapper.readValue(jsonBruto, MessageDTO.class);

            log.info("Processando mensagem de: {}", message.username());

            converterService.convertAndSend(message);
            log.info("Mensagem processada com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao processar mensagem", e);
        }
    }
}
package com.eletra.business.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
public class BusinessProducer {

    private final JmsTemplate jmsTemplate;

    public void send(String message) {
        try {
            jmsTemplate.convertAndSend("training-converter.send_as_json", message);
        } catch (JmsException e) {
            log.error("Erro ao enviar mensagem para a fila: {}", e.getMessage());
        }

    }
}

package com.eletra.integracao.business.listener;

import com.eletra.integracao.business.service.ModifiJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class BusinessListener {

    private final ModifiJson jsonFormatService;

    @JmsListener(destination = "training-converter.receive_as_json")
    public void onMessage(String json) {
        try {
            jsonFormatService.execute(json);
        } catch (Exception e) {
            log.error("Erro ao processar mensagem da fila: {}", e.getMessage());
            // Aqui você poderia tratar o erro ou mandar para uma DLQ
        }
    }
}
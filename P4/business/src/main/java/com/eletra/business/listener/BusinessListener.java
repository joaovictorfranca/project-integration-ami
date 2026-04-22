package com.eletra.business.listener;

import com.eletra.business.model.entities.ProcessEntity;
import com.eletra.business.repositories.ProcessRepository;
import com.eletra.business.repositories.TicketRepository;
import com.eletra.business.service.ModifiJson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Component
public class BusinessListener {

    private final ModifiJson jsonFormatService;
    private final TicketRepository ticketRepository;
    private final ProcessRepository processRepository;

    @JmsListener(destination = "training-converter.receive_as_json")
    public void receivePreviousProcess(UUID idProcesso) {
        try {
            log.info("Recebido ID do processo da fila: {}", idProcesso);

            // O findById retorna Optional. Usamos o .orElseThrow para lançar erro se não achar.
            ProcessEntity process = processRepository.findById(idProcesso)
                    .orElseThrow(() -> new RuntimeException("Processo não encontrado com o ID: " + idProcesso));

            // Agora você tem acesso ao Ticket e ao Payload que salvou lá no outro microserviço
            String payloadOriginal = process.getPayload();

            log.info("Payload recuperado do banco: {}", payloadOriginal);

            // Chama o seu serviço de modificação
            jsonFormatService.execute(payloadOriginal, process.getTicket().getId());

        } catch (Exception e) {
            log.error("Erro ao processar mensagem da fila: {}", e.getMessage(), e);
        }
    }
}
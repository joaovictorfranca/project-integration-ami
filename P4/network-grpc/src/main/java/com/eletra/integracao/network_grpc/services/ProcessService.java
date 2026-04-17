package com.eletra.integracao.network_grpc.services;

import com.eletra.integracao.network_grpc.models.entities.ProcessEntity;
import com.eletra.integracao.network_grpc.models.entities.TicketsEntity;
import com.eletra.integracao.network_grpc.models.enums.ProcessStatus;
import com.eletra.integracao.network_grpc.models.enums.ProcessType;
import com.eletra.integracao.network_grpc.models.enums.TicketsStatus;
import com.eletra.integracao.network_grpc.repositories.ProcessRepository;
import com.eletra.integracao.network_grpc.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProcessService {

    private final TicketRepository ticketRepository;
    private final ProcessRepository processRepository;
    private final JmsTemplate jmsTemplate;

    @Value("${queue.training-converter.name}")
    private String queueName;

    @Transactional
    public ProcessEntity startIntegration(String jsonPayload) {
        // 1. Cria o Ticket Pai (TicketsEntity)
        TicketsEntity ticket = new TicketsEntity(TicketsStatus.OPEN);
        ticket = ticketRepository.save(ticket);
        log.info("Ticket gerado: {}", ticket.getId());

        // 2. Cria o primeiro rastro: O Processo do gRPC
        ProcessEntity process = new ProcessEntity(
                ProcessStatus.SUCCESS,
                jsonPayload,
                ProcessType.NETWORK_GRPC,
                ticket
        );
        process = processRepository.save(process);
        log.info("Processo gRPC registrado: {}", process.getId());

        // 3. Atualiza o status do Ticket para IN_PROCESS
        ticket.setStatus(TicketsStatus.IN_PROCESS);
        ticketRepository.save(ticket);

        return process; // Retornamos o ProcessEntity completo para acesso ao ID do processo e ticket
    }

    public void sendToQueue(UUID processId) {
        jmsTemplate.convertAndSend(queueName, processId.toString());
        log.info("Bastão enviado (Process ID: {}) para a fila: {}", processId, queueName);
    }
}
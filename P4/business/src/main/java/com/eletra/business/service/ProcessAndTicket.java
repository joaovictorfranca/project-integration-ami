package com.eletra.business.service;

import com.eletra.business.model.entities.ProcessEntity;
import com.eletra.business.model.entities.TicketsEntity;
import com.eletra.business.model.enums.ProcessStatus;
import com.eletra.business.model.enums.ProcessType;
import com.eletra.business.model.enums.TicketsStatus;
import com.eletra.business.repositories.ProcessRepository;
import com.eletra.business.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class ProcessAndTicket {

    private final TicketRepository ticketRepository;
    private final ProcessRepository processRepository;

    @Transactional
    public ProcessEntity createMyProcess(String payload, UUID ticketId) {
        try {
            log.info("Iniciando criação do processo para o Ticket ID: {}", ticketId);

            // 1. Busca o Ticket (O método getTicketId já lança exceção se não achar)
            TicketsEntity ticket = getTicketId(ticketId);

            // 2. Instancia o novo processo
            ProcessEntity myProcess = new ProcessEntity(
                    ProcessStatus.PROCESSING,
                    payload,
                    ProcessType.BUSINESS,
                    ticket
            );

            // 3. Salva o processo
            ProcessEntity savedProcess = processRepository.save(myProcess);
            log.info("Processo salvo com sucesso. ID: {}", savedProcess.getId());

            // 4. Atualiza o status do Ticket
            updateTicket(ticket, TicketsStatus.IN_PROCESS);

            return savedProcess;

        } catch (RuntimeException e) {
            log.error("Erro de negócio ao criar processo: {}", e.getMessage());
            throw e; // Lança novamente para o Listener saber que falhou
        } catch (Exception e) {
            log.error("Erro crítico e inesperado ao criar processo para o Ticket {}: ", ticketId, e);
            throw new RuntimeException("Falha técnica ao criar processo de negócio", e);
        }
    }

    @Transactional
    public void updateProcess(ProcessEntity process, ProcessStatus status, String payload) {
        try {
            log.info("Atualizando processo ID: {} para status: {}", process.getId(), status);
            process.setPayload(payload);
            process.setStatus(status);
            processRepository.save(process);
        } catch (Exception e) {
            log.error("Erro ao atualizar processo ID: {}: ", process.getId(), e);
            throw e;
        }
    }

    @Transactional
    public void updateTicket(TicketsEntity ticket, TicketsStatus status) {
        try {
            log.info("Atualizando status do Ticket ID: {} para: {}", ticket.getId(), status);
            ticket.setStatus(status);
            ticketRepository.save(ticket);
        } catch (Exception e) {
            log.error("Erro ao atualizar ticket ID: {}: ", ticket.getId(), e);
            throw e;
        }
    }

    public TicketsEntity getTicketId(UUID idTicket) {
        // Não precisa de try-catch aqui se for apenas uma consulta simples
        return ticketRepository.findById(idTicket)
                .orElseThrow(() -> {
                    log.warn("Tentativa de busca falhou: Ticket ID {} não existe.", idTicket);
                    return new RuntimeException("Ticket não encontrado com o ID: " + idTicket);
                });
    }
}
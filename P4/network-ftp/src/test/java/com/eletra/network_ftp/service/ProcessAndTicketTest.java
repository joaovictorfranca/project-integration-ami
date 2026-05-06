package com.eletra.network_ftp.service;

import com.eletra.network_ftp.TestcontainersConfiguration;
import com.eletra.network_ftp.model.entities.ProcessEntity;
import com.eletra.network_ftp.model.entities.TicketsEntity;
import com.eletra.network_ftp.model.enums.ProcessStatus;
import com.eletra.network_ftp.model.enums.ProcessType;
import com.eletra.network_ftp.model.enums.TicketsStatus;
import com.eletra.network_ftp.repositories.ProcessRepository;
import com.eletra.network_ftp.repositories.TicketRepository;
import com.eletra.network_ftp.service.ProcessAndTicket;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ProcessAndTicketTest {

    @Autowired
    private ProcessAndTicket processAndTicket;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProcessRepository processRepository;

    @Test
    @DisplayName("GIVEN um ticket aberto WHEN criar um processo THEN o processo deve ser PROCESSING e o ticket IN_PROCESS")
    public void mustCreateProcessAndReturnEntityTest() {
        // GIVEN
        TicketsEntity ticket = new TicketsEntity();
        ticket.setStatus(TicketsStatus.OPEN);
        ticket = ticketRepository.save(ticket);
        
        String fakePayload = "FAKE_CSV";

        // WHEN
        ProcessEntity response = processAndTicket.createMyProcess(fakePayload, ticket.getId());

        // THEN
        Assertions.assertNotNull(response);
        Assertions.assertEquals(ProcessStatus.PROCESSING, response.getStatus());
        Assertions.assertEquals(ProcessType.NETWORK_FTP, response.getType());

        TicketsEntity updatedTicket = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertEquals(TicketsStatus.IN_PROCESS, updatedTicket.getStatus());
    }

    @Test
    @DisplayName("GIVEN um processo existente WHEN atualizar para sucesso THEN campos devem refletir a mudança")
    public void shouldUpdateProcessStatusAndPayloadTest() {
        // GIVEN
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.OPEN));
        ProcessEntity process = processRepository.save(new ProcessEntity(ProcessStatus.PROCESSING, "", ProcessType.NETWORK_FTP, ticket));

        String newPayload = "FINAL_FTP_STREAM_ID";

        // WHEN
        processAndTicket.updateProcess(process, ProcessStatus.SUCCESS, newPayload);

        // THEN
        ProcessEntity updated = processRepository.findById(process.getId()).orElse(null);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(ProcessStatus.SUCCESS, updated.getStatus());
        Assertions.assertEquals(newPayload, updated.getPayload());
    }

    @Test
    @DisplayName("GIVEN um ticket IN_PROCESS WHEN terminar fluxo THEN status deve ser DONE")
    public void shouldUpdateTicketToDoneTest() {
        // GIVEN
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));

        // WHEN
        processAndTicket.updateTicket(ticket, TicketsStatus.DONE);

        // THEN
        TicketsEntity updated = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(TicketsStatus.DONE, updated.getStatus());
    }

    @Test
    @DisplayName("GIVEN um ticket aleatório WHEN ticket não encontrado no banco THEN deve lançar RuntimeException (Cobre createMyProcess catch e getTicketId)")
    public void catchDoCreateMyProcess() {
        // GIVEN
        UUID idAleatorio = UUID.randomUUID();

        // WHEN / THEN
        Assertions.assertThrows(RuntimeException.class, () -> {
            processAndTicket.createMyProcess("Deve dar erro", idAleatorio);
        });
    }

    @Test
    @DisplayName("GIVEN ticketId inexistente WHEN buscar ticket THEN deve lançar RuntimeException")
    public void shouldThrowExceptionWhenGetTicketIdNotFoundTest() {
        // GIVEN
        UUID nonExistentId = UUID.randomUUID();

        // WHEN / THEN
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            processAndTicket.getTicketId(nonExistentId);
        });

        Assertions.assertTrue(exception.getMessage().contains("Ticket não encontrado"));
    }
}

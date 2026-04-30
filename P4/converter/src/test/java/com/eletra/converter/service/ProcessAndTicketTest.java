package com.eletra.converter.service;

import com.eletra.converter.TestcontainersConfiguration;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.model.entities.TicketsEntity;
import com.eletra.converter.model.enums.ProcessStatus;
import com.eletra.converter.model.enums.ProcessType;
import com.eletra.converter.model.enums.TicketsStatus;
import com.eletra.converter.repositories.ProcessRepository;
import com.eletra.converter.repositories.TicketRepository;
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
    @DisplayName("Caminho Feliz: Deve criar o ProcessEntity e comitar a atualização do Ticket")
    public void mustCreateProcessAndReturnEntityTest() {
        // Given
        TicketsEntity ticket = new TicketsEntity();
        ticket.setStatus(TicketsStatus.OPEN);
        ticket = ticketRepository.save(ticket);
        
        String dummyPayload = "DUMMY_PAYLOAD";

        // When
        ProcessEntity response = processAndTicket.createMyProcess(dummyPayload, ticket.getId());

        // Then
        Assertions.assertNotNull(response, "O ProcessEntity retornado nao pode ser nulo");
        Assertions.assertEquals(dummyPayload, response.getPayload());
        Assertions.assertEquals(ProcessStatus.PROCESSING, response.getStatus());
        Assertions.assertEquals(ProcessType.CONVERTER, response.getType());

        // Checar o banco de forma independente para ver se salvou!
        var processSalvo = processRepository.findById(response.getId()).orElse(null);
        Assertions.assertNotNull(processSalvo);
        Assertions.assertEquals(ProcessStatus.PROCESSING, processSalvo.getStatus());

        TicketsEntity updatedTicket = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertNotNull(updatedTicket);
        // Validar que o status do TicketEntity foi alterado para IN_PROCESS
        Assertions.assertEquals(TicketsStatus.IN_PROCESS, updatedTicket.getStatus());
    }

    @Test
    @DisplayName("Cobertura de Erro: Deve disparar RuntimeException quando usar ticket falso")
    public void shouldThrowExceptionWhenTicketNotFoundTest() {
        // Given
        UUID invalidTicketId = UUID.randomUUID();

        // When / Then
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            processAndTicket.createMyProcess("PAYLOAD_BUGADO", invalidTicketId);
        });

        Assertions.assertTrue(thrown.getMessage().contains("Ticket não encontrado com o ID"));
    }
}

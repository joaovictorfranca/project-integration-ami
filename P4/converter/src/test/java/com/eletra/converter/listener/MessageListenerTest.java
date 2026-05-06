package com.eletra.converter.listener;

import com.eletra.converter.TestcontainersConfiguration;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.model.entities.TicketsEntity;
import com.eletra.converter.model.enums.ProcessStatus;
import com.eletra.converter.model.enums.ProcessType;
import com.eletra.converter.model.enums.TicketsStatus;
import com.eletra.converter.repositories.ProcessRepository;
import com.eletra.converter.repositories.TicketRepository;
import com.eletra.converter.service.MessageConverterService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class MessageListenerTest {

    @Autowired
    private MessageListener messageListener;

    @MockitoSpyBean
    private MessageConverterService messageConverterService;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private TicketRepository ticketRepository;

    String jsonBruto = """
            {
                "username":"francisco.parreira",
                "message":"No. Interestingly enough, her leaf blower picked up.",
                "sentAt":"2026-01-27T12:05:04.001Z"
            }""";

    @Test
    public void messageDelegatedToTheService() throws Exception {
        // Given
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity pprocess = processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, jsonBruto, ProcessType.NETWORK_GRPC, ticket));
        
        UUID idPProcess = pprocess.getId();

        // When
        Assertions.assertDoesNotThrow(() -> {
            messageListener.onMessage(idPProcess);
        });

        // Then: Verifica se passou para a MessageConverterService invocando convertAndSend
        Mockito.verify(messageConverterService, Mockito.times(1))
                .convertAndSend(ArgumentMatchers.any(), ArgumentMatchers.eq(ticket.getId()));
    }

    @Test
    public void shouldReturnErrorIdNotFound() throws Exception {
        // Given
        UUID idForaDoBanco = UUID.randomUUID();

        // When
        Assertions.assertDoesNotThrow(() -> {
            messageListener.onMessage(idForaDoBanco);
        });

        // Then
        // O metodo falha e nao delega pra convertAndSend
        Mockito.verify(messageConverterService, Mockito.never())
                .convertAndSend(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void dealingWithServiceFailureTest() throws Exception {
        // GIVEN: Um processo válido e um ticket
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity pprocess = processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, jsonBruto, ProcessType.NETWORK_GRPC, ticket));

        // Forçamos o serviço a explodir quando for chamado
        Mockito.doThrow(new RuntimeException("Failure in conversion logic"))
                .when(messageConverterService).convertAndSend(ArgumentMatchers.any(), ArgumentMatchers.any());

        // WHEN: Listener tenta processar
        Assertions.assertDoesNotThrow(() -> {
            messageListener.onMessage(pprocess.getId());
        });

        // THEN: O catch deve atuar (nenhuma exception vaza) e o serviço deve ter sido chamado 1 vez antes da falha
        Mockito.verify(messageConverterService, Mockito.times(1))
                .convertAndSend(ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}

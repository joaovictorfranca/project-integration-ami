package com.eletra.business.listener;

import com.eletra.business.TestcontainersConfiguration;
import com.eletra.business.model.entities.ProcessEntity;
import com.eletra.business.model.entities.TicketsEntity;
import com.eletra.business.model.enums.ProcessStatus;
import com.eletra.business.model.enums.ProcessType;
import com.eletra.business.model.enums.TicketsStatus;
import com.eletra.business.repositories.ProcessRepository;
import com.eletra.business.repositories.TicketRepository;
import com.eletra.business.service.ModifyJson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // Usa o seu Artemis do Testcontainers
@ActiveProfiles("test")
public class BusinessListenerTest {

    @Autowired
    private BusinessListener businessListener;

    // Usamos Spy para verificar se o Listener realmente chamou a Service
    @MockitoSpyBean
    private ModifyJson modifyJson;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private TicketRepository ticketRepository;

    String message = """
                {
                    "user": {
                        "id":"b16404b4-f690-44dc-8db0-8f48ec568590",
                        "username":"francisco.parreira",
                        "firstName":"Lorraine",
                        "lastName":"Almeida",
                        "employeeCode":"640708",
                        "position":"gardener",
                        "cpf":"534.670.770-05"
                    },
                    "log": {
                        "id":"9580ab40-b0b6-42cb-bb8f-7c1e1f654f6a",
                        "sentAt":"01-27-2026T12:05:04.001Z",
                        "message":"No. Interestingly enough, her leaf blower picked up.",
                        "format":null
                    }
                }""";


    @Test
    public void shouldReturnErrorIdNotFound() throws Exception{
        // Given: Um ID que temos certeza que não está no banco (acabou de ser gerado)
        UUID idForaDoBanco = UUID.randomUUID();

        // When: Chamamos o listener
        // Verificamos que ele NÃO lança exceção (o try-catch interno segura)
        Assertions.assertDoesNotThrow(() -> {
            businessListener.receivePreviousProcess(idForaDoBanco);
        });

        // Then: Garantimos que o modifyJson NUNCA foi chamado
        // Já que o findById falhou e lançou a RuntimeException que caiu no catch
        Mockito.verify(modifyJson, Mockito.never()).execute(ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    public void messageDelegatedToTheService() throws Exception {
        // Given
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity pprocess = processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, message, ProcessType.NETWORK_GRPC, ticket));
        UUID ticketId = pprocess.getTicket().getId();
        UUID idPProcess = pprocess.getId();
        String payload = pprocess.getPayload();

        // When
        Assertions.assertDoesNotThrow(() -> {
            businessListener.receivePreviousProcess(idPProcess);
        });

        // Then: Verifica se o Listener passou a bola para a Service ModifiJson
        Mockito.verify(modifyJson, Mockito.times(1)).execute(payload,ticketId);
    }

    @Test
    public void dealingWithMalformedJson() throws Exception {
        // Given: JSON malformado
        String malformedMessage = """
            {
                "user" { "id": "123" }
            }""";

        TicketsEntity t = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity p = processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, malformedMessage, ProcessType.NETWORK_GRPC, t));

        // When & Then
        // 1. Verificamos que o Listener não deixa a exceção subir (o try-catch dele funciona)
        Assertions.assertDoesNotThrow(() -> {
            businessListener.receivePreviousProcess(p.getId());
        });

        // 2. Opcional: Em vez de "never", verificamos que ele FOI chamado,
        // mas sabemos que ele falhou internamente.
        Mockito.verify(modifyJson, Mockito.times(1)).execute(p.getPayload(),t.getId());

        // Isso garante que o fluxo passou pelo Listener e entrou na Service!
    }
}
package com.eletra.integracao.network_grpc.services;

import com.eletra.integracao.network_grpc.configs.TestcontainersConfig; // Importe o config do dev
import com.eletra.integracao.network_grpc.repositories.ProcessRepository;
import com.eletra.integracao.network_grpc.repositories.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

@Import(TestcontainersConfig.class)
//@SpringBootTest
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
})
public class ProcessServiceTest {

    @Autowired
    private ProcessService processService;

    @MockitoSpyBean
    private TicketRepository ticketRepository;

    @MockitoSpyBean
    private ProcessRepository processRepository;

    @MockitoSpyBean
    private JmsTemplate jmsTemplate;

    @Test
    @DisplayName("Deve criar Ticket e Processo e atualizar o status do Ticket")
    public void mustStartCompleteIntegrationTest() {
        // Given
        String payload = """
                {
                    "user":
                        {
                            "id":"b16404b4-f690-44dc-8db0-8f48ec568590",
                            "username":"francisco.parreira",
                            "firstName":"Lorraine",
                            "lastName":"Almeida",
                            "employeeCode":"640708",
                            "position":"gardener",
                            "cpf":"534.670.770-05"
                        },
                    "log":
                        {
                            "id":"9580ab40-b0b6-42cb-bb8f-7c1e1f654f6a",
                            "sentAt":"01-27-2026T12:05:04.001Z",
                            "message":"No. Interestingly enough, her leaf blower picked up.",
                            "format":null
                        }
                }""";

        // When
        var processoSalvoEntity = processService.startIntegration(payload);
        UUID processId = processoSalvoEntity.getId();

        // Then
        Assertions.assertNotNull(processId);

        // Verifica se salvou o Ticket (1x na criação e 1x no update de status para IN_PROCESS)
        Mockito.verify(ticketRepository, Mockito.atLeastOnce()).save(Mockito.any());

        // Verifica se salvou o Processo vinculado
        Mockito.verify(processRepository, Mockito.times(1)).save(Mockito.any());

        // Opcional: Validar se o processo salvo no banco tem o mesmo payload
        var processoSalvo = processRepository.findById(processId);
        Assertions.assertTrue(processoSalvo.isPresent());
        Assertions.assertEquals(payload, processoSalvo.get().getPayload());
    }

    @Test
    @DisplayName("Deve enviar o ID do Processo para a fila correta do Artemis")
    public void mustSendProcessIdToFilaTest() {
        // Given
        UUID mockProcessId = UUID.randomUUID();

        // When
        Assertions.assertDoesNotThrow(() -> {
            processService.sendToQueue(mockProcessId);
        });

        // Then
        // Aqui garantimos que você está passando o ID do PROCESSO e não do TICKET
        Mockito.verify(jmsTemplate, Mockito.times(1))
                .convertAndSend(Mockito.eq("training-converter.receive_as_json"), Mockito.eq(mockProcessId.toString()));
    }
}
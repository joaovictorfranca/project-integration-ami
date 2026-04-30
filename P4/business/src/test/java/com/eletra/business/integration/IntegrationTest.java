package com.eletra.business.integration;

import com.eletra.business.TestcontainersConfiguration;
import com.eletra.business.model.entities.ProcessEntity;
import com.eletra.business.model.entities.TicketsEntity;
import com.eletra.business.model.enums.ProcessStatus;
import com.eletra.business.model.enums.ProcessType;
import com.eletra.business.model.enums.TicketsStatus;
import com.eletra.business.producer.BusinessProducer;
import com.eletra.business.repositories.ProcessRepository;
import com.eletra.business.repositories.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class IntegrationTest {

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private JmsTemplate jmsTemplate;

    @Autowired
    private BusinessProducer businessProducer;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProcessRepository processRepository;

    @BeforeEach
    void setUp() {
        // Limpa as filas caso algum teste anterior tenha deixado lixo
        jmsTemplate.setReceiveTimeout(100);
        while (jmsTemplate.receive("training-converter.send_as_json") != null) {}
    }

    @Test
    void mustProcessAndSendOnlyTheIdToAConverterWithPayloadSavedInTheBank() {
        // 1. GIVEN: O processo anterior (ex: gRPC) salvou o JSON sujo no banco
        String jsonEntrada = """
        {
            "user": {
                "id": "olivia.tavares",
                "username": "otavares",
                "firstName": "Olivia",
                "lastName": "Tavares",
                "cpf": "123.456.789-00"
            },
            "log": {
                "id": "uuid-qualquer",
                "message": "I’m glad we’re having a rehearsal dinner.",
                "sentAt": "03-17-2026T15:13:25.000Z",
                "format": "text"
            }
        }
        """;

        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity processoAnterior = processRepository.save(
                new ProcessEntity(ProcessStatus.SUCCESS, jsonEntrada, ProcessType.NETWORK_GRPC, ticket)
        );

        // 2. WHEN: O listener recebe o ID desse processo pela fila de entrada
        jmsTemplate.convertAndSend("training-converter.receive_as_json", processoAnterior.getId().toString());

        // Aguardamos o processamento assíncrono e pegamos o que saiu na fila de saída
        jmsTemplate.setReceiveTimeout(10000); // Aguarda até 10s
        String idEnviadoParaSaida = (String) jmsTemplate.receiveAndConvert("training-converter.send_as_json");

        // 3. THEN: Verificações
        Assertions.assertNotNull(idEnviadoParaSaida, "A fila de saída não deveria estar vazia! O fluxo parou no meio.");

        // Vai no banco de dados buscar o novo processo usando o ID que saiu na fila
        ProcessEntity novoProcessoBusiness = processRepository.findById(UUID.fromString(idEnviadoParaSaida))
                .orElseThrow(() -> new AssertionError("Processo de saída não encontrado no banco!"));

        String jsonSaidaNoBanco = novoProcessoBusiness.getPayload();

        // O payload no banco deve estar transformado e limpo!
        Assertions.assertNotNull(jsonSaidaNoBanco);
        Assertions.assertTrue(jsonSaidaNoBanco.contains("\"username\":\"olivia.tavares\""));
        Assertions.assertTrue(jsonSaidaNoBanco.contains("\"sentAt\":\"2026-03-17 15:13:25\""));

        // O JSON de saída NÃO deve ter campos que não devia
        Assertions.assertFalse(jsonSaidaNoBanco.contains("firstName"), "O JSON no banco não deve ter firstName!");
        Assertions.assertFalse(jsonSaidaNoBanco.contains("cpf"), "O JSON no banco não deve ter CPF!");
    }

    @Test
    void mustCheckIfTheIdIsBeingSentToAConverter() {
        // 1. GIVEN: Um UUID qualquer simulando o ID do processo gerado pelo Business
        String idProcessoBusiness = UUID.randomUUID().toString();

        // 2. WHEN: Chamamos o seu producer para enviar a mensagem (agora ele envia ID)
        businessProducer.send(idProcessoBusiness);

        // 3. THEN: Lemos da fila de SAÍDA para ver se o Producer postou o ID lá
        jmsTemplate.setReceiveTimeout(5000);
        String idRecebidoDaFila = (String) jmsTemplate.receiveAndConvert("training-converter.send_as_json");

        // 4. ASSERTIONS: Valida se o ID que saiu é o mesmo que entrou
        Assertions.assertNotNull(idRecebidoDaFila, "O Producer não enviou o ID para a fila");
        Assertions.assertEquals(idProcessoBusiness, idRecebidoDaFila, "O ID recebido na fila é diferente do enviado");
    }

    @Test
    void mustCatchWhenThrowingJmsException() {
        // Given: Um cenário onde o Broker (Artemis) está fora do ar
        org.mockito.Mockito.doThrow(new org.springframework.jms.UncategorizedJmsException("Error Simulado"))
                .when(jmsTemplate).convertAndSend(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString());

        // When/Then: Envia um ID e garante que não quebra a aplicação
        Assertions.assertDoesNotThrow(() -> {
            businessProducer.send(UUID.randomUUID().toString());
        });

        // Reseta o mock para não afetar outros testes
        org.mockito.Mockito.reset(jmsTemplate);
    }
}
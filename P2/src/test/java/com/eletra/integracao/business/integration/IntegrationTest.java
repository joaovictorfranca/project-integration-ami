package com.eletra.integracao.business.integration;

import com.eletra.integracao.business.TestcontainersConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import com.eletra.integracao.business.producer.BusinessProducer;


@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class IntegrationTest {

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private JmsTemplate jmsTemplate;

    @Autowired
    private BusinessProducer businessProducer;

    @Test
    void deveProcessarEEnviarParaAConverterNoPadraoExato() {
        // 1. GIVEN: JSON COMPLETO vindo do P0 (Fiel à realidade)
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

        // WHEN
        jmsTemplate.convertAndSend("training-converter.receive_as_json", jsonEntrada);

        jmsTemplate.setReceiveTimeout(10000);
        String jsonSaida = (String) jmsTemplate.receiveAndConvert("training-converter.send_as_json");

        // THEN
        Assertions.assertNotNull(jsonSaida);
        // As validações abaixo confirmam que o P2 "limpou" o lixo e mandou só o necessário
        Assertions.assertTrue(jsonSaida.contains("\"username\":\"olivia.tavares\""));
        Assertions.assertTrue(jsonSaida.contains("\"sentAt\":\"2026-03-17 15:13:25\""));

        // IMPORTANTE: Verifique se o JSON de saída NÃO contém campos que não devia
        Assertions.assertFalse(jsonSaida.contains("firstName"), "O JSON de saída não deve ter firstName!");
        Assertions.assertFalse(jsonSaida.contains("cpf"), "O JSON de saída não deve ter CPF!");
    }

    @Test
    void deveChecarSeOJsonEstaSendoEnviadoParaAConverter() {
        // 1. GIVEN: O JSON exatamente no padrão que o Converter (P1) espera
        String jsonParaEnviarAConverter = """
        {
            "username":"olivia.tavares",
            "createdAt":"2026-03-17 15:13:40",
            "sentAt":"2026-03-17 15:13:25",
            "message":"I’m glad we’re having a rehearsal dinner. I rarely practice my meals before I eat."
        }
        """;

        // 2. WHEN: Chamamos o seu producer para enviar a mensagem
        businessProducer.send(jsonParaEnviarAConverter);

        // 3. THEN: Tentamos ler da fila de SAÍDA para ver se o Producer postou lá
        jmsTemplate.setReceiveTimeout(5000); // Aguarda até 5 segundos
        String jsonRecebidoDaFila = (String) jmsTemplate.receiveAndConvert("training-converter.send_as_json");

        // 4. ASSERTIONS: Valida se o que saiu é igual ao que entrou
        Assertions.assertNotNull(jsonRecebidoDaFila, "O Producer não enviou a mensagem para a fila 'training-converter.send_as_json'");
        Assertions.assertEquals(jsonParaEnviarAConverter.trim(), jsonRecebidoDaFila.trim(), "O JSON recebido na fila de saída é diferente do enviado");

        // Validação extra de segurança
        Assertions.assertTrue(jsonRecebidoDaFila.contains("olivia.tavares"));
        
    }

    @Test
    void deveFazerCatchAoLancarExcecaoDoJms() {
        // Simula uma falha de conexão com a fila apenas para verificar a cobertura do bloco catch no Producer
        // Given: Um cenário onde o Broker (Artemis) está fora do ar ou inacessível
        org.mockito.Mockito.doThrow(new org.springframework.jms.UncategorizedJmsException("Error Simulado"))
            .when(jmsTemplate).convertAndSend(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString());

        // When: O producer tenta enviar uma mensagem nesse cenário de falha
        // Then: O sistema deve tratar o erro internamente (catch) e não lançar exceção para fora
        Assertions.assertDoesNotThrow(() -> {
            businessProducer.send("Mensagem");
        });

        // Then: Garantir o reset para não interferir em outros testes caso rodem depois.
        org.mockito.Mockito.reset(jmsTemplate);
    }

}
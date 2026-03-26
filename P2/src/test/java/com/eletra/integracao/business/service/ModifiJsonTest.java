package com.eletra.integracao.business.service;

import com.eletra.integracao.business.TestcontainersConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // Importa o container de Artemis que você já tem
public class ModifiJsonTest {

    @Autowired
    private ModifiJson modifiJson;

    // O Spy permite que o JmsTemplate funcione normalmente, mas nos deixa "vigiar" as chamadas
    @MockitoSpyBean
    private JmsTemplate jmsTemplate;

    @MockitoBean
    private Clock clock;

    @Test
    public void jsonDeveSerConvertidoCorretamente() {
        // Given: Tempo congelado para validar o createdAt
        Instant fixedInstant = Instant.parse("2026-01-27T12:05:34Z");
        Mockito.when(clock.instant()).thenReturn(fixedInstant);
        Mockito.when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        String message = """
                {
                    "user": {
                        "id":"b16404b4-f690-44dc-8db0-8f48ec568590",
                        "username":"francisco.parreira"
                    },
                    "log": {
                        "id":"9580ab40-b0b6-42cb-bb8f-7c1e1f654f6a",
                        "sentAt":"01-27-2026T12:05:04.001Z",
                        "message":"No. Interestingly enough, her leaf blower picked up."
                    }
                }""";

        // When: Executa a lógica
        Assertions.assertDoesNotThrow(() -> {
            modifiJson.execute(message);
        });

        // Then: Verifica se o JmsTemplate enviou para a fila certa o JSON formatado
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
                Mockito.eq("training-converter.send_as_json"),
                messageCaptor.capture());

        final String result = messageCaptor.getValue();

        // Valida se o ID do user foi para o campo username e se as datas estão corretas
        assertTrue(result.contains("\"username\":\"b16404b4-f690-44dc-8db0-8f48ec568590\""));
        assertTrue(result.contains("\"createdAt\":\"2026-01-27 12:05:34\""));
        assertTrue(result.contains("\"sentAt\":\"2026-01-27 12:05:04\""));
    }

    @Test
    public void deveLancarExcecaoQuandoIdDoUsuarioEstiverFaltando() {
        // Given: JSON sem o ID do usuário
        String message = """
                {
                    "user": { "username":"teste" },
                    "log": { "message":"oi", "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";
        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(message));
        assertTrue(exception.getMessage().contains("User ou Log ausentes")
                || exception.getMessage().contains("Invalid user ID"));
    }

    @Test
    public void deveLancarExcecaoQuandoIdDoUsuarioEstiverVazio() {
        // Given: JSON com o ID vazio
        String message = """
                {
                    "user": { "id":"" },
                    "log": { "message":"oi", "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";
        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(message));
        assertTrue(exception.getMessage().contains("Invalid user ID"));
    }

    @Test
    public void deveLancarExcecaoQuandoOLogEstiverAusente() {
        // Given: JSON sem o log
        String message = "{ \"user\": { \"id\":\"123\" } }";

        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(message));
        assertTrue(exception.getMessage().contains("Log is missing"));
    }

    @Test
    public void deveLancarExcecaoQuandoOUsuarioEstiverAusente() {
        // Given
        String message = "{ \"log\": { \"message\":\"oi\", \"sentAt\":\"01-27-2026T12:05:04.001Z\" } }";

        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(message));
        assertTrue(exception.getMessage().contains("User is missing"));
    }

    @Test
    public void deveLancarExcecaoQuandoSentAtEstiverAusenteOuVazio() {
        // Given
        String messageWithoutSentAt = """
                {
                    "user": { "id":"123" },
                    "log": { "message":"oi" }
                }""";

        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(messageWithoutSentAt));
        assertTrue(exception.getMessage().contains("Invalid sentAt"));

        // Given
        String messageEmptySentAt = """
                {
                    "user": { "id":"123" },
                    "log": { "message":"oi", "sentAt":"" }
                }""";
        // When e Then
        Exception exception2 = assertThrows(Exception.class, () -> modifiJson.execute(messageEmptySentAt));
        assertTrue(exception2.getMessage().contains("Invalid sentAt"));
    }

    @Test
    public void deveLancarExcecaoQuandoMensagemEstiverAusenteOuVazia() {
        // Given
        String messageWithoutMsg = """
                {
                    "user": { "id":"123" },
                    "log": { "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";

        // When e Then
        Exception exception = assertThrows(Exception.class, () -> modifiJson.execute(messageWithoutMsg));
        assertTrue(exception.getMessage().contains("Invalid message content"));

        // Given
        String messageEmptyMsg = """
                {
                    "user": { "id":"123" },
                    "log": { "sentAt":"01-27-2026T12:05:04.001Z", "message":"" }
                }""";

        // When e Then
        Exception exception2 = assertThrows(Exception.class, () -> modifiJson.execute(messageEmptyMsg));
        assertTrue(exception2.getMessage().contains("Invalid message content"));
    }
}
package com.eletra.business.service;

import com.eletra.business.TestcontainersConfiguration;
import com.eletra.business.model.entities.ProcessEntity;
import com.eletra.business.model.entities.TicketsEntity;
import com.eletra.business.model.enums.ProcessStatus;
import com.eletra.business.model.enums.TicketsStatus;
import com.eletra.business.producer.BusinessProducer;
import com.eletra.business.repositories.ProcessRepository;
import com.eletra.business.repositories.TicketRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ModifyJsonTest {

    @Autowired
    private ModifyJson modifyJson;

    // Trocamos o JmsTemplate pelo seu Producer
    @MockitoSpyBean
    private BusinessProducer producer;

    @MockitoBean
    private Clock clock;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProcessAndTicket processAndTicket;

    // Método auxiliar para não repetirmos código nos testes de exceção
    private UUID createValidTicket() {
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        return ticket.getId();
    }

    @Test
    public void jsonMustBeConvertedCorrectlyEIdSentToQueue() throws Exception {
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

        UUID ticketId = createValidTicket();

        // When: Executa a lógica
        modifyJson.execute(message, ticketId);

        // Then: 1. Verifica se o Producer enviou o ID (e captura ele)
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(producer, Mockito.times(1)).send(idCaptor.capture());

        String idEnviadoParaFila = idCaptor.getValue();
        assertNotNull(idEnviadoParaFila, "O ID enviado para a fila não pode ser nulo");

        // Then: 2. Vai no banco de dados buscar o processo que acabou de ser criado
        ProcessEntity processoSalvo = processRepository.findById(UUID.fromString(idEnviadoParaFila))
                .orElseThrow(() -> new AssertionError("Processo não encontrado no banco com o ID da fila"));

        String payloadModificado = processoSalvo.getPayload();

        // Then: 3. Valida se o JSON dentro do banco está com o formato novo correto!
        assertTrue(payloadModificado.contains("\"username\":\"b16404b4-f690-44dc-8db0-8f48ec568590\""));
        assertTrue(payloadModificado.contains("\"createdAt\":\"2026-01-27 12:05:34\""));
        assertTrue(payloadModificado.contains("\"sentAt\":\"2026-01-27 12:05:04\""));
        assertEquals(ProcessStatus.SUCCESS, processoSalvo.getStatus(), "O status do processo deve ser SUCCESS");
    }

    @Test
    public void mustThrowExceptionWhenUserIdIsMissing() {
        // Given
        UUID ticketId = createValidTicket();
        String message = """
                {
                    "user": { "username":"teste" },
                    "log": { "message":"oi", "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";

        // When + Then
        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(message, ticketId));
        assertTrue(exception.getMessage().contains("User ou Log ausentes")
                || exception.getMessage().contains("Invalid user ID"));

        // Valida se o banco salvou o processo com status de ERROR e se preservou o payload problemático
        java.util.List<ProcessEntity> processos = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticketId))
                .toList();
        
        assertFalse(processos.isEmpty(), "Deveria ter criado um processo no banco");
        ProcessEntity processError = processos.get(0);
        assertEquals(ProcessStatus.ERROR, processError.getStatus(), "O status do processo deve ser ERROR");
        assertEquals(message, processError.getPayload(), "O payload deve conter o JSON com erro");
    }

    @Test
    public void mustThrowExceptionWhenUserIdIsEmpty() {
        UUID ticketId = createValidTicket();
        String message = """
                {
                    "user": { "id":"" },
                    "log": { "message":"oi", "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";

        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(message, ticketId));
        assertTrue(exception.getMessage().contains("Invalid user ID"));
    }

    @Test
    public void shouldThrowExceptionWhenLOGIsMissing() {
        UUID ticketId = createValidTicket();
        String message = "{ \"user\": { \"id\":\"123\" } }";

        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(message, ticketId));
        assertTrue(exception.getMessage().contains("Log is missing"));
    }

    @Test
    public void shouldThrowExceptionWhenUserIsMissing() {
        UUID ticketId = createValidTicket();
        String message = "{ \"log\": { \"message\":\"oi\", \"sentAt\":\"01-27-2026T12:05:04.001Z\" } }";

        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(message, ticketId));
        assertTrue(exception.getMessage().contains("User is missing"));
    }

    @Test
    public void shouldThrowExceptionWhenSentAtIsMissingOrEmpty() {
        UUID ticketId = createValidTicket();

        String messageWithoutSentAt = """
                {
                    "user": { "id":"123" },
                    "log": { "message":"oi" }
                }""";
        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(messageWithoutSentAt, ticketId));
        assertTrue(exception.getMessage().contains("Invalid sentAt"));

        String messageEmptySentAt = """
                {
                    "user": { "id":"123" },
                    "log": { "message":"oi", "sentAt":"" }
                }""";
        Exception exception2 = assertThrows(Exception.class, () -> modifyJson.execute(messageEmptySentAt, ticketId));
        assertTrue(exception2.getMessage().contains("Invalid sentAt"));
    }

    @Test
    public void shouldThrowExceptionWhenMessageIsMissingOrEmpty() {
        UUID ticketId = createValidTicket();

        String messageWithoutMsg = """
                {
                    "user": { "id":"123" },
                    "log": { "sentAt":"01-27-2026T12:05:04.001Z" }
                }""";
        Exception exception = assertThrows(Exception.class, () -> modifyJson.execute(messageWithoutMsg, ticketId));
        assertTrue(exception.getMessage().contains("Invalid message content"));

        String messageEmptyMsg = """
                {
                    "user": { "id":"123" },
                    "log": { "sentAt":"01-27-2026T12:05:04.001Z", "message":"" }
                }""";
        Exception exception2 = assertThrows(Exception.class, () -> modifyJson.execute(messageEmptyMsg, ticketId));
        assertTrue(exception2.getMessage().contains("Invalid message content"));
    }

    @Test
    void mustCheckWhenNotCreatingProcess() {
        // Given
        UUID ticketId = UUID.randomUUID();

        // When + Then
        Exception exception = assertThrows(Exception.class, () -> processAndTicket.createMyProcess("", ticketId));
        assertFalse(exception.getMessage().contains("ticket não encontrado"));
    }

    @Test
    void shouldTriggerPreUpdateUsingRepository() throws Exception {
        TicketsEntity ticket = new TicketsEntity();
        ticket.setStatus(TicketsStatus.OPEN);

        ticket = ticketRepository.saveAndFlush(ticket);

        LocalDateTime oldUpdatedAt = ticket.getUpdatedAt();

        Thread.sleep(5);

        ticket.setStatus(TicketsStatus.IN_PROCESS);

        ticket = ticketRepository.saveAndFlush(ticket);

        assertTrue(ticket.getUpdatedAt().isAfter(oldUpdatedAt));
    }
}
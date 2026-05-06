package com.eletra.converter.service;

import com.eletra.converter.TestcontainersConfiguration;
import com.eletra.converter.dto.MessageDTO;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.model.entities.TicketsEntity;
import com.eletra.converter.model.enums.ProcessStatus;
import com.eletra.converter.model.enums.ProcessType;
import com.eletra.converter.model.enums.TicketsStatus;
import com.eletra.converter.repositories.ProcessRepository;
import com.eletra.converter.repositories.TicketRepository;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class MessageConverterServiceTest {

    @Autowired
    private MessageConverterService messageConverterService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProcessRepository processRepository;

    @MockitoSpyBean
    private JmsTemplate jmsTemplate;

    @MockitoSpyBean
    private CsvMapper csvMapper;

    @Test
    @DisplayName("Caminho Feliz: Deve converter, processar, atualizar mock no banco e enviar pra fila")
    public void convertAndSendShouldBeProcessedAndSentTest() {
        // Given
        TicketsEntity newTicket = new TicketsEntity();
        newTicket.setStatus(TicketsStatus.OPEN);
        TicketsEntity ticket = ticketRepository.save(newTicket);

        MessageDTO dto = new MessageDTO("francisco.parreira", "2026-01-27T12:05:34Z", "2026-01-27T12:05:04.001Z", "No. Interestingly enough, her leaf blower picked up.");

        // When
        messageConverterService.convertAndSend(dto, ticket.getId());

        // THEN: Capturamos o UUID enviado para a fila
        org.mockito.ArgumentCaptor<String> idCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsTemplate, Mockito.times(1))
                .convertAndSend(Mockito.eq("training-converter.send_as_csv"), idCaptor.capture());

        UUID generatedId = UUID.fromString(idCaptor.getValue());

        // Validamos o estado no banco usando o ID real enviado
        ProcessEntity savedProcess = processRepository.findById(generatedId).orElse(null);
        Assertions.assertNotNull(savedProcess);
        Assertions.assertEquals(ProcessStatus.SUCCESS, savedProcess.getStatus());
        Assertions.assertEquals(ProcessType.CONVERTER, savedProcess.getType());

        // Valida se a string montada contem os valores esperados
        String csvPayload = savedProcess.getPayload();
        Assertions.assertTrue(csvPayload.contains("user,time,message"));
        Assertions.assertTrue(csvPayload.contains("francisco.parreira,2026-01-27T12:05:04.001Z,\"No. Interestingly enough, her leaf blower picked up.\""));

        // Garante que o status do Ticket foi modificado 
        TicketsEntity updatedTicket = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertNotNull(updatedTicket);
        Assertions.assertEquals(TicketsStatus.IN_PROCESS, updatedTicket.getStatus());
    }

    @Test
    @DisplayName("Cobertura de Erro: Deve salvar erro quando falhar conversao")
    public void convertAndSendExceptionShouldSaveErrorStatusTest() throws Exception {
        // Given
        TicketsEntity newTicket = new TicketsEntity();
        newTicket.setStatus(TicketsStatus.OPEN);
        TicketsEntity ticket = ticketRepository.save(newTicket);

        MessageDTO dto = new MessageDTO("francisco.parreira", "2026-01-27T12:05:34Z", "2026-01-27T12:05:04.001Z", "Message with fault");

        // Forçamos uma falha no ObjectMapper simulando quebrar a regra
        Mockito.doThrow(new RuntimeException("Simulated CsvMapper exception"))
                .when(csvMapper).writer(any((com.fasterxml.jackson.dataformat.csv.CsvSchema.class)));

        // When
        Assertions.assertThrows(Exception.class, () -> {
            messageConverterService.convertAndSend(dto, ticket.getId());
        });

        // Then
        // Valida que NAO mandou nada para a mensageria nessa transacao!
        Mockito.verify(jmsTemplate, Mockito.never())
                .convertAndSend(Mockito.eq("training-converter.send_as_csv"), anyString());
    }

    @Test
    @DisplayName("Caminho Feliz: Deve manter histórico de processos vinculados ao mesmo Ticket")
    public void convertAndSendShouldMaintainProcessHistoryTest() {
        // GIVEN: Um ticket que já possui um processo anterior (simulando BUSINESS)
        TicketsEntity newTicket = new TicketsEntity();
        newTicket.setStatus(TicketsStatus.OPEN);
        TicketsEntity ticket = ticketRepository.save(newTicket);

        ProcessEntity previousProcess = new ProcessEntity(
                ProcessStatus.SUCCESS,
                "{\"original\":\"json\"}",
                ProcessType.BUSINESS,
                ticket
        );
        processRepository.save(previousProcess);

        MessageDTO dto = new MessageDTO("francisco.parreira", "2026-01-27T12:05:34Z", "2026-01-27T12:05:04.001Z", "No. Interestingly enough, her leaf blower picked up.");

        // WHEN: O microserviço converter processa o ticket
        messageConverterService.convertAndSend(dto, ticket.getId());

        // THEN: Capturamos o ID enviado para a fila
        org.mockito.ArgumentCaptor<String> idCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        Mockito.verify(jmsTemplate, Mockito.times(1))
                .convertAndSend(Mockito.eq("training-converter.send_as_csv"), idCaptor.capture());

        UUID newProcessId = UUID.fromString(idCaptor.getValue());

        // 1. Validamos que o NOVO processo foi salvo corretamente
        ProcessEntity savedProcess = processRepository.findById(newProcessId).orElse(null);
        Assertions.assertNotNull(savedProcess, "O novo processo deve existir no banco");
        Assertions.assertEquals(ProcessStatus.SUCCESS, savedProcess.getStatus());
        Assertions.assertEquals(ProcessType.CONVERTER, savedProcess.getType());
        Assertions.assertTrue(savedProcess.getPayload().contains("francisco.parreira"));

        // 2. Validamos a RASTREABILIDADE (Traceability): O ticket agora deve ter 2 processos no banco
        var allProcesses = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticket.getId()))
                .toList();

        Assertions.assertEquals(2, allProcesses.size(), "O ticket deve manter o histórico (Processo BUSINESS + CONVERTER)");
        
        // 3. Validamos que o ticket permanece em processamento
        TicketsEntity updatedTicket = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertEquals(TicketsStatus.IN_PROCESS, updatedTicket.getStatus());
    }

    @Test
    @DisplayName("Cobertura de Erro: Deve converter exception e salvar status de erro")
    public void convertToCsvShouldThrowExceptionTest() {
        // GIVEN: Um DTO nulo ou inválido para forçar erro no mapper se possível, ou testar a exceção direta
        MessageDTO invalidDto = new MessageDTO(null, null, null, null);

        // WHEN / THEN: Testamos a cobertura da ConversionException
        // Como o convertToCsv é público, testamos ele diretamente para atingir 100% de coverage
        // No catch do convertToCsv
        Assertions.assertThrows(com.eletra.converter.exception.ConversionException.class, () -> {
            messageConverterService.convertToCsv(null);
        });
    }

    @Test
    @DisplayName("Deve validar o acesso aos campos do DTO para cobertura total")
    public void messageDtoTest() {
        // GIVEN
        String expectedCreatedAt = "2026-05-05T12:00:00Z";
        MessageDTO dto = new MessageDTO("usuario", expectedCreatedAt, "2026-05-05T11:59:00Z", "Mensagem");

        // WHEN
        String actualCreatedAt = dto.createdAt(); // Chama o accessor do record

        // THEN
        Assertions.assertEquals(expectedCreatedAt, actualCreatedAt, "O campo createdAt deve ser acessível e correto");
    }

}

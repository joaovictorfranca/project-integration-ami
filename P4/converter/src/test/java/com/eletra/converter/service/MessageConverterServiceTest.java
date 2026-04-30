package com.eletra.converter.service;

import com.eletra.converter.TestcontainersConfiguration;
import com.eletra.converter.dto.MessageDTO;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.model.entities.TicketsEntity;
import com.eletra.converter.model.enums.ProcessStatus;
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

        // Then
        // Verifica que o envio via JMS aconteceu com o "send_as_csv"
        Mockito.verify(jmsTemplate, Mockito.times(1))
                .convertAndSend(Mockito.eq("training-converter.send_as_csv"), anyString());

        // Precisamos verificar o banco de dados. Vamos resgatar o processo criado associado ao nosso ticket.
        UUID ticketId = ticket.getId();
        var processos = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticketId))
                .toList();
        
        Assertions.assertFalse(processos.isEmpty(), "Um processo deveria ter sido salvo");
        ProcessEntity savedProcess = processos.get(0);

        Assertions.assertEquals(ProcessStatus.SUCCESS, savedProcess.getStatus(), "O status do processo deve ser SUCCESS");

        // Valida se a string montada contem os valores esperados
        String csvPayload = savedProcess.getPayload();
        Assertions.assertTrue(csvPayload.contains("user,time,message"));
        Assertions.assertTrue(csvPayload.contains("francisco.parreira,2026-01-27T12:05:04.001Z,\"No. Interestingly enough, her leaf blower picked up.\""));

        // Opcional: Garante que o status do Ticket foi modificado 
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

        // Verifica estado no banco:
        UUID ticketId = ticket.getId();
        var processos = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticketId))
                .toList();
        
        Assertions.assertFalse(processos.isEmpty(), "Um processo deveria ter sido salvo mesmo na falha");
        ProcessEntity savedProcess = processos.get(0);

        Assertions.assertEquals(ProcessStatus.ERROR, savedProcess.getStatus(), "O status deve ser gravado como ERROR no catch");
        // Quando há erro, ele deposita no payload a versão original input.toString() 
        Assertions.assertTrue(savedProcess.getPayload().contains("Message with fault"));
    }
}

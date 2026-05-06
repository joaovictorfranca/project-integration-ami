package com.eletra.network_ftp.controller;

import com.eletra.network_ftp.TestcontainersConfiguration;
import com.eletra.network_ftp.model.entities.ProcessEntity;
import com.eletra.network_ftp.model.entities.TicketsEntity;
import com.eletra.network_ftp.model.enums.ProcessStatus;
import com.eletra.network_ftp.model.enums.ProcessType;
import com.eletra.network_ftp.model.enums.TicketsStatus;
import com.eletra.network_ftp.repositories.ProcessRepository;
import com.eletra.network_ftp.repositories.TicketRepository;
import com.eletra.network_ftp.service.CsvFtpService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class JmsControllerTest {

    @Autowired
    private JmsController jmsController;

    @MockitoSpyBean
    private CsvFtpService csvFtpService;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("GIVEN ID de processo do converter WHEN receber mensagem THEN deve delegar para CsvFtpService")
    public void receiveCsvShouldDelegateToServiceTest() throws Exception {
        // GIVEN: Um ticket e um processo vindo do CONVERTER
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        ProcessEntity prevProcess = processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, "csv_payload", ProcessType.CONVERTER, ticket));

        // WHEN
        Assertions.assertDoesNotThrow(() -> {
            jmsController.receiveCsv(prevProcess.getId());
        });

        // THEN: Validamos se a service foi chamada com o payload e ticket ID corretos
        Mockito.verify(csvFtpService, Mockito.times(1))
                .execute(ArgumentMatchers.eq("csv_payload"), ArgumentMatchers.eq(ticket.getId()));
    }

    @Test
    @DisplayName("GIVEN ID inexistente WHEN receber mensagem THEN não deve chamar o serviço")
    public void receiveCsvShouldHandleNotFoundTest() throws Exception {
        // GIVEN
        UUID invalidId = UUID.randomUUID();

        // WHEN
        Assertions.assertDoesNotThrow(() -> {
            jmsController.receiveCsv(invalidId);
        });

        // THEN
        Mockito.verify(csvFtpService, Mockito.never())
                .execute(ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }
}

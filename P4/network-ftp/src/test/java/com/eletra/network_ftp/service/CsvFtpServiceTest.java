package com.eletra.network_ftp.service;

import com.eletra.network_ftp.TestcontainersConfiguration;
import com.eletra.network_ftp.model.entities.ProcessEntity;
import com.eletra.network_ftp.model.entities.TicketsEntity;
import com.eletra.network_ftp.model.enums.ProcessStatus;
import com.eletra.network_ftp.model.enums.ProcessType;
import com.eletra.network_ftp.model.enums.TicketsStatus;
import com.eletra.network_ftp.repositories.ProcessRepository;
import com.eletra.network_ftp.repositories.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class CsvFtpServiceTest {

    @Autowired
    private CsvFtpService csvFtpService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProcessRepository processRepository;

    @MockitoSpyBean
    private DefaultFtpSessionFactory ftpSessionFactory;

    @Test
    @DisplayName("GIVEN ticket com histórico WHEN finalizar fluxo no FTP THEN status deve ser DONE e histórico completo")
    public void executeShouldSaveToFtpAndMarkTicketAsDoneTest() throws Exception {
        // GIVEN: Um ticket que já passou por BUSINESS e CONVERTER
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        
        processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, "{}", ProcessType.BUSINESS, ticket));
        processRepository.save(new ProcessEntity(ProcessStatus.SUCCESS, "csv_data", ProcessType.CONVERTER, ticket));

        String csvContent = "user,time,message\nadmin,2026-05-05,hello";

        // WHEN: Executa o serviço final de FTP
        csvFtpService.execute(csvContent, ticket.getId());

        // THEN: 
        // 1. O Ticket deve estar DONE
        TicketsEntity updatedTicket = ticketRepository.findById(ticket.getId()).orElse(null);
        Assertions.assertEquals(TicketsStatus.DONE, updatedTicket.getStatus());

        // 2. Um novo processo de tipo NETWORK_FTP deve ter sido criado com SUCCESS
        var processes = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticket.getId()))
                .toList();
        
        Assertions.assertEquals(3, processes.size(), "Deve conter o histórico: BUSINESS, CONVERTER e NETWORK_FTP");
        
        ProcessEntity finalProcess = processes.stream()
                .filter(p -> p.getType() == ProcessType.NETWORK_FTP)
                .findFirst().orElse(null);
        
        Assertions.assertNotNull(finalProcess);
        Assertions.assertEquals(ProcessStatus.SUCCESS, finalProcess.getStatus());
    }

    @Test
    @DisplayName("GIVEN falha técnica no FTP WHEN executar THEN processo deve ser salvo com status ERROR")
    public void executeShouldSaveErrorStatusWhenFtpFailsTest() throws Exception {
        // GIVEN
        TicketsEntity ticket = ticketRepository.save(new TicketsEntity(TicketsStatus.IN_PROCESS));
        
        // Forçamos erro na factory do FTP
        Mockito.doThrow(new RuntimeException("FTP Connection Refused"))
                .when(ftpSessionFactory).getSession();

        // WHEN / THEN
        Assertions.assertThrows(Exception.class, () -> {
            csvFtpService.execute("any_csv", ticket.getId());
        });

        // Verificamos se o registro de erro foi persistido
        var processes = processRepository.findAll().stream()
                .filter(p -> p.getTicket().getId().equals(ticket.getId()))
                .filter(p -> p.getType() == ProcessType.NETWORK_FTP)
                .toList();

        Assertions.assertFalse(processes.isEmpty());
        Assertions.assertEquals(ProcessStatus.ERROR, processes.get(0).getStatus());
    }
}

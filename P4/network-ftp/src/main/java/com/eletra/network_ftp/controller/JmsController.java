package com.eletra.network_ftp.controller;

import com.eletra.network_ftp.model.entities.ProcessEntity;
import com.eletra.network_ftp.repositories.ProcessRepository;
import com.eletra.network_ftp.service.CsvFtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Controller
public class JmsController {

    private final CsvFtpService csvFtpService;

    private final ProcessRepository processRepository;

    /**
     * Listener que monitora a fila de CSVs vindos do P1 (Converter).
     * Se houver erro no processamento, a Exception lançada faz
     * com que a mensagem permaneça na fila para nova tentativa.
     */
    @JmsListener(destination = "training-converter.send_as_csv")
    public void receiveCsv(UUID idPProcess) throws Exception {

        try {
            log.info("Iniciando processamento FTP...");

            log.info("ID do converter recebido: {} Agora vamos procurá-lo no banco", idPProcess);

            // O findById retorna Optional. Usamos o .orElseThrow para lançar erro se não achar.
            ProcessEntity process = processRepository.findById(idPProcess)
                    .orElseThrow(() -> new RuntimeException("Processo não encontrado com o ID: " + idPProcess));

            // Agora você tem acesso ao Ticket e ao Payload que salvou lá no outro microservico
            String payloadOriginal = process.getPayload();

            log.info("Payload recuperado do banco: {}", payloadOriginal);

            // Aciona a services que gerencia o stream e o envio
            csvFtpService.execute(payloadOriginal, process.getTicket().getId());

            log.info("Processamento finalizado com sucesso para a mensagem atual.");

        } catch (Exception e) {
            log.error("Falha ao processar mensagem", e);
        }

    }
}
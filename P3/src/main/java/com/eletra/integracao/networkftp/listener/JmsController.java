package com.eletra.integracao.networkftp.listener;

import com.eletra.integracao.networkftp.service.CsvFtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Controller;

@Log4j2
@RequiredArgsConstructor
@Controller
public class JmsController {

    private final CsvFtpService csvFtpService;

    /**
     * Listener que monitora a fila de CSVs vindos do P1 (Converter).
     * Se houver erro no processamento, a Exception lançada faz
     * com que a mensagem permaneça na fila para nova tentativa.
     */
    @JmsListener(destination = "training-converter.send_as_csv")
    public void receiveCsv(String message) throws Exception {
        log.info("Mensagem recebida da fila. Iniciando processamento FTP...");

        // Aciona a service que gerencia o stream e o envio
        csvFtpService.execute(message);

        log.info("Processamento finalizado com sucesso para a mensagem atual.");
    }
}
package com.eletra.integracao.networkftp.service;

import com.eletra.integracao.networkftp.producer.EnviaParaFtp;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class CsvFtpService {

    private final EnviaParaFtp enviaParaFtp;

    public void execute(String csvContent) throws Exception {

        String uniqueID = UUID.randomUUID().toString().substring(0, 8); // Pega só os 8 primeiros caracteres do ID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));

        String fileName = "data_" + timestamp + "_" + uniqueID + ".csv";

        // Criamos o stream aqui
        try (InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))) {
            // Apenas passamos o stream para o especialista em envio
            enviaParaFtp.envia(inputStream, fileName);
            log.info("Processo de envio para o arquivo {} iniciado.", fileName);
        } catch (Exception e) {
            log.error("Falha no fluxo de processamento do CSV: {}", e.getMessage());
            throw e; // Mantemos o throw para o Artemis saber que falhou
        }
    }
}
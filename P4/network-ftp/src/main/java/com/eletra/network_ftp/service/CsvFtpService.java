package com.eletra.integracao.networkftp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class CsvFtpService {

    private final DefaultFtpSessionFactory ftpSessionFactory;

    public void execute(String csvContent) throws Exception {

        String uniqueID = UUID.randomUUID().toString().substring(0, 8); // Pega só os 8 primeiros caracteres do ID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));

        String fileName = "data_" + timestamp + "_" + uniqueID + ".csv";

        // Criamos o stream aqui
        try (InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))) {
            // Apenas passamos o stream para o especialista em envio
            send(inputStream, fileName);
            log.info("Processo de envio para o arquivo {} iniciado.", fileName);
        } catch (Exception e) {
            log.error("Falha no fluxo de processamento do CSV: {}", e.getMessage());
            throw e; // Mantemos o throw para o Artemis saber que falhou
        }
    }

    private void send(InputStream data, String fileName) throws IOException {
        // Abre e fecha a conexão com o FTP automaticamente aqui
        try (FtpSession session = ftpSessionFactory.getSession()) {
            session.write(data, fileName);
            log.info("Upload concluído: {}", fileName);
        } catch (Exception e) {
            log.error("Erro técnico no upload FTP: {}", e.getMessage());
            throw e;
        }
    }
}
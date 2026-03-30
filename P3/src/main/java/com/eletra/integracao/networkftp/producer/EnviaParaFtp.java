package com.eletra.integracao.networkftp.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Log4j2
@Service
@RequiredArgsConstructor
public class EnviaParaFtp {
    private final DefaultFtpSessionFactory ftpSessionFactory;

    public void envia(InputStream data, String fileName) throws IOException {
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

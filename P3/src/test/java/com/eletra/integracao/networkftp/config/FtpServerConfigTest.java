package com.eletra.integracao.networkftp.config;

import org.apache.ftpserver.FtpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
@TestPropertySource(properties = {
        "application.ftp.pasv_ports=30000-30001",
        "application.ftp.username=user_test",
        "application.ftp.password=pass_test",
        "application.ftp.host=localhost",
        "application.ftp.port=2221",
        "application.ftp.listener=default",
        "application.ftp.root_directory=test_ftp_dir"
})
class FtpServerConfigTest {

    @Autowired
    private DefaultFtpSessionFactory ftpSessionFactory;

    @Autowired
    private FtpServer ftpServer;

    @Test
    @DisplayName("GIVEN propriedades válidas WHEN contexto inicia THEN deve configurar factory e servidor")
    void deveConfigurarComponentesFtpComSucesso() {
        // GIVEN - Propriedades injetadas via @TestPropertySource

        // WHEN - O Spring Boot sobe o contexto (Autowired faz isso)

        // THEN
        Assertions.assertNotNull(ftpSessionFactory, "A Factory de sessão deveria ter sido criada");
        Assertions.assertNotNull(ftpServer, "O Servidor FTP deveria ter sido iniciado");
        Assertions.assertFalse(ftpServer.isStopped(), "O Servidor FTP deve estar rodando");
    }

    @Test
    @DisplayName("GIVEN configuração de diretório WHEN servidor inicia THEN deve criar pasta no user.home")
    void deveCriarDiretorioRaizComSucesso() {
        // GIVEN
        String rootDir = "test_ftp_dir";
        Path expectedPath = Paths.get(System.getProperty("user.home"), rootDir);

        // WHEN - O Bean ftpServer foi instanciado

        // THEN
        Assertions.assertTrue(Files.exists(expectedPath), "O diretório do FTP deveria ter sido criado automaticamente");

        expectedPath.toFile().delete();
    }
}
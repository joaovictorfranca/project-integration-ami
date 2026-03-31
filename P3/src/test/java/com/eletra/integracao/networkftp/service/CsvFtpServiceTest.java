package com.eletra.integracao.networkftp.service;

import com.eletra.integracao.networkftp.TestcontainersConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

import java.io.IOException;


@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CsvFtpServiceTests {

    @Autowired
    private CsvFtpService csvFtpService;

    @Autowired
    private DefaultFtpSessionFactory ftpSessionFactory;

    @Test
    @DisplayName("Deve processar e armazenar o CSV no servidor FTP com sucesso")
    void csvShouldBeStored() {
        // GIVEN (Dado que...)
        String csv = "user,time,message\n\"id123\",\"2026-03-30\",\"Teste FTP\"";

        // WHEN (Quando...)
        Assertions.assertDoesNotThrow(() -> {
            csvFtpService.execute(csv);
        });

        // THEN (Então...)
        // Para garantir a lógica, verificamos se a sessão FTP consegue "enxergar" algum arquivo
        try (FtpSession session = ftpSessionFactory.getSession()) {
            String[] files = session.listNames("/");
            Assertions.assertTrue(files.length > 0, "O servidor FTP deveria conter pelo menos um arquivo!");
        } catch (IOException e) {
            Assertions.fail("Erro ao verificar arquivo no servidor FTP: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver falha técnica (Cobre o bloco Catch)")
    void shouldThrowExceptionOnFailure() {
        // GIVEN (Dado que enviamos um conteúdo nulo ou inválido que force um erro no stream ou no producer)
        // Ou poderíamos mockar o Producer para lançar erro aqui
        String csvInvalido = null;

        // WHEN & THEN (Quando e Então...)
        Assertions.assertThrows(Exception.class, () -> {
            csvFtpService.execute(csvInvalido);
        }, "Deveria lançar exceção para cobrir o bloco catch da Service");
    }
}
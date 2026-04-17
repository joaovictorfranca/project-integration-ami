package com.eletra.integracao.networkftp.service;

import com.eletra.integracao.networkftp.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringBootTest
@Import(TestcontainersConfiguration.class) // Importa o seu servidor FTP com a porta dinâmica
class CsvFtpServiceTest {

    @Autowired
    private CsvFtpService csvFtpService;

    @MockitoSpyBean
    private DefaultFtpSessionFactory ftpSessionFactory;

    @Mock
    private FtpSession ftpSession;

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

    @Test
    @DisplayName("GIVEN dados válidos WHEN enviar para FTP THEN deve realizar upload e fechar sessão")
    void deveEnviarArquivoComSucesso() throws Exception {
        // GIVEN
        String fileName = "teste.csv";
        // Mockamos a sessão para garantir que o Spring use o nosso mock dentro do método privado
        when(ftpSessionFactory.getSession()).thenReturn(ftpSession);

        // WHEN - Chamamos o EXECUTE (que por dentro chama o envia privado)
        csvFtpService.execute(fileName);

        // THEN - Verificamos se o que estava no método privado aconteceu
        verify(ftpSession, times(1)).write(any(InputStream.class), anyString());
        verify(ftpSession, times(1)).close();
    }

    @Test
    @DisplayName("GIVEN erro na escrita WHEN enviar para FTP THEN deve logar, lançar exceção e fechar sessão")
    void deveLancarExcecaoQuandoEscritaFalhar() throws IOException {
        // GIVEN
        String fileName = "erro.csv";
        when(ftpSessionFactory.getSession()).thenReturn(ftpSession);

        // Simula erro durante a escrita dentro do método privado
        doThrow(new IOException("Falha de rede FTP")).when(ftpSession).write(any(), anyString());

        // WHEN & THEN
        assertThrows(IOException.class, () -> {
            csvFtpService.execute(fileName);
        });

        // Garante que mesmo com erro o try-with-resources fechou a sessão
        verify(ftpSession, times(1)).close();
    }

    @Test
    @DisplayName("GIVEN erro ao abrir sessão WHEN enviar para FTP THEN deve relançar erro (Cobre falha na factory)")
    void deveLancarExcecaoQuandoNaoConseguirSessao() {
        // GIVEN
        // Forçamos erro logo na abertura da sessão (primeira linha do try-with-resources do envia)
        when(ftpSessionFactory.getSession()).thenThrow(new RuntimeException("Servidor Offline"));

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> {
            csvFtpService.execute("qualquer conteudo");
        });
    }
}
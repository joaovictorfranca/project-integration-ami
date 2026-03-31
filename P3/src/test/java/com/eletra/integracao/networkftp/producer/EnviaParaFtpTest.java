package com.eletra.integracao.networkftp.producer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnviaParaFtpTest {

    @Mock
    private DefaultFtpSessionFactory ftpSessionFactory;

    @Mock
    private FtpSession ftpSession;

    @InjectMocks
    private EnviaParaFtp enviaParaFtp;

    @Test
    @DisplayName("GIVEN dados válidos WHEN enviar para FTP THEN deve realizar upload e fechar sessão")
    void deveEnviarArquivoComSucesso() throws IOException {
        // GIVEN
        String fileName = "teste.csv";
        InputStream data = new ByteArrayInputStream("conteudo".getBytes());
        when(ftpSessionFactory.getSession()).thenReturn(ftpSession);

        // WHEN
        enviaParaFtp.envia(data, fileName);

        // THEN
        verify(ftpSession, times(1)).write(data, fileName);
        verify(ftpSession, times(1)).close(); // Garante que o try-with-resources fechou a sessão
    }

    @Test
    @DisplayName("GIVEN erro na escrita WHEN enviar para FTP THEN deve logar, lançar exceção e fechar sessão")
    void deveLancarExcecaoQuandoEscritaFalhar() throws IOException {
        // GIVEN
        String fileName = "erro.csv";
        InputStream data = new ByteArrayInputStream("dados".getBytes());
        when(ftpSessionFactory.getSession()).thenReturn(ftpSession);

        // Simula erro durante a escrita no FTP
        doThrow(new IOException("Falha de rede FTP")).when(ftpSession).write(any(), anyString());

        // WHEN & THEN
        assertThrows(IOException.class, () -> {
            enviaParaFtp.envia(data, fileName);
        });

        verify(ftpSession, times(1)).close(); // O try-with-resources deve fechar mesmo com erro
    }

    @Test
    @DisplayName("GIVEN erro ao abrir sessão WHEN enviar para FTP THEN deve relançar erro (Cobre falha na factory)")
    void deveLancarExcecaoQuandoNaoConseguirSessao() {
        // GIVEN
        when(ftpSessionFactory.getSession()).thenThrow(new RuntimeException("Servidor Offline"));

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> {
            enviaParaFtp.envia(mock(InputStream.class), "arquivo.csv");
        });
    }
}
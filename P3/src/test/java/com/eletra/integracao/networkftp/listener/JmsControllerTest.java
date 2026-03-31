package com.eletra.integracao.networkftp.listener;

import com.eletra.integracao.networkftp.service.CsvFtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JmsControllerTest {

    @Mock
    private CsvFtpService csvFtpService;

    @InjectMocks
    private JmsController jmsController;

    @Test
    @DisplayName("Deve chamar a service com sucesso ao receber mensagem da fila")
    void deveProcessarMensagemComSucesso() throws Exception {
        // GIVEN (Dado que...)
        String mensagemSimulada = "id;nome;valor\n1;Teste;100.0";

        // WHEN (Quando...)
        jmsController.receiveCsv(mensagemSimulada);

        // THEN (Então...)
        // Verifica se o método execute da service foi chamado exatamente 1 vez com a String correta
        verify(csvFtpService, times(1)).execute(mensagemSimulada);
    }

    @Test
    @DisplayName("Deve repassar a exceção caso a service falhe")
    void deveLancarExcecaoQuandoServiceFalhar() throws Exception {
        // GIVEN
        String mensagemErro = "dados_invalidos";
        // Lançando uma Exception genérica para casar com o 'throws Exception' do método
        Exception erroSimulado = new Exception("Falha técnica");
        doThrow(erroSimulado).when(csvFtpService).execute(anyString());

        // WHEN & THEN
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            jmsController.receiveCsv(mensagemErro);
        });

        // Garante que o fluxo passou pela service antes de estourar o erro
        verify(csvFtpService, times(1)).execute(mensagemErro);
    }
}
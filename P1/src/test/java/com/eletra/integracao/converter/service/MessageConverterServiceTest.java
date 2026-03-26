package com.eletra.integracao.converter.service;

import com.eletra.integracao.converter.dto.MessageDTO;
import com.eletra.integracao.converter.exception.ConversionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class MessageConverterServiceTest {

    @MockitoSpyBean
    private MessageConverterService service;



    @Test
    @DisplayName("Deve converter MessageDTO para CSV no formato esperado")
    void deveConverterParaCsvComSucesso() {
        // Given
        MessageDTO dto = new MessageDTO(
                "Tereza",
                "2026-08-24 14:00:00",
                "2026-08-24 13:59:00",
                "Mensagem simples"
        );

        // When
        String resultado = service.convertToCsv(dto);

        String esperado = """
                user,time,message
                Tereza,"2026-08-24 13:59:00","Mensagem simples"
                """.trim();

        // Then
        Assertions.assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve usar sentAt como campo time no CSV")
    void deveUsarSentAtComoTimeNoCsv() {
        // Given
        MessageDTO dto = new MessageDTO(
                "joao.franca",
                "2026-03-10 10:00:00",
                "2026-03-10 10:05:00",
                "Teste"
        );
        // When
        String resultado = service.convertToCsv(dto);

        // Then
        Assertions.assertTrue(resultado.contains("2026-03-10 10:05:00"));
        Assertions.assertFalse(resultado.contains("2026-03-10 10:00:00"));
    }

    @Test
    @DisplayName("Deve gerar CSV com aspas nos campos de saída")
    void deveGerarCsvComAspasNosCamposDeSaida() {
        // Given
        MessageDTO dto = new MessageDTO(
                "bot",
                "2026-03-10 10:00:00",
                "2026-03-10 10:05:00",
                "Texto com, virgula"
        );

        // When
        String resultado = service.convertToCsv(dto);

        String esperado = """
                user,time,message
                bot,"2026-03-10 10:05:00","Texto com, virgula"
                """.trim();

        // Then
        Assertions.assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve enviar o CSV convertido para a fila correta")
    void deveConverterEEnviarParaFilaCorreta() {
        // Given
        MessageDTO dto = new MessageDTO(
                "Tereza",
                "2026-08-24 14:00:00",
                "2026-08-24 13:59:00",
                "Mensagem simples"
        );

        // When
        String csvEsperado = """
                user,time,message
                Tereza,"2026-08-24 13:59:00","Mensagem simples"
                """.trim();

        service.convertAndSend(dto);
        // Then
        Mockito.verify(service).convertAndSend(dto);
    }

    @Test
    @DisplayName("Deve lançar ConversionException quando receber valores nulos")
    void deveLancarConversionExceptionQuandoReceberValoresNulos() {
        // Given
        MessageDTO dto = new MessageDTO(
                "user",
                "2026-01-01 10:00:00",
                "2026-01-01 10:01:00",
                null
        );

        // When + Then
        ConversionException exception = Assertions.assertThrows(
                ConversionException.class,
                () -> service.convertToCsv(dto)
        );

        Assertions.assertEquals("Erro ao converter mensagem para CSV", exception.getMessage());
        Assertions.assertNotNull(exception.getCause());
    }
}
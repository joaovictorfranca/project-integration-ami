package com.eletra.integracao.converter.service;

import com.eletra.integracao.converter.dto.MessageDTO;
import com.eletra.integracao.converter.exception.ConversionException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageConverterServiceTest {

    private JmsTemplate jmsTemplate;
    private MessageConverterService service;

    @BeforeEach
    void setUp() {
        jmsTemplate = mock(JmsTemplate.class);
        service = new MessageConverterService(new CsvMapper(), jmsTemplate);
    }

    @Test
    @DisplayName("Deve converter MessageDTO para CSV no formato esperado")
    void deveConverterParaCsvComSucesso() {
        MessageDTO dto = new MessageDTO(
                "Tereza",
                "2026-08-24 14:00:00",
                "2026-08-24 13:59:00",
                "Mensagem simples"
        );

        String resultado = service.convertToCsv(dto);

        String esperado = """
                user,time,message
                Tereza,"2026-08-24 13:59:00","Mensagem simples"
                """.trim();

        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve usar sentAt como campo time no CSV")
    void deveUsarSentAtComoTimeNoCsv() {
        MessageDTO dto = new MessageDTO(
                "joao.franca",
                "2026-03-10 10:00:00",
                "2026-03-10 10:05:00",
                "Teste"
        );

        String resultado = service.convertToCsv(dto);

        assertTrue(resultado.contains("2026-03-10 10:05:00"));
        assertFalse(resultado.contains("2026-03-10 10:00:00"));
    }

    @Test
    @DisplayName("Deve gerar CSV com aspas nos campos de saída")
    void deveGerarCsvComAspasNosCamposDeSaida() {
        MessageDTO dto = new MessageDTO(
                "bot",
                "2026-03-10 10:00:00",
                "2026-03-10 10:05:00",
                "Texto com, virgula"
        );

        String resultado = service.convertToCsv(dto);

        String esperado = """
                user,time,message
                bot,"2026-03-10 10:05:00","Texto com, virgula"
                """.trim();

        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve enviar o CSV convertido para a fila correta")
    void deveConverterEEnviarParaFilaCorreta() {
        MessageDTO dto = new MessageDTO(
                "Tereza",
                "2026-08-24 14:00:00",
                "2026-08-24 13:59:00",
                "Mensagem simples"
        );

        String csvEsperado = """
                user,time,message
                Tereza,"2026-08-24 13:59:00","Mensagem simples"
                """.trim();

        service.convertAndSend(dto);

        verify(jmsTemplate).convertAndSend("training-converter.send_as_csv", csvEsperado);
    }

    @Test
    @DisplayName("Deve lançar ConversionException quando receber valores nulos")
    void deveLancarConversionExceptionQuandoReceberValoresNulos() {
        MessageDTO dto = new MessageDTO(
                "user",
                "2026-01-01 10:00:00",
                "2026-01-01 10:01:00",
                null
        );

        ConversionException exception = assertThrows(
                ConversionException.class,
                () -> service.convertToCsv(dto)
        );

        assertEquals("Erro ao converter mensagem para CSV", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}
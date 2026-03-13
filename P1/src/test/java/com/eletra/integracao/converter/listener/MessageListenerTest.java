package com.eletra.integracao.converter.listener;

import com.eletra.integracao.converter.dto.MessageDTO;
import com.eletra.integracao.converter.service.MessageConverterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MessageListenerTest {

    @Mock
    private MessageConverterService converterService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MessageListener messageListener;

    @Test
    @DisplayName("Deve ler o JSON, converter para MessageDTO e enviar para o service")
    void deveProcessarMensagemComSucesso() throws Exception {
        // Given
        String jsonBruto = """
                {
                  "username": "Tereza",
                  "createdAt": "2026-08-24 14:00:00",
                  "sentAt": "2026-08-24 13:59:00",
                  "message": "yipe hey, yipe ho... e uma garrafa de rum!"
                }
                """;

        MessageDTO dto = new MessageDTO(
                "Tereza",
                "2026-08-24 14:00:00",
                "2026-08-24 13:59:00",
                "yipe hey, yipe ho... e uma garrafa de rum!"
        );

        Mockito.when(objectMapper.readValue(jsonBruto, MessageDTO.class)).thenReturn(dto);

        // When
        messageListener.onMessage(jsonBruto);

        // Then
        ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
        Mockito.verify(converterService).convertAndSend(captor.capture());

        MessageDTO capturado = captor.getValue();
        Assertions.assertEquals("Tereza", capturado.username());
        Assertions.assertEquals("2026-08-24 14:00:00", capturado.createdAt());
        Assertions.assertEquals("2026-08-24 13:59:00", capturado.sentAt());
        Assertions.assertEquals("yipe hey, yipe ho... e uma garrafa de rum!", capturado.message());
    }

    @Test
    @DisplayName("Não deve chamar o service quando o JSON for inválido")
    void naoDeveChamarServiceQuandoJsonForInvalido() throws Exception {
        // Given
        String jsonInvalido = "{ json invalido }";

        Mockito.when(objectMapper.readValue(jsonInvalido, MessageDTO.class))
                .thenThrow(new RuntimeException("Erro ao desserializar"));

        // When & Then
        Assertions.assertDoesNotThrow(() -> messageListener.onMessage(jsonInvalido));

        Mockito.verify(converterService, Mockito.never()).convertAndSend(Mockito.any(MessageDTO.class));
    }
}

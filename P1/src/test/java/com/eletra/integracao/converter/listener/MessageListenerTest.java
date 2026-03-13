package com.eletra.integracao.converter.listener;

import com.eletra.integracao.converter.dto.MessageDTO;
import com.eletra.integracao.converter.service.MessageConverterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
        // Arrange
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

        org.mockito.Mockito.when(objectMapper.readValue(jsonBruto, MessageDTO.class)).thenReturn(dto);

        // Act
        messageListener.onMessage(jsonBruto);

        // Assert
        ArgumentCaptor<MessageDTO> captor = ArgumentCaptor.forClass(MessageDTO.class);
        verify(converterService).convertAndSend(captor.capture());

        MessageDTO capturado = captor.getValue();
        assertEquals("Tereza", capturado.username());
        assertEquals("2026-08-24 14:00:00", capturado.createdAt());
        assertEquals("2026-08-24 13:59:00", capturado.sentAt());
        assertEquals("yipe hey, yipe ho... e uma garrafa de rum!", capturado.message());
    }

    @Test
    @DisplayName("Não deve chamar o service quando o JSON for inválido")
    void naoDeveChamarServiceQuandoJsonForInvalido() throws Exception {
        // Arrange
        String jsonInvalido = "{ json invalido }";

        org.mockito.Mockito.when(objectMapper.readValue(jsonInvalido, MessageDTO.class))
                .thenThrow(new RuntimeException("Erro ao desserializar"));

        // Act + Assert
        assertDoesNotThrow(() -> messageListener.onMessage(jsonInvalido));

        verify(converterService, never()).convertAndSend(org.mockito.Mockito.any(MessageDTO.class));
    }
}

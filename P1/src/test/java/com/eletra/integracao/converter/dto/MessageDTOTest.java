package com.eletra.integracao.converter.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve mapear JSON para MessageDTO corretamente")
    void deveMapearJsonParaRecordCorretamente() throws Exception {
        String json = """
                {
                    "username": "admin",
                    "createdAt": "2026-01-01",
                    "sentAt": "2026-01-02",
                    "message": "Teste unitario"
                }
                """;

        MessageDTO dto = objectMapper.readValue(json, MessageDTO.class);

        assertEquals("admin", dto.username());
        assertEquals("2026-01-01", dto.createdAt());
        assertEquals("2026-01-02", dto.sentAt());
        assertEquals("Teste unitario", dto.message());
    }
}
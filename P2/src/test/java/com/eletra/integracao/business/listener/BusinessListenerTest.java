package com.eletra.integracao.business.listener;

import com.eletra.integracao.business.TestcontainersConfiguration;
import com.eletra.integracao.business.service.ModifiJson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class) // Usa o seu Artemis do Testcontainers
@ActiveProfiles("test")
public class BusinessListenerTest {

    @Autowired
    private BusinessListener businessListener;

    // Usamos Spy para verificar se o Listener realmente chamou a Service
    @MockitoSpyBean
    private ModifiJson modifiJson;

    @Test
    public void menssagenDelegadaAoServico() throws Exception {
        // Given
        String message = """
                {
                    "user": {
                        "id":"b16404b4-f690-44dc-8db0-8f48ec568590",
                        "username":"francisco.parreira",
                        "firstName":"Lorraine",
                        "lastName":"Almeida",
                        "employeeCode":"640708",
                        "position":"gardener",
                        "cpf":"534.670.770-05"
                    },
                    "log": {
                        "id":"9580ab40-b0b6-42cb-bb8f-7c1e1f654f6a",
                        "sentAt":"01-27-2026T12:05:04.001Z",
                        "message":"No. Interestingly enough, her leaf blower picked up.",
                        "format":null
                    }
                }""";

        // When
        Assertions.assertDoesNotThrow(() -> {
            businessListener.onMessage(message);
        });

        // Then: Verifica se o Listener passou a bola para a Service ModifiJson
        Mockito.verify(modifiJson, Mockito.times(1)).execute(message);
    }

    @Test
    public void lidandoComJsonMalformado() throws Exception {
        // Given: JSON malformado
        String malformedMessage = """
            {
                "user" { "id": "123" }
            }""";

        // When & Then
        // 1. Verificamos que o Listener não deixa a exceção subir (o try-catch dele funciona)
        Assertions.assertDoesNotThrow(() -> {
            businessListener.onMessage(malformedMessage);
        });

        // 2. Opcional: Em vez de "never", verificamos que ele FOI chamado,
        // mas sabemos que ele falhou internamente.
        Mockito.verify(modifiJson, Mockito.times(1)).execute(malformedMessage);

        // Isso garante que o fluxo passou pelo Listener e entrou na Service!
    }
}
package com.eletra.integracao.network_grpc.services; // Ajuste para seu package real

import io.grpc.stub.StreamObserver;
import com.eletra.integracao.network_grpc.configs.TestcontainersConfig;
import com.eletra.integracao.network_grpc.grpc.TransactionRequest;
import com.eletra.integracao.network_grpc.grpc.TransactionResponse;
import com.eletra.integracao.network_grpc.repositories.TicketRepository;
import com.eletra.integracao.network_grpc.repositories.ProcessRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@Import(TestcontainersConfig.class)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
public class TransactionGrpcServiceTest {

    @Autowired
    private TransactionGrpcService transactionGrpcService;

    @MockitoSpyBean
    private ProcessService processService;

    @MockitoSpyBean
    private TicketRepository ticketRepository;

    @MockitoSpyBean
    private ProcessRepository processRepository;

    private final String validPayload = """
                {
                    "user":
                        {
                            "id":"b16404b4-f690-44dc-8db0-8f48ec568590",
                            "username":"francisco.parreira",
                            "firstName":"Lorraine",
                            "lastName":"Almeida",
                            "employeeCode":"640708",
                            "position":"gardener",
                            "cpf":"534.670.770-05"
                        },
                    "log":
                        {
                            "id":"9580ab40-b0b6-42cb-bb8f-7c1e1f654f6a",
                            "sentAt":"01-27-2026T12:05:04.001Z",
                            "message":"No. Interestingly enough, her leaf blower picked up.",
                            "format":null
                        }
                }""";

    @Test
    @DisplayName("Caminho Feliz: Deve criar Ticket, Processo e retornar RECEIVED")
    public void transactionShouldBeProcessedTest() {
        // Given
        TransactionRequest request = TransactionRequest.newBuilder()
                .setJsonPayload(validPayload)
                .build();

        // Mock do observer para verificar chamadas
        StreamObserver<TransactionResponse> responseObserver = Mockito.mock(StreamObserver.class);

        // When
        transactionGrpcService.createTransaction(request, responseObserver);

        // Then
        ArgumentCaptor<TransactionResponse> responseCaptor = ArgumentCaptor.forClass(TransactionResponse.class);

        // Verifica se enviou a resposta de sucesso
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        Assertions.assertEquals("OPEN", responseCaptor.getValue().getStatus());

        // Verifica se o fluxo de Processo foi chamado
        Mockito.verify(processService, Mockito.times(1)).startIntegration(anyString());

        // Verifica se salvou no banco (Ticket e Processo)
        Mockito.verify(ticketRepository, Mockito.atLeastOnce()).save(any());
        Mockito.verify(processRepository, Mockito.atLeastOnce()).save(any());

        Mockito.verify(responseObserver).onCompleted();
    }

    @Test
    @DisplayName("Cobertura de Erro: Deve chamar onError quando o serviço falhar")
    public void shouldHandleErrorWhenServiceFailsTest() {
        // Given
        TransactionRequest request = TransactionRequest.newBuilder().setJsonPayload(validPayload).build();
        StreamObserver<TransactionResponse> responseObserver = Mockito.mock(StreamObserver.class);

        // Forçamos um erro no seu serviço de processo
        Mockito.doThrow(new RuntimeException("Falha catastrófica"))
                .when(processService).startIntegration(anyString());

        // When
        transactionGrpcService.createTransaction(request, responseObserver);

        // Then
        // Aqui você cobre a linha do "catch" e o método "onError" do gRPC
        Mockito.verify(responseObserver).onError(any(Throwable.class));
        Mockito.verify(responseObserver, Mockito.never()).onCompleted();
    }

    @Test
    @DisplayName("Validar Payload: Deve lidar com payload vazio ou nulo")
    public void shouldHandleEmptyPayloadTest() {
        // Given
        TransactionRequest request = TransactionRequest.newBuilder().setJsonPayload("").build();
        StreamObserver<TransactionResponse> responseObserver = Mockito.mock(StreamObserver.class);

        // When
        transactionGrpcService.createTransaction(request, responseObserver);

        // Then
        // Deve falhar a validação (Fail-Fast) e chamar onError com INVALID_ARGUMENT
        Mockito.verify(responseObserver, Mockito.atLeastOnce()).onError(any(io.grpc.StatusRuntimeException.class));
        Mockito.verify(responseObserver, Mockito.never()).onNext(any());
        Mockito.verify(responseObserver, Mockito.never()).onCompleted();

    }
}
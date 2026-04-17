package com.eletra.integracao.network_grpc.services;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.eletra.integracao.network_grpc.grpc.EletraISServiceGrpc;
import com.eletra.integracao.network_grpc.grpc.TransactionRequest;
import com.eletra.integracao.network_grpc.grpc.TransactionResponse;
import com.eletra.integracao.network_grpc.models.entities.ProcessEntity;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Log4j2
@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcService extends EletraISServiceGrpc.EletraISServiceImplBase {

    private final ProcessService processService;

    @Override
    public void createTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {
            log.info("Requisição gRPC recebida. Iniciando fluxo...");

            String jsonPayload = request.getJsonPayload();
            if (jsonPayload.trim().isEmpty()) {
                responseObserver.onError(
                        io.grpc.Status.INVALID_ARGUMENT
                                .withDescription("Payload JSON não pode ser vazio")
                                .asRuntimeException()
                );
                return;
            }

            // 1. Chama o serviço para salvar Ticket e Processo
            ProcessEntity process = processService.startIntegration(jsonPayload);
            UUID processId = process.getId();
            UUID ticketId = process.getTicket().getId();

            // 2. Dispara o ID do processo para o Artemis (Assíncrono)
            // Feito antes da resposta para prevenir falha silenciosa
            processService.sendToQueue(processId);

            // 3. Monta a resposta para o cliente gRPC
            TransactionResponse response = TransactionResponse.newBuilder()
                    .setTicketId(ticketId.toString())
                    .setStatus("OPEN")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Resposta gRPC enviada. Ticket ID: {}, Process ID: {}", ticketId, processId);

        } catch (Exception e) {
            log.error("Erro ao processar transação gRPC: ", e);
            responseObserver.onError(e);
        }
    }
}
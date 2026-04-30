//package com.eletra.converter.listener;
//
//import com.eletra.converter.dto.MessageDTO;
//import com.eletra.converter.service.MessageConverterService;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.jms.annotation.JmsListener;
//import org.springframework.stereotype.Component;
//
//
//@Log4j2
//@Component
//@RequiredArgsConstructor
//public class MessageListener {
//
//    private final MessageConverterService converterService;
//    private final ObjectMapper objectMapper; // Injetado do config acima
//
//    @JmsListener(destination = "training-converter.send_as_json")
//    public void onMessage(String jsonBruto) { // Recebe String!
//        try {
//
//            System.out.println("Recebendo a Mensagem ...\n\n");
//
//            log.info("JSON bruto recebido: {}", jsonBruto);
//
//            // Converte a String manualmente para o seu Record
//            MessageDTO message = objectMapper.readValue(jsonBruto, MessageDTO.class);
//
//            log.info("Processando mensagem de: {}", message.username());
//
//            converterService.convertAndSend(message);
//            log.info("Mensagem processada com sucesso!");
//
//        } catch (Exception e) {
//            log.error("Falha ao processar mensagem", e);
//        }
//    }
//}

package com.eletra.converter.listener;

import com.eletra.converter.dto.MessageDTO;
import com.eletra.converter.model.entities.ProcessEntity;
import com.eletra.converter.repositories.ProcessRepository;
import com.eletra.converter.service.MessageConverterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Log4j2
@Component
@RequiredArgsConstructor
public class MessageListener {

    private final MessageConverterService converterService;
    private final ObjectMapper objectMapper; // Injetado do config acima
    private final ProcessRepository processRepository;

    @JmsListener(destination = "training-converter.send_as_json")
    public void onMessage(UUID idPProcess) { // Recebe id do processo anterior!
        try {

            System.out.println("Recebendo ID do Business ...\n\n");

            log.info("ID do business recebido: {} Agora vamos procurá-lo no banco", idPProcess);

            // O findById retorna Optional. Usamos o .orElseThrow para lançar erro se não achar.
            ProcessEntity process = processRepository.findById(idPProcess)
                    .orElseThrow(() -> new RuntimeException("Processo não encontrado com o ID: " + idPProcess));

            // Agora você tem acesso ao Ticket e ao Payload que salvou lá no outro microservico
            String payloadOriginal = process.getPayload();

            log.info("Payload recuperado do banco: {}", payloadOriginal);

            // Converte a String manualmente para o seu Record
            MessageDTO message = objectMapper.readValue(payloadOriginal, MessageDTO.class);

            log.info("Processando mensagem de: {}", message.username());

            converterService.convertAndSend(message, process.getTicket().getId());
            log.info("Mensagem processada com sucesso!");

        } catch (Exception e) {
            log.error("Falha ao processar mensagem", e);
        }
    }
}
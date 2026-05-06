# Projeto Integration Network (gRPC to Artemis)

Este microsserviço é o ponto de entrada de alta performance para o processamento das mensagens. Ele atua como um *Gateway*, recebendo requisições via **gRPC**, persistindo os dados e disparando eventos para processamento assíncrono.

---

## Microsserviço Business (Core Logic)

O **Business** é o segundo elo da cadeia de processamento. Ele é responsável por aplicar as regras de negócio e garantir a integridade dos dados antes da conversão final. As principais atribuições para atender ao **Projeto 5 (Padrões AMI)** são:

* **Auditabilidade & Rastreabilidade**: Cada execução é vinculada ao `Ticket` original, mas o microsserviço cria seu próprio `ProcessEntity` (tipo `BUSINESS`). Isso permite auditar exatamente o que o Business recebeu e o que ele gerou, pois cada ```process``` tem seu payload e seu status sendo atualizado quando necessário.
* **Processamento Assíncrono**: Consome eventos da fila `receive_as_json`.
* **Transformação de Dados**: Realiza o parsing do JSON original (`ReceivedMessageDTO`), valida campos obrigatórios e transforma para o formato padronizado de saída (`SentMessageDTO`), incluindo a formatação rigorosa de timestamps em UTC.
* **Continuidade da Saga**: Após o processamento com sucesso, ele persiste o novo payload e encaminha o ID do seu processo para a próxima fila (`send_as_json`), mantendo a coreografia da saga ativa.

---

### Microsserviço Converter

O **Converter** atua como o motor de transformação de formato:
* **Consumo**: Recebe o ID do processo vindo do Business pela fila ```send_as_json```.
* **Transformação**: Converte o JSON estruturado em um formato **CSV** pronto para transferência externa.
* **Traceability**: Cria um `ProcessEntity` de tipo `CONVERTER`, registrando o payload em CSV no banco de dados.
* **Evento**: Encaminha o ID do ```process``` criado para a fila de saída final ```send_as_csv``` para o próximo serviço.

---

### Microsserviço Network-FTP

O **Network-FTP** é o estágio final da integração:
* **Entrega**: Consome o CSV e realiza o upload para o servidor **FTP** configurado que salva os CSVs em um PATH na máquina.
* **Finalização**: É o responsável por marcar o `TicketsEntity` como `DONE`, sinalizando que toda a jornada da mensagem foi concluída com sucesso.
* **Auditoria Final**: Registra o último `ProcessEntity` da cadeia, garantindo que o histórico completo (do gRPC ao FTP) esteja disponível para consulta.

---

##  Arquitetura e Tecnologias

O projeto foi construído utilizando as melhores práticas de microsserviços modernos:

* **Java 17 & Spring Boot 4**: Core do ecossistema.
* **gRPC (Protobuf)**: Protocolo binário para comunicação rápida e eficiente.
* **PostgreSQL**: Armazenamento relacional persistente.
* **Flyway**: Controle de versão do esquema de banco de dados (Migrations).
* **ActiveMQ Artemis**: Broker de mensageria para desacoplamento de processos.
* **Testcontainers**: Testes de integração reais utilizando Docker.
* **JaCoCo**: Monitoramento de cobertura de testes (focado no código de negócio).

---

## Fluxo da Requisição

1. **Entrada**: Um cliente envia um `TransactionRequest` (via gRPC) contendo um JSON.
2. **Validação**: O `TransactionGrpcService` verifica a integridade do payload através do método `createTransaction`.
3. **Persistência**:
    * **Ticket**: É criado para acompanhar todo processo entre os microsserviços e constantemente sendo atualizado o status para um melhor acompanhamento do processo de transformação da mensagem.
    * **Process**: Cria a instância de execução vinculada ao Ticket, cada microserviço cria um **process** informando o tipo de servico e o payload da mensagem já feita a alteração dele.
4. **Processamento Business**:
    * O microserviço **Business** consome o evento, recupera o payload do processo anterior e aplica as transformações de negócio.
    * Um novo **Process** (tipo `BUSINESS`) é criado para registrar essa etapa.
    * O status do **Ticket** é atualizado para `IN_PROCESS`.
5. **Conversão**:
    * O microserviço **Converter** transforma o JSON resultante em um arquivo **CSV**.
    * Um novo **Process** (tipo `CONVERTER`) é gerado.
6. **Entrega Final (FTP)**:
    * O microserviço **Network-FTP** faz o upload do CSV para o servidor destino.
    * O status do **Ticket** é finalmente alterado para **`DONE`**.
7. **Resposta**: O cliente recebe um `ticket_id` e o status `OPEN` imediatamente, enquanto o processamento real ocorre em background para uma posterior consulta completa da mensagem.

---

## Modelo de Dados

### Tabela: `ticket`
Guarda a origem de tudo o que entra no sistema.
* `id` (UUID): Identificador único para rastreamento externo.
* `ticketStatus` (ENUM): Vai está sendo atualizado com frequencia dependendo do estado que se encontra a mensagem e contém os seguintes valores para os microsserviços irem atualizando: `OPEN`, `IN_PROCESS`, `DONE`, `FAILED`.
* `created_at` (TIMESTAMP): Momento exato da entrada.
* `update_at` (TIMESTAMP): Momento exato da atulização.

### Tabela: `process`
Gerencia o ciclo de vida interno da integração.
* `id` (UUID): ID interno usado para comunicação entre microsserviços.
* `ticket_id` (FK): Referência ao ticket de origem.
* `process_status` (ENUM): Estado atual do **process** (Ex: `PENDING`, `PROCESSING`, `SUCCESS`, `ERROR`).
* `process_type` (ENUM): Tipo de processo, ou seja, qual microsserviço é responsável por esse processo (Ex: `NETWORK_GRPC`, `BUSINESS`, `CONVERTER`, `NETWORK_FTP`)
* `payload` (TEXT): Aqui ele perciste as mensagens atualizadas. Pode vir como JSON ou como CSV.
* `created_at` (TIMESTAMP): Momento exato da criação do processo.

---

## Qualidade e Testes

A estratégia de testes garante que mudanças no código não quebrem o contrato gRPC ou a persistência:

* **Mocking**: Uso de `MockitoSpyBean` para validar interações de serviço.
* **Container Reusability**: Postgres e Artemis são subidos uma única vez para todos os testes, acelerando o CI/CD.
* **Coverage**: Configurado para ignorar classes geradas automaticamente pelo gRPC, garantindo foco no código autoral e garante 100% de cobertura.

---

## Dificuldades encontradas

* No `network-grpc` surgiram alguns erros relacionados a conflitos de porta do servidor gRPC quando subiam os testes com a aplicação rodando, esse erro acontecia pois o servidor gRPC dos testes estava tentando subir na mesma porta do servidor da aplicação.

Criei um "Resourses" para os testes e alterei a porta do gRPC nos testes, coloquei o valor 0 para ele, pois assim ele sobe em alguma porta que esteja livre.

* A maior dificuldade encontrada nos serviços de Business, Converter e FTP foi o impacto da nova arquitetura nos testes existentes. Como o sistema passou a trafegar apenas IDs de ```process``` e a buscar o conteúdo real no banco de dados, os testes unitários anteriormente feitos. Foi necessário reconstruir os cenários para incluir o ciclo completo: salvar o estado inicial no banco, disparar o evento via fila e validar a transformação final consultando novamente a persistência.
---

## Comandos Úteis

**Gerar classes do Protobuf no `network-grpc`:**
```bash
./mvnw clean compile
# 🚀 Projeto Integration Network (gRPC to Artemis)

Este microsserviço é o ponto de entrada de alta performance para o processamento de integrações AMI. Ele atua como um *Gateway*, recebendo requisições via **gRPC**, persistindo os dados e disparando eventos para processamento assíncrono.

---

## 🏗️ Arquitetura e Tecnologias

O projeto foi construído utilizando as melhores práticas de microsserviços modernos:

* **Java 17 & Spring Boot 4**: Core do ecossistema.
* **gRPC (Protobuf)**: Protocolo binário para comunicação rápida e eficiente.
* **PostgreSQL**: Armazenamento relacional persistente.
* **Flyway**: Controle de versão do esquema de banco de dados (Migrations).
* **ActiveMQ Artemis**: Broker de mensageria para desacoplamento de processos.
* **Testcontainers**: Testes de integração reais utilizando Docker.
* **JaCoCo**: Monitoramento de cobertura de testes (focado no código de negócio).

---

## 🛠️ Fluxo da Requisição

1.  **Entrada**: Um cliente envia um `TransactionRequest` (via gRPC) contendo um JSON.
2.  **Validação**: O `TransactionGrpcService` verifica a integridade do payload através do método `createTransaction`.
3.  **Persistência**:
    * **Ticket**: É criado para acompanhar todo processo entre os microsserviços e constantemente sendo atualizado o status para um melhor acompanhamento do processo de transformação da mensagem.
    * **Process**: Cria a instância de execução vinculada ao Ticket, cada microserviço cria um **process** informando o tipo de servico e o payload da mensagem já feita a alteração dele.
4.  **Evento**: O ID do **Process** é postado em uma fila do Artemis para o próximo microsserviço ter acesso ao estado da mensagem atual **payload de process**.
5.  **Resposta**: O cliente recebe um `ticket_id` e o status `OPEN` imediatamente, enquanto o processamento real ocorre em background para uma posterior consulta completa da menssagem.

---

## 🗄️ Modelo de Dados

### 🎫 Tabela: `ticket`
Guarda a origem de tudo o que entra no sistema.
* `id` (UUID): Identificador único para rastreamento externo.
* `ticketStatus` (ENUM): Vai está sendo atualizado com frequencia dependendo do estado que se encontra a mensagem e contém os seguintes valores para os microsserviços irem atualizando: `OPEN`, `IN_PROCESS`, `DONE`, `FAILED`.
* `created_at` (TIMESTAMP): Momento exato da entrada.
* `update_at` (TIMESTAMP): Momento exato da atulização.

### ⚙️ Tabela: `process`
Gerencia o ciclo de vida interno da integração.
* `id` (UUID): ID interno usado para comunicação entre microsserviços.
* `ticket_id` (FK): Referência ao ticket de origem.
* `process_status` (ENUM): Estado atual do **process** (Ex: `PENDING`, `PROCESSING`, `SUCCESS`, `ERROR`).
* `process_type` (ENUM): Tipo de processo, ou seja, qual microsserviço é responsável por esse processo (Ex: `NETWORK_GRPC`, `BUSINESS`, `CONVERTER`, `NETWORK_FTP`)
* `payload` (TEXT): Aqui ele perciste as mensagens atualizadas. Pode vir como JSON ou como CSV.
* `created_at` (TIMESTAMP): Momento exato da criação do processo.

---

## 🧪 Qualidade e Testes

A estratégia de testes garante que mudanças no código não quebrem o contrato gRPC ou a persistência:

* **Mocking**: Uso de `MockitoSpyBean` para validar interações de serviço.
* **Container Reusability**: Postgres e Artemis são subidos uma única vez para todos os testes, acelerando o CI/CD.
* **Coverage**: Configurado para ignorar classes geradas automaticamente pelo gRPC, garantindo foco no código autoral e garante 100% de cobertura.

---

## ⚙️ Comandos Úteis

**Gerar classes do Protobuf:**
```bash
./mvnw clean compile
# Plano de implementação: movimentações de estoque

> **Execução:** usar `superpowers:executing-plans` para implementar por tarefas nesta sessão, após revisão deste plano. Este documento não autoriza implementação, testes, build, lint, commit ou alterações em outros módulos além do escopo descrito.

**Objetivo:** cadastrar e consultar movimentações de estoque, atualizando o saldo físico e preservando seu histórico de forma atômica.

**Arquitetura:** `StockMovementService` coordena validações, bloqueio do estoque, cálculo do saldo e persistência em uma única transação. A implementação fica em `stock/movement`, próxima de `StockItem`, seguindo a organização de `customer/address`. O histórico comercial será consultável desde a primeira etapa, mas a criação de movimentos vinculados a vendas e compras será integrada aos respectivos fluxos em uma etapa posterior.

**Tecnologias:** Java, Spring Boot, Spring Data JPA e PostgreSQL, sem novas dependências.

**Especificação de origem:** solicitação desta conversa, `AGENTS.md`, tabelas `stock_items` e `stock_movements` da migration V1 e regras atuais de `StockItemService`. O usuário confirmou que cadastrar uma movimentação deve atualizar o saldo, registrar o histórico na mesma transação, calcular os saldos no backend e proteger as quantidades reservadas. As demais decisões abaixo são propostas para revisão, não regras comerciais já confirmadas.

## Restrições globais

- Não criar nem executar testes automatizados, build ou lint sem solicitação explícita. A verificação desta implementação será revisão direta dos arquivos e do diff.
- Preservar alterações locais em `order`, `order/item`, na migration V1 e nos planos existentes. Conferir novamente o Git antes da execução.
- Não criar frontend, autenticação, CRUD de usuários ou compras, faturamento, reservas ou infraestrutura genérica como efeito colateral.
- Seguir `customer` em nomes, injeção por campos com `@Autowired`, filtros com `Specification.allOf()`, respostas com entidades e exceções em inglês com ponto final.
- Preservar `ResponseErrorDTO` e `GlobalExceptionHandler`.
- Não alterar migrations existentes. O schema atual comporta o escopo inicial.
- Não fazer commit ou push sem solicitação.

## Situação atual conferida

- `stock_movements` existe no schema e nos dados iniciais, mas não possui classes Java.
- Tipos permitidos: `INBOUND`, `OUTBOUND` e `ADJUSTMENT`. Motivos permitidos: `PURCHASE`, `SALE`, `RETURN`, `MANUAL_ADJUSTMENT` e `LOSS`; o motivo é anulável no banco.
- A tabela exige estoque, tipo, quantidade, saldo anterior, saldo posterior, usuário e data. Pedido e compra são opcionais e mutuamente exclusivos.
- `StockItemRepository.findByIdForUpdate(Long id)` já aplica `PESSIMISTIC_WRITE`.
- `StockItemService.update` e `delete` usam esse bloqueio. A troca de produto ou depósito já é rejeitada quando existe histórico.
- O PATCH de estoque ainda aceita alterar quantidade diretamente. Seu contrato precisa ser tratado ao introduzir movimentações para evitar alterações de saldo sem histórico.
- Usuários e compras não possuem entidades/repositories Java. `Order` já mantém `userId` como `Long` enquanto o módulo de usuários não existe.
- Os dados iniciais contêm movimentos comerciais; a consulta não pode escondê-los nem exigir que todos sigam as novas regras de cadastro manual.

## Escopo e decisões propostas

### Etapa 1: operação manual e consulta do histórico completo

- Disponibilizar `GET /stockMovement/search`, `GET /stockMovement/{id}` e `POST /stockMovement`.
- O GET por ID segue `customer`: `200` quando encontrado e `204` quando ausente. Cadastro retorna `201` com a entidade salva.
- Não disponibilizar PATCH nem DELETE de movimentações: saldos e registros históricos não serão reescritos. Correções serão feitas por nova movimentação compensatória, sem endpoint específico de estorno nesta etapa.
- O POST recebe apenas `stockItemId`, `type`, `reason`, `quantity`, `userId` e `notes`. Não recebe saldos, data, pedido ou compra.
- Tipos e motivos do cadastro manual: `INBOUND` com `RETURN` ou `MANUAL_ADJUSTMENT`; `OUTBOUND` com `RETURN`, `LOSS` ou `MANUAL_ADJUSTMENT`; `ADJUSTMENT` somente com `MANUAL_ADJUSTMENT`.
- Exigir motivo no cadastro para rastreabilidade, mantendo leitura de registros antigos com motivo nulo. `PURCHASE` e `SALE` ficam disponíveis no enum e nas consultas, mas sua criação depende da etapa 2.
- Para `INBOUND` e `OUTBOUND`, `quantity` é uma magnitude estritamente positiva. Para `ADJUSTMENT`, é a diferença assinada, positiva ou negativa, nunca zero. Exemplo: saldo 10 e ajuste -3 resultam em 7; ajuste +3 resulta em 13.
- `previousBalance` vem do estoque bloqueado. Entrada soma, saída subtrai e ajuste soma a diferença. `newBalance` deve ser não negativo e maior ou igual a `reservedQuantity`.
- Calcular com `long` intermediário e rejeitar resultados fora de `Integer`, antes de converter. Não permitir que overflow produza um saldo inválido.
- Não alterar `reservedQuantity`, `minQuantity` ou `maxQuantity`. O máximo atual não é um bloqueio de capacidade em `StockItemService`; não criar essa regra incidentalmente.
- Exigir usuário existente e ativo. Manter `userId` como `Long`, consultando o estado persistido por query nativa pontual no repository de movimentações; não criar entidades incompletas de usuário nem expor senha/hash.
- `occurredAt` será definido no backend por `@PrePersist`, como os callbacks de `customer`. Não aceitar data retroativa nesta etapa.
- Normalizar `notes` no service: texto ausente, vazio ou somente espaços vira `null`; texto preenchido usa `trim()`. Não adicionar `@Size` para repetir o tamanho físico da coluna.
- Campos de saldo, data e origem comercial não pertencem ao DTO de cadastro. Mesmo que enviados como propriedades extras, nunca serão usados no cálculo ou persistência; preservar a política atual do projeto para propriedades JSON desconhecidas, sem alterar a configuração global do Jackson ou criar desserializador específico.

### Controle do saldo fora das movimentações

- Preservar o cadastro atual de `StockItem`, incluindo saldo inicial: nesta etapa, ele é o ponto de partida, não um movimento criado automaticamente, pois seu contrato não exige operador.
- No `StockItemService.update`, rejeitar `quantity` quando diferente do saldo persistido, mesmo sem histórico. Campo omitido, nulo ou igual ao atual continua permitido. Orientar a mensagem de erro a usar movimentação para alterar saldo.
- Essa é uma alteração deliberada do contrato de estoque e precisa ser considerada na aprovação do plano. Preservar edição de limites, reservas e associações conforme as regras atuais; não criar um fluxo de reservas nesta tarefa.
- A exclusão de estoque com movimentos deve retornar erro de negócio específico antes do DELETE, reutilizando `existsMovementsByStockItemId`, em vez de depender apenas da FK.

### Etapa 2: integração comercial futura, fora da execução inicial

- Criar movimentos `SALE` e `PURCHASE` na mesma transação da operação comercial que efetivamente baixa ou recebe mercadoria.
- Definir depósito de atendimento, consumo/liberação de reservas, quantidades parciais, devoluções, autorização de operador e proteção contra repetição antes de implementar.
- Usar `orderId` ou `purchaseId`, nunca ambos. Validar existência, produto, status e quantidade contra os respectivos documentos.
- Não expor um POST comercial avulso que permita repetir a baixa de um pedido ou o recebimento de uma compra sem controle do documento de origem.
- Reaproveitar o cálculo e o bloqueio do módulo sem criar antecipadamente interfaces para regras ainda indefinidas.
- Uma movimentação compensatória manual não cancela pedidos, desfaz recebimentos nem altera financeiro.

## Arquivos e responsabilidades

Base dos novos arquivos: `backend/src/main/java/com/pi/erp/stock/movement/`.

| Arquivo | Responsabilidade |
| --- | --- |
| `StockMovement.java` | Entidade de `stock_movements`, associação a `StockItem`, IDs de origem/operador e callback de data. |
| `StockMovementType.java` | Enum com os três tipos existentes no schema. |
| `StockMovementReason.java` | Enum com os cinco motivos existentes no schema. |
| `RequestStockMovementDTO.java` | Record de cadastro manual, sem saldos ou origens comerciais. |
| `StockMovementFilter.java` | Record de filtros opcionais. |
| `StockMovementRepository.java` | Persistência, Specifications e consulta pontual do usuário. |
| `StockMovementService.java` | Busca, cadastro atômico, cálculo e validações. |
| `StockMovementController.java` | HTTP, validação de entrada e delegação. |

Modificar somente `backend/src/main/java/com/pi/erp/stock/StockItemService.java` para proteger quantidade e exclusão. Não criar `PatchStockMovementDTO`, DTO de resposta ou endpoints sem uso no domínio.

## Foco da revisão

1. Duas movimentações concorrentes do mesmo estoque devem calcular seus saldos em sequência sob o bloqueio existente.
2. Saída ou ajuste negativo não pode deixar saldo abaixo da reserva; falha não pode deixar histórico ou saldo parcialmente salvo.
3. Quantidades zero, sinais incompatíveis e overflow devem ser rejeitados antes dos setters.
4. Movimentos comerciais e motivo nulo dos seeds precisam continuar consultáveis, mesmo que não sejam aceitos pelo cadastro manual.
5. O PATCH de estoque não pode continuar alterando saldo físico por fora do histórico; as alterações locais de pedidos devem permanecer intactas.

Esses cenários serão conferidos pela leitura do código e do diff. Concorrência, rollback, binding HTTP e persistência não serão apresentados como validados em execução sem autorização para verificação prática.

## Tarefas de implementação

### Tarefa 1: mapear o histórico e o contrato manual

**Arquivos:** criar entidade, enums e DTO de cadastro na pasta indicada acima.

- [x] Reler `customer/Customer.java`, `customer/RequestCustomerDTO.java`, `stock/StockItem.java` e o trecho de `stock_movements` da V1.
- [x] Criar `StockMovementType` e `StockMovementReason` com os valores exatos do schema.
- [x] Mapear `StockMovement`: `Long id`, `StockItem stockItem`, enums, `Integer quantity`, `Integer previousBalance`, `Integer newBalance`, `Long orderId`, `Long purchaseId`, `Long userId`, `LocalDateTime occurredAt` e `String notes`.
- [x] Usar `@ManyToOne`/`@JoinColumn` para estoque, `@Enumerated(EnumType.STRING)` para enums e callbacks no padrão de `customer`. IDs escalares de origem são uma adaptação temporária à ausência do módulo de compras; não duplicar mapeamento da mesma coluna.
- [x] Criar o record `RequestStockMovementDTO(Long stockItemId, StockMovementType type, StockMovementReason reason, Integer quantity, Long userId, String notes)` com `@NotNull` nos cinco campos obrigatórios. A regra condicional de sinal fica no service.
- [x] Revisar nomes de colunas, nulabilidade, imports, campos por linha e leitura de registros legados.

### Tarefa 2: consultas e acesso ao operador

**Arquivos:** criar filtro e repository.

**Interfaces:** `StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement>`; `Optional<Boolean> findUserActiveById(Long userId)` com query nativa `SELECT active FROM users WHERE user_id = :userId` e `@Param` explícito.

- [x] Criar `StockMovementFilter` com `stockItemId`, `warehouseId`, `productId`, `type`, `reason`, `orderId`, `purchaseId`, `userId`, `occurredFrom` e `occurredTo`. IDs usam `Long`; datas usam `LocalDateTime` e binding ISO explícito.
- [x] Implementar a consulta pontual do usuário no repository. Ausência deve ser distinguível de `active = false`.
- [x] Conferir que não há dependência de entidades de usuários/compras, alteração de schema ou exposição de dados de usuário.

### Tarefa 3: cadastro transacional e busca

**Arquivo:** criar service.

**Interfaces públicas, nesta ordem:** `List<StockMovement> search(StockMovementFilter filter)` e `@Transactional StockMovement register(RequestStockMovementDTO data)`, seguidas dos auxiliares privados.

- [x] Injetar `repository` e `stockItemRepository` por campos com `@Autowired`.
- [x] Implementar busca com `Specification.allOf()` e variável `spec`. Compor igualdade para IDs/enums, caminhos `stockItem.warehouse.id` e `stockItem.product.id`, e intervalo inclusivo de data. Rejeitar início posterior ao fim; filtros ausentes não restringem a busca.
- [x] Implementar auxiliares `StockItem findStockItem(Long stockItemId)` usando `findByIdForUpdate` e `void validateUser(Long userId)` usando a consulta da tarefa 2. ID obrigatório ausente gera `IllegalArgumentException`; recurso ausente gera `ResourceNotFoundException`; usuário inativo gera `IllegalArgumentException`.
- [x] Validar campos obrigatórios também no service, matriz de tipo/motivo, quantidade e operador antes de modificar entidades.
- [x] Bloquear e ler o estoque dentro de `register`, calcular saldo com `long` intermediário e validar limites de `Integer`, saldo não negativo e reserva. Se saldo/reserva persistidos forem nulos, negativos ou inconsistentes, rejeitar a operação sem tentar repará-los silenciosamente.
- [x] Criar o movimento com saldos derivados e notas normalizadas; manter origens comerciais nulas. Atualizar `StockItem.quantity`, salvar estoque e retornar `repository.save(stockMovement)` dentro da mesma transação, sem capturar exceções que devam causar rollback.
- [x] Revisar fluxo de falha, bloqueio mantido até o fim da transação, callback de data e preservação das reservas/limites. Não afirmar garantia de idempotência: repetir um POST bem-sucedido cria outra movimentação.

### Tarefa 4: endpoints

**Arquivo:** criar controller.

- [x] Seguir `CustomerController`, injetando `repository` e `service` e usando `@RequestMapping("/stockMovement")`.
- [x] Implementar `ResponseEntity<List<StockMovement>> search(StockMovementFilter filter)` e `ResponseEntity<StockMovement> findOne(@PathVariable Long id)`.
- [x] Implementar `ResponseEntity<StockMovement> register(@RequestBody @Valid RequestStockMovementDTO data)` com resposta `201`.
- [x] Confirmar GET por ID ausente com `204`, retorno da entidade, interpretação de datas ISO e inexistência de PATCH/DELETE.

### Tarefa 5: proteger o estoque contra alteração sem histórico

**Arquivo:** modificar `backend/src/main/java/com/pi/erp/stock/StockItemService.java`.

- [x] Depois de carregar o estoque bloqueado em `update`, rejeitar `data.quantity()` diferente de `stockItem.getQuantity()`, antes dos setters. Preservar `null`, omissão e envio do mesmo valor.
- [x] Em `delete`, verificar `repository.existsMovementsByStockItemId(id)` depois do bloqueio e lançar `IllegalArgumentException` com mensagem específica quando houver histórico.
- [x] Preservar validações existentes, edição de reservas/limites, cadastro inicial e bloqueio de troca de produto/depósito com movimentos.
- [x] Revisar a interação com a tarefa 3: ambas as operações de escrita devem usar o mesmo bloqueio de estoque, sem chamadas HTTP internas ou transações independentes.

### Tarefa 6: revisão final e entrega

- [x] Comparar os novos arquivos com os equivalentes de `customer` e `customer/address`, justificando IDs escalares temporários, histórico imutável e ausência de PATCH/DELETE pelo domínio.
- [x] Revisar diretamente os cinco cenários de foco, os exemplos de saldo e as consultas de registros comerciais antigos.
- [x] Conferir o diff somente dos arquivos desta tarefa e o estado final do Git, preservando mudanças anteriores. Não executar testes, build ou lint.
- [x] Informar endpoints, campos aceitos, convenção de ajuste, restrição nova no PATCH de estoque e limites da etapa comercial. Separar revisão estática de verificação em execução.

## Critério de conclusão da etapa 1

O código deverá permitir consulta do histórico completo e cadastro manual com saldo e histórico na mesma transação, bloquear alterações diretas de saldo pelo PATCH e impedir exclusão de estoque com histórico. A entrega não será apresentada como integração pronta com pedidos/compras nem como comportamento validado em execução.

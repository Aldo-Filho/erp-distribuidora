# Plano de implementação: reservas e saída comercial do pedido

> Para execução: usar superpowers:executing-plans, tarefa por tarefa, após revisão e aprovação deste plano. Esta entrega é somente documental; não implementar código, alterar schema nem recriar banco agora.

**Objetivo:** reservar estoque ao confirmar, liberar reservas ao cancelar e registrar a saída comercial do pedido de forma atômica, sem emissão fiscal.

**Arquitetura:** OrderService continua coordenando confirmação/cancelamento. StockReservationService controla reservas vinculadas aos itens. OrderDispatchService coordena a saída comercial, usando as reservas e uma operação interna de venda em StockMovementService. Todas as alterações de cada operação participam de uma única transação.

**Tecnologias:** Java, Spring Boot, Spring Data JPA, PostgreSQL e BigDecimal, sem dependências novas.

**Especificação:** usuário definiu depósito por item e confirmou que a operação representa apenas saída comercial; emissão de documento fiscal fica para depois. Os contratos técnicos abaixo são a proposta de implementação a revisar.

## Restrições globais

- Seguir AGENTS.md e o padrão de customer/customer.address; preservar alterações locais anteriores e o módulo stock/movement criado em paralelo.
- Não criar/executar testes, build ou lint sem solicitação explícita. A verificação prevista é revisão direta e independente estática.
- Não fazer commit/push, recriar banco ou aplicar migrations nesta implementação sem nova solicitação.
- Não implementar documento fiscal, NF-e, contas a receber, pagamentos, autenticação, permissões ou devolução de venda concluída.
- Manter preços salvos, cálculo do total, descontos, responsável opcional e uma linha por produto no pedido.
- Manter os endpoints existentes. A saída comercial terá um endpoint adicional; o POST manual de movimentações continua rejeitando SALE/PURCHASE.
- Usar ResourceNotFoundException para recursos ausentes e IllegalArgumentException para regras inválidas, com mensagens específicas em inglês e ponto final.

## Fluxo e contratos propostos

| Operação | Entrada | Efeito |
|---|---|---|
| Confirmar | PATCH /order/{id} com status CONFIRMED | Valida pedido e cria reservas integrais; não altera saldo físico |
| Cancelar pendente | PATCH com status CANCELED | Cancela sem movimentar estoque; não pode haver reserva ativa |
| Cancelar confirmado | PATCH com status CANCELED | Libera somente as reservas desse pedido; preserva saldo físico e histórico |
| Registrar saída comercial | POST /order/{id}/dispatch | Consome reservas, baixa saldo físico, cria OUTBOUND/SALE e muda para INVOICED |

- Manter INVOICED por compatibilidade com o enum/schema atual. Nesta etapa significa saída comercial concluída e não comprova emissão de nota fiscal. A emissão fiscal futura terá fluxo/documento próprio.
- A saída só processa pedido CONFIRMED e atende todos os itens de uma vez. Não há saída parcial, reserva parcial, backorder ou reabertura.
- A operação recebe RequestOrderDispatchDTO com @NotNull Long operatorUserId e String notes opcional. O backend determina itens, quantidades, depósitos, saldos, tipo, motivo, pedido e data.
- operatorUserId identifica quem registra a saída; não é o responsável comercial Order.user. Pedido automático pode continuar sem responsável, mas sua saída exige operador existente e ativo.
- Sem autenticação, esse operador ainda é informado pelo cliente da API. Não apresentar isso como identidade autenticada.
- Resposta da saída: 200 com Order atualizado. A operação altera o pedido existente; não criar um novo pedido.
- paymentMethod continua opcional: uma saída física não cria recebimento financeiro neste escopo.
- Pedido já INVOICED retorna o estado existente sem nova baixa, reserva ou movimento; não substituir o operador dos registros originais. A primeira execução exige operador ativo. O DTO continua exigindo operatorUserId nas requisições.
- Pedido cancelado/pendente não pode executar saída. INVOICED permanece sem cancelamento direto ou retorno a outro status.
- PATCH comum não pode atribuir INVOICED. Apenas a operação de saída executa essa transição com seus efeitos completos.

## Modelo de reserva

Criar stock/reservation/{StockReservation.java,StockReservationStatus.java,StockReservationRepository.java,StockReservationService.java}.

- Tabela stock_reservations: stock_reservation_id, order_item_id, stock_item_id, quantity, status, created_at e updated_at.
- order_item_id obrigatório e único, referenciando order_items; uma reserva por item em seu ciclo de vida, sem apagar registros.
- stock_item_id obrigatório, referenciando o saldo exato de produto + depósito. Não usar apenas o produto para identificar origem.
- quantity inteira positiva e igual à quantidade do item congelado na confirmação.
- Status ACTIVE, RELEASED ou CONSUMED. Confirmar cria ACTIVE; cancelar muda para RELEASED; saída muda para CONSUMED.
- Não criar CRUD público de reservas. Não permitir editar quantidade/origem ou apagar reserva histórica.
- Sem cascata de exclusão para histórico. Pedidos confirmados/cancelados/concluídos e seus itens já não podem ser excluídos pelos services atuais.
- Adicionar stock_reservation_id opcional e único a stock_movements, com FK à reserva. Movimentos manuais/antigos permanecem com null; uma reserva pode originar apenas uma saída.
- Restringir vínculo de reserva a OUTBOUND/SALE com order_id obrigatório e purchase_id ausente. A constraint deve rejeitar reason nulo nesse caso, respeitando o tratamento SQL de null.

## Invariantes de estoque

- quantity é saldo físico; reservedQuantity é o total das reservas ACTIVE daquele StockItem.
- Disponível = quantity − reservedQuantity. Todos os valores devem permanecer entre zero e Integer.MAX_VALUE, e reservedQuantity não pode superar quantity.
- Confirmar: aumentar reservedQuantity, sem reduzir quantity. Exigir disponibilidade para todos os itens antes de concluir.
- Cancelar: reduzir reservedQuantity pelo total das próprias reservas ACTIVE, sem alterar quantity e sem criar entrada física.
- Saída: reduzir quantity e reservedQuantity pela quantidade de cada reserva, marcá-la CONSUMED e criar o movimento com saldo físico anterior/posterior.
- Antes de alterar cada saldo bloqueado, conferir reservedQuantity contra a soma de reservas ACTIVE. Inconsistência deve abortar a operação; não corrigir automaticamente nem descontar reservas de outro pedido.
- Usar long para somas/intermediários e validar antes de converter para Integer.
- Produtos/clientes devem continuar ativos na confirmação, conforme regra atual. A saída de um pedido já confirmado não reprecifica itens nem introduz nova exigência de cliente/produto ativo; exige integridade das reservas e operador ativo.

Exemplo: saldo físico 10, outras reservas 4, pedido de 3 unidades. Confirmar resulta em físico 10 e reservado 7; cancelar volta a físico 10 e reservado 4; executar saída resulta em físico 7 e reservado 4. A reserva de outro pedido permanece intacta.

## Concorrência e ordem dos bloqueios

- Toda operação comercial bloqueia e atualiza o pedido antes de consultar seu estado e antes dos saldos.
- Resolver os IDs dos StockItems para todos os pares produto/depósito; adquirir os bloqueios em ordem crescente de stock_item_id, independentemente da ordem dos itens.
- Depois de bloquear, atualizar a leitura do saldo e revalidar seu produto/depósito: o cadastro pode ter mudado ou sido removido entre a consulta do ID e a aquisição do lock.
- Reservas de um pedido só são alteradas enquanto esse pedido está bloqueado. Consultas de reservas/itens não devem adquirir locks de item após o lock do pedido.
- Ajustar o lock inicial de OrderItemRepository.findByIdForUpdate para FOR NO KEY UPDATE. Os updates/deletes de item continuam serializados, mas a inserção da reserva pode adquirir KEY SHARE da FK para order_items sem esperar um FOR UPDATE de item que já esteja aguardando o pedido.
- O DELETE efetivo do item só ocorre depois de obter o pedido PENDING. Se uma confirmação vencer a disputa, o update/delete do item deve revalidar status e rejeitar antes de alterar/apagar.
- Usar SELECT FOR UPDATE nativo apenas da linha de stock_items em StockItemRepository.findByIdForUpdate, evitando bloquear produto/depósito via joins eager. Manter a assinatura atual para os services de estoque/movimentação.
- Refresh ocorre sob lock antes de modificar a entidade. Não fazer refresh de saldo/reserva/pedido que já recebeu alterações nesta operação.
- Executar flush antes de consultas agregadas que precisem refletir alterações locais. Qualquer erro em um item reverte pedido, reservas, saldos e movimentos de todos os itens.

## Tarefa 1: schema e base de dados coerente

Arquivos: backend/src/main/resources/db/migration/{V1__create-table-erp.sql,V2__insert-initial-data.sql}.

- [x] Seguir a preferência do usuário por recriar o banco: inserir stock_reservations na V1 depois de order_items e antes de stock_movements, com FKs, unicidade, checks e índices para stock_item_id/status.
- [x] Incluir stock_reservation_id opcional em stock_movements, sua FK/unique e a restrição de origem descrita acima. Preservar a exclusividade atual pedido/compra.
- [x] Ajustar a V2 para iniciar reserved_quantity em zero nos cenários existentes, pois seus pedidos estão INVOICED e não representam reservas pendentes. Preservar saldos físicos, movimentos e demais dados.
- [x] Não inventar reservas históricas para os pedidos INVOICED do seed; os movimentos antigos ficam com stock_reservation_id null.
- [x] Não aplicar alterações a banco existente. Se futuramente for necessária migração preservando dados, planejar reconciliação específica; não zerar reservas de uma instância em uso.

## Tarefa 2: entidades, repositories e bloqueios

Arquivos novos: backend/src/main/java/com/pi/erp/stock/reservation/{StockReservation.java,StockReservationStatus.java,StockReservationRepository.java}.

Arquivos alterados: stock/StockItemRepository.java, stock/movement/{StockMovement.java,StockMovementRepository.java} e order/item/OrderItemRepository.java, sob backend/src/main/java/com/pi/erp.

- [x] Mapear reserva com OrderItem, StockItem, quantidade, enum e callbacks no padrão existente, evitando ciclos de serialização.
- [x] Mapear StockMovement.stockReservation opcional com JsonIgnore; preservar os campos/respostas existentes de orderId, purchaseId e userId desta entrega.
- [x] Criar List<StockReservation> findByOrderId(Long orderId), boolean existsByStockItemId(Long stockItemId) e Long sumActiveQuantityByStockItemId(Long stockItemId). A soma deve retornar zero quando vazia.
- [x] Adicionar Optional<Long> findIdByWarehouseIdAndProductId(Long warehouseId, Long productId) ao repository de estoque; a consulta apenas resolve o ID, sem adquirir lock.
- [x] Implementar os ajustes de locks descritos acima e manter a verificação de estado depois do lock do pedido nos services de itens.

## Tarefa 3: reserva e liberação

Arquivo novo: backend/src/main/java/com/pi/erp/stock/reservation/StockReservationService.java.

- [x] Definir reserve(Order order) e release(Order order), com transação obrigatória existente: Propagation.MANDATORY. São operações internas, chamadas somente com o pedido já bloqueado pelo coordenador.
- [x] reserve consulta os itens, resolve e bloqueia os saldos em ordem crescente, revalida os pares, saldos e disponibilidade. Rejeitar reserva prévia inesperada em pedido PENDING; não duplicar nem reaproveitar reserva RELEASED/CONSUMED.
- [x] Validar todos os itens antes das mutações sempre que possível; criar reservas ACTIVE e atualizar reservedQuantity no mesmo contexto/transação.
- [x] release exige conjunto íntegro de reservas ACTIVE para pedido CONFIRMED: uma por item, com quantidade/produto/depósito correspondentes. Bloquear os saldos ordenadamente, validar agregados e liberar apenas essas quantidades.
- [x] Para cancelamento PENDING, não liberar nada; se houver reserva inesperada, rejeitar por inconsistência em vez de esconder dados corrompidos.
- [x] Preservar os registros RELEASED, sem criar StockMovement de entrada/saída física.

## Tarefa 4: integrar confirmação/cancelamento e fechar alterações manuais de reservas

Arquivos: order/OrderService.java, stock/StockItemService.java e, se houver necessidade de refletir o contrato, stock/RequestStockItemDTO.java e product/RequestProductDTO.java.

- [x] Em PENDING → CONFIRMED, manter as validações existentes e chamar reserve antes de salvar CONFIRMED. Pedido e reservas devem confirmar/rollback juntos.
- [x] Em CONFIRMED → CANCELED, chamar release antes de mudar o status. PENDING → CANCELED continua sem saída física.
- [x] Repetir o mesmo status sem dados comerciais não chama reserve/release novamente. Preservar rejeição de cancelamento combinado com alterações comerciais.
- [x] No cadastro de StockItem, permitir saldo físico inicial conforme contrato atual, mas exigir reservedQuantity zero. Preservar a mesma regra quando o estoque é criado pelo cadastro transacional de Product.
- [x] No PATCH de estoque, reservar/reduzir reserva deixa de ser entrada livre: null/omissão/valor igual preserva; valor diferente é rejeitado. Não retirar campos do contrato silenciosamente.
- [x] Impedir exclusão ou troca de produto/depósito de um StockItem que possua qualquer reserva histórica, inclusive RELEASED/CONSUMED, além da proteção já existente para movimentações. Consultar existência sob o lock do saldo, sem bloquear linhas de reservas.
- [x] Não alterar o POST de movimentos manuais para aceitar SALE; manter a proteção existente do saldo físico contra consumo de reservas.

## Tarefa 5: registrar a saída de uma reserva

Arquivos: stock/movement/StockMovementService.java, StockMovement.java e repository de reservas.

- [x] Criar StockMovement registerSale(StockReservation reservation, Long operatorUserId, String notes), interno e com Propagation.MANDATORY. Não expor esta assinatura pelo controller manual.
- [x] O coordenador já deve ter bloqueado o pedido e todos os saldos envolvidos. O método não pode adquirir lock de pedido depois do saldo, nem fazer refresh que descarte mutações pendentes.
- [x] Validar reserva ACTIVE, vínculo com o item/pedido CONFIRMED, par produto/depósito, quantidade e operador ativo. Rejeitar reserva sem identidade persistida ou movimento já vinculado.
- [x] Calcular anterior/posterior no backend, reduzir saldo físico e reservado juntos, marcar a reserva CONSUMED e persistir OUTBOUND/SALE com orderId, operador, reserva, notas e data. purchaseId permanece null.
- [x] Atribuir a esta operação a propriedade exclusiva do consumo de cada reserva: o chamador não deve descontar os saldos ou mudar a reserva para CONSUMED uma segunda vez.
- [x] Preservar a validação de saldo físico remanescente maior ou igual às reservas dos outros pedidos. Falha de persistência/unique/FK reverte a transação inteira.

## Tarefa 6: operação de saída comercial

Arquivos novos: backend/src/main/java/com/pi/erp/order/dispatch/{RequestOrderDispatchDTO.java,OrderDispatchService.java,OrderDispatchController.java}.

- [x] Criar o DTO com operatorUserId obrigatório e notes opcional, sem campos de saldo, quantidade, status ou origem.
- [x] Criar Order dispatch(Long id, RequestOrderDispatchDTO data) em OrderDispatchService, com @Transactional. O controller recebe @RequestBody @Valid e retorna ResponseEntity.ok(order).
- [x] Bloquear/atualizar o pedido; se já INVOICED, retornar sem reprocessar. Exigir CONFIRMED na primeira execução e operador existente/ativo.
- [x] Consultar reservas e itens e exigir cobertura exata de todos os itens por reservas ACTIVE. Rejeitar faltas, sobras, estados/quantidades/origens divergentes, sem tentar reservar automaticamente nesta etapa.
- [x] Resolver/bloquear os StockItems em ordem crescente, revalidar correspondência e agregados. Processar as reservas em ordem determinística, chamando registerSale uma vez por reserva.
- [x] Depois de consumir todas, definir INVOICED e salvar o pedido na mesma transação. Não aceitar nem aplicar alterações comerciais nessa operação.
- [x] Normalizar notes no service: branco vira null, texto preenchido usa trim. Não criar documento fiscal, financeiro ou cobrança.
- [x] Manter o bloqueio de INVOICED no PATCH comum. O método dispatch é uma ação de domínio, justificando seu nome específico fora dos verbos do CRUD.
- [x] Preservar dependências em um só sentido: OrderService → StockReservationService; OrderDispatchService → StockMovementService/repositories. StockMovementService não chama OrderService nem OrderDispatchService.

## Tarefa 7: revisão estática e critérios de aceitação

- [x] Conferir um pedido com produtos de dois depósitos: confirmar reserva ambos; cancelar libera ambos; saída baixa ambos e cria exatamente um SALE por reserva.
- [x] Conferir falta de disponibilidade em um dos depósitos: nenhum status, saldo ou reserva do pedido pode ficar parcialmente alterado.
- [x] Conferir dois pedidos disputando o mesmo saldo e movimentação manual concorrente: locks ordenados e disponibilidade calculada após o lock.
- [x] Conferir confirmação concorrente com edição/exclusão de item: compatibilidade KEY SHARE da nova FK e FOR NO KEY UPDATE, seguida de revalidação do status no editor.
- [x] Conferir dupla confirmação, duplo cancelamento e dupla saída, inclusive repetição após perda da resposta: sem novas reservas/baixas/movimentos.
- [x] Conferir operador diferente do responsável comercial, pedido sem responsável, operador ausente/inativo e cancelamento sem dependência de operador histórico ativo.
- [x] Conferir reservas inconsistentes, quantidade inteira extrema, proteção das reservas de outros pedidos e rejeição de manipulação direta de reservedQuantity.
- [x] Conferir pedidos INVOICED históricos sem novas reservas, seed com reservado zero e preservação de consultas/movimentos manuais existentes.
- [x] Comparar os trechos alterados com customer/address e fazer revisão independente estática de transações, flush, locks, FKs, serialização e efeitos duplicados.
- [x] Registrar explicitamente que testes, banco, binding HTTP, SQL e concorrência em runtime não foram executados. Esta revisão não comprova esses comportamentos em execução.

## Critério de conclusão

Confirmar mantém reservas rastreáveis por item e depósito; cancelar libera somente as próprias reservas; a saída comercial consome reservas e saldos e registra movimentos de venda atomicamente. Repetições não duplicam efeitos. Não existe emissão fiscal nesta etapa, e o enum INVOICED não deve ser apresentado como comprovação de nota emitida.

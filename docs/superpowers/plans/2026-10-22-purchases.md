# Plano de implementação: compras e recebimento de mercadorias

> Plano aprovado pelo usuário em 22/10/2026 e executado com superpowers:executing-plans. Registro da implementação e dos limites de verificação em 2026-10-22-purchases-progress.md. Não aplicar migrations nem recriar banco nesta entrega.

**Objetivo:** cadastrar compras de fornecedores, controlar itens e custo total e receber mercadorias parcial ou integralmente, atualizando o estoque e registrando seu histórico de forma atômica.

**Arquitetura:** PurchaseService coordena o pedido de compra; PurchaseItemService controla seus itens e total. PurchaseReceiptService registra lotes imutáveis de recebimento, atualiza quantidades recebidas e usa uma operação interna de StockMovementService para gerar entradas comerciais. A operação manual de movimentação permanece separada.

**Tecnologias:** Java, Spring Boot, Spring Data JPA, PostgreSQL e BigDecimal, sem novas dependências.

**Especificação:** solicitação do usuário para o módulo de compras, AGENTS.md, tabelas purchases/purchase_items e integrações existentes de fornecedores, produtos, usuários, estoque, reservas e movimentações. As escolhas comerciais abaixo foram aprovadas pelo usuário.

## Restrições globais

- Seguir customer e customer/address na organização, nomes, injeção por campos, DTOs records, Specifications, respostas com entidades e tratamento centralizado de erros.
- Preservar alterações locais anteriores de pedidos, preços, usuários, estoque e suas migrations/documentação. Não reescrever esses módulos para uniformizar código.
- Não criar/executar testes, build ou lint sem solicitação explícita; fazer revisão direta e independente estática.
- Não fazer commit/push, aplicar migrations ou recriar banco nesta implementação sem nova solicitação.
- Não implementar frontend, emissão fiscal, importação de XML, contas a pagar, pagamentos, devoluções ou cancelamento de recebimentos.
- Sugestões automáticas de compra ficam para outra etapa. A primeira versão permite compras manuais.
- Valores inválidos usam IllegalArgumentException; recursos ausentes usam ResourceNotFoundException. Mensagens em inglês e com ponto final.

## Decisões comerciais aprovadas

### Cadastro, itens e custos

- Compra nasce em DRAFT, com total zero e usuário responsável obrigatório, como exige purchases.user_id. Cadastro exige fornecedor e responsável existentes/ativos.
- Cada item tem produto, depósito de destino, quantidade positiva, unitCost e receivedQuantity inicial zero. O cliente não informa status inicial, total ou quantidade já recebida.
- Permitir um depósito diferente por item, seguindo a seleção já adotada em pedidos. Uma linha não será dividida entre depósitos nesta versão.
- Manter uma linha por produto na mesma compra; rejeitar duplicidade em vez de consolidar automaticamente custos/quantidades.
- Exigir produto ativo e vínculo ProductSupplier ativo com o fornecedor ao incluir/substituir produto e ao enviar/confirmar a compra.
- unitCost explícito prevalece. Se omitido no cadastro ou na troca efetiva de produto, usar ProductSupplier.cost. Não criar vínculo de catálogo automaticamente nem usar Product.cost como fallback de um vínculo inexistente/inativo.
- Custos não negativos e representáveis em centavos; total = soma(quantity × unitCost), dentro de NUMERIC(12,2). Zero é permitido; sem frete, desconto, impostos ou arredondamento silencioso nesta etapa.
- unitCost salvo é um snapshot: alterações posteriores do catálogo não reprecificam compras existentes. Receber não altera automaticamente Product.cost nem ProductSupplier.cost.
- PATCH de item do mesmo produto com custo omitido preserva unitCost. Trocar só o depósito também preserva o custo.
- Para o primeiro MVP, trocar fornecedor exige compra DRAFT e sem itens. Não apagar/reprecificar itens como efeito colateral. Itens não serão transferidos entre compras pelo PATCH.
- Exigir StockItem previamente cadastrado para o produto no depósito escolhido, inclusive com saldo zero. Não criar estoque automaticamente no recebimento com parâmetros presumidos.
- expectedDeliveryDate é opcional e editável apenas em DRAFT. Permitir data passada para representar atrasos/cadastros históricos; não adicionar @Future sem necessidade. PATCH null preserva; removeExpectedDeliveryDate permite limpar e não pode vir junto com nova data.

### Estados

| Origem | Destinos permitidos | Condições |
|---|---|---|
| DRAFT | SENT, CANCELED | Enviar exige itens, fornecedor/responsável/produtos/vínculos ativos e dados válidos |
| SENT | CONFIRMED, CANCELED | Confirmar revalida a compra; não permite alterar os dados comerciais |
| CONFIRMED | PARTIALLY_RECEIVED, RECEIVED, CANCELED | Recebimentos definem os dois primeiros; cancelar somente sem recebimento |
| PARTIALLY_RECEIVED | PARTIALLY_RECEIVED, RECEIVED | Somente novos recebimentos podem avançar o atendimento |
| RECEIVED ou CANCELED | Nenhum novo estado | Histórico preservado; repetição do mesmo status sem alterações não tem efeito |

- SENT é um registro interno de envio, sem e-mail, WhatsApp ou integração externa.
- Somente DRAFT permite editar fornecedor, responsável, previsão e itens, incluindo excluir linhas. Após SENT, quantidade, custo, produto e destino ficam congelados.
- Não permitir atalhos DRAFT → CONFIRMED/RECEIVED nem retorno a DRAFT. O PATCH comum não atribui estados de recebimento.
- Excluir fisicamente somente compra DRAFT e sem itens. Cancelar preserva itens e total.
- Não permitir cancelar após qualquer recebimento, nem encerrar parcialmente o saldo pendente nesta versão. Devolução/estorno e encerramento parcial exigem fluxo posterior.
- Rejeitar cancelamento combinado com mudanças comerciais no mesmo PATCH. Enviar junto com alterações valida os dados finais e aplica tudo na mesma transação.

### Recebimento

- Permitir recebimento total ou parcial de itens selecionados em compra CONFIRMED/PARTIALLY_RECEIVED.
- Cada chamada informa quantidades adicionais recebidas, e não novos totais acumulados. Exemplo: comprado 10, já recebido 4, nova entrada 3 resulta em receivedQuantity 7.
- Não aceitar lista vazia, item repetido na requisição, item de outra compra, quantidade zero/negativa ou acima do saldo pendente da linha.
- O destino é o depósito já definido no item. O recebimento não muda custo, quantidade contratada, produto ou depósito.
- O operador do recebimento é obrigatório, existente/ativo e pode ser diferente do responsável pela compra. Sem autenticação, continua sendo um identificador informado pela API.
- Receber mercadoria de uma compra já confirmada não será impedido pela inativação posterior do fornecedor/produto/vínculo; preservar o compromisso e validar operador, estoque de destino, estados e quantidades.
- Cada linha recebida aumenta quantity do StockItem e gera INBOUND/PURCHASE com saldos anterior/posterior. Não altera reservedQuantity; exigir consistência com as reservas ACTIVE e respeitar Integer.MAX_VALUE.
- maxQuantity continua parâmetro de reposição, não uma proibição de entrada, conforme o comportamento atual das movimentações.
- Se qualquer linha falhar, reverter lote, entradas, contadores e status integralmente. O total contratado da compra não muda durante recebimentos.
- Após receber, derivar status de todos os itens: todos completos → RECEIVED; algum recebido e algum pendente → PARTIALLY_RECEIVED.

## Histórico e repetição de requisições

O schema atual tem apenas received_quantity acumulado e purchase_id no movimento. Isso não identifica quais linhas foram recebidas em cada entrega nem permite reconhecer uma repetição enquanto a compra ainda está parcial.

- Criar purchase_receipts: purchase_receipt_id, purchase_id, request_id UUID, operator_user_id, received_at e notes. Unicidade de purchase_id + request_id.
- Criar purchase_receipt_items: purchase_receipt_item_id, purchase_receipt_id, purchase_item_id, stock_item_id e quantity positiva. Unicidade de lote + item de compra.
- Histórico imutável, sem PATCH/DELETE e sem cascata de exclusão. Não inventar recebimentos retroativos para os movimentos antigos do seed.
- Adicionar purchase_receipt_item_id opcional e único a stock_movements. Quando presente, exigir INBOUND/PURCHASE, purchase_id informado e order_id/stock_reservation_id ausentes. Rejeitar reason nulo nesse vínculo.
- O cliente gera requestId para cada entrega e reutiliza o mesmo identificador em retries. Não reutilizar o identificador para entregas diferentes.
- Sob lock da compra, consultar requestId antes de executar: mesmo identificador e mesmo conteúdo devolvem o lote existente; conteúdo diferente é rejeitado. Comparar operador, notes normalizadas e pares item/quantidade ordenados, sem depender da ordem da lista.
- A repetição válida pode devolver o lote mesmo se a compra já estiver RECEIVED ou o operador tiver sido inativado depois. Não alterar os registros originais.
- requestId novo em compra totalmente recebida é rejeitado. A proteção não depende somente do status, pois várias entregas legítimas podem manter PARTIALLY_RECEIVED.

## Organização e contratos

Arquivos em backend/src/main/java/com/pi/erp:

- purchase/{Purchase,PurchaseStatus,PurchaseRepository,PurchaseService,PurchaseFilter,RequestPurchaseDTO,PatchPurchaseDTO,PurchaseController}.java.
- purchase/item/{PurchaseItem,PurchaseItemRepository,PurchaseItemService,PurchaseItemFilter,RequestPurchaseItemDTO,PatchPurchaseItemDTO,PurchaseItemController}.java.
- purchase/receipt/{PurchaseReceipt,PurchaseReceiptItem,PurchaseReceiptRepository,PurchaseReceiptItemRepository,PurchaseReceiptService,RequestPurchaseReceiptDTO,PurchaseReceiptController}.java.

### DTOs de entrada

- RequestPurchaseDTO: supplierId e userId obrigatórios; expectedDeliveryDate opcional. Não receber total, status ou purchaseSuggestionId.
- PatchPurchaseDTO: supplierId, userId, expectedDeliveryDate, Boolean removeExpectedDeliveryDate e PurchaseStatus status, todos opcionais, conforme as restrições de estado.
- RequestPurchaseItemDTO: purchaseId, productId, warehouseId e quantity obrigatórios; unitCost opcional.
- PatchPurchaseItemDTO: productId, warehouseId, quantity e unitCost opcionais. Não receber purchaseId nem receivedQuantity.
- RequestPurchaseReceiptDTO: requestId UUID e operatorUserId obrigatórios; notes opcional; lista não vazia com @Valid ItemDTO(purchaseItemId, quantity), ambos obrigatórios. Quantidades positivas e combinações são validadas no service.
- PurchaseFilter: supplierId, userId, status e intervalo de criação. PurchaseItemFilter: purchaseId, productId e warehouseId. Validar apenas coerência de intervalos quando informados.
- Mapear purchaseSuggestionId como Long opcional somente para leitura dos dados existentes; não implementar entidades ou fluxo de sugestões como dependência da compra manual.

### Endpoints

- CRUD /purchase e /purchaseItem no padrão customer: GET /search, GET /{id}, POST, PATCH /{id}, DELETE /{id}. Consultas/edição 200, cadastro 201, exclusão 204 e consulta por ID ausente 204.
- POST /purchase/{id}/receive retorna 201 com PurchaseReceipt criado; retry válido retorna 200 com o lote existente. O service deve fornecer ao controller a indicação de criação sem usar DTO de resposta de negócio: record interno ReceiptRegistration(PurchaseReceipt receipt, boolean created).
- GET /purchase/{id}/receipts lista os lotes em 200; compra ausente é ResourceNotFoundException.
- GET /purchase/{purchaseId}/receipts/{id} retorna o lote daquela compra, 204 quando ausente. Não retornar lote de outra compra por coincidência de ID.
- Não expor endpoint direto para cadastrar PURCHASE no controller de movimentações manuais.
- Responder entidades, com JsonIgnore nos vínculos de retorno necessários para evitar ciclos; manter os campos atuais de StockMovement.

## Transações e ordem dos locks

- Compra é a raiz: bloquear Purchase antes de itens, recebimentos ou saldos e fazer refresh antes de mutações.
- PurchaseItemService.update/delete deve primeiro consultar o purchaseId do item sem lock, bloquear a compra e só então bloquear/reler o item. Se ele não pertencer mais à compra consultada, abortar e pedir repetição, sem adquirir outro lock depois do item.
- Não copiar a ordem item → pedido usada atualmente em OrderItemService. Recebimento precisa atualizar receivedQuantity e entraria em deadlock com um editor que mantivesse lock de item enquanto aguardasse a compra.
- Recebimento bloqueia compra → itens por ID crescente → StockItems por ID crescente. Manter a mesma ordem em todos os caminhos.
- Resolver IDs dos estoques e revalidar produto/depósito depois de bloquear, pois o cadastro pode ter mudado desde a consulta inicial.
- Não fazer refresh de entidades já modificadas. Flush antes de agregados ou consultas que precisem refletir as mudanças locais.
- Movimentação manual, vendas e reservas já bloqueiam StockItem; a entrada de compra deve usar o mesmo lock sem adquirir compra depois de saldo.
- Lock da compra serializa requestId, saldo pendente e estados. Unicidades do lote e do vínculo no movimento oferecem proteção adicional.

## Tarefa 1: schema e entidades

- [x] Na V1, adicionar warehouse_id obrigatório em purchase_items e criar purchase_receipts/purchase_receipt_items depois de purchases/purchase_items e antes de stock_movements.
- [x] Adicionar o vínculo opcional único de movimento à linha de recebimento e suas constraints sem quebrar os checks de venda/reservas existentes.
- [x] Ajustar o insert de purchase_items na V2 para usar o warehouse_key já associado ao stock_key daquele cenário. Preservar saldos, contadores recebidos e movimentos históricos; novas tabelas de recebimento ficam vazias no seed.
- [x] Criar entidades com IDs, mapeamentos, enums e callbacks compatíveis com customer e o schema. receivedQuantity começa em zero; Purchase começa DRAFT com total zero.
- [x] Não aplicar schema nem recriar banco. Seguir a preferência por V1/V2 para banco recriado; migração preservando dados exigirá plano próprio se necessária.

## Tarefa 2: repositories e CRUD de compras

- [x] PurchaseRepository estende JpaRepository e JpaSpecificationExecutor; criar Optional<Purchase> findByIdForUpdate(Long id) com lock nativo apenas da linha.
- [x] PurchaseService implementa search/register/update/delete, depois auxiliares privados de busca, edição, ativos, datas e transições.
- [x] Implementar os DTOs/filtros/controller e status conforme o contrato; validar dados finais antes dos setters. Responsável obrigatório não pode ser removido.
- [x] Ao enviar/confirmar, exigir itens válidos e vínculos ativos; ao trocar fornecedor, exigir DRAFT vazio.
- [x] Excluir DRAFT vazio após verificar itens; cancelar somente sem recebimentos e sem alterações comerciais combinadas. Não modificar histórico.

## Tarefa 3: itens, custo e total

- [x] PurchaseItemRepository implementa filtros, findByPurchaseId, findPurchaseIdByItemId e lock da linha após o lock da compra; métodos existsByPurchaseIdAndProductId e ...AndIdNot para duplicidade.
- [x] Adicionar Optional<ProductSupplier> findBySupplierIdAndProductId(Long supplierId, Long productId) ao repository existente do catálogo, preservando seus métodos.
- [x] Implementar cadastro/PATCH/exclusão de itens somente em DRAFT, validando produto, vínculo do fornecedor, depósito e estoque cadastrado antes dos setters/save.
- [x] Resolver custo explícito ou do catálogo conforme as regras; validar quantidade, precisão, sinal e capacidade monetária.
- [x] Recalcular total com BigDecimal calculateTotalByPurchaseId(Long purchaseId), soma persistida de quantity × unit_cost e zero quando vazia; flush antes da soma. Item e total na mesma transação.
- [x] Não oferecer PATCH de receivedQuantity, transferência de item entre compras ou entrada manual de total.

## Tarefa 4: persistência e validação do recebimento

- [x] Criar repositories de cabeçalho/linhas com findByPurchaseIdAndRequestId, consultas de histórico por compra e vínculo único lote/item.
- [x] PurchaseReceiptService.register(Long id, RequestPurchaseReceiptDTO data) usa @Transactional, bloqueia/atualiza a compra e normaliza notes.
- [x] Rejeitar item repetido na requisição e comparar retry pelo conteúdo normalizado antes das validações próprias de uma nova entrega.
- [x] Para nova entrega, exigir estado CONFIRMED/PARTIALLY_RECEIVED e operador ativo. Bloquear/reler os itens e exigir que todos pertençam à compra; validar cada quantidade contra o pendente atual.
- [x] Resolver e bloquear todos os saldos ordenadamente, revalidando suas origens, capacidade de entrada em long e consistência das reservas. Validar todo o lote antes das mutações sempre que possível.
- [x] Persistir cabeçalho e linhas imutáveis, capturando operador e data do backend. Não alterar unitCost, quantity contratada ou depósitos.

## Tarefa 5: entrada comercial e atualização do atendimento

- [x] Criar StockMovement registerPurchase(PurchaseReceiptItem receiptItem) em StockMovementService com Propagation.MANDATORY; o coordenador já deve manter compra/itens/saldos bloqueados.
- [x] O método interno valida linha persistida e positiva, origem compatível, compra em estado de recebimento, operador ativo do cabeçalho, saldo/reservas, limite Integer e ausência de movimento prévio para aquela linha. Reutilizar a validação de operador já existente no service de movimentações.
- [x] Gerar INBOUND/PURCHASE com purchaseId, operador/notas do cabeçalho, quantidade da linha e saldos calculados. orderId, stockReservation e vínculos de venda ficam ausentes.
- [x] registerPurchase altera somente saldo físico e movimento, sem mudar reservedQuantity ou receivedQuantity. Somente PurchaseReceiptService incrementa o contador recebido da linha, uma vez após a entrada.
- [x] Depois de todas as linhas, fazer flush, derivar PARTIALLY_RECEIVED/RECEIVED dos itens completos e atualizar a compra. Registrar updatedAt também quando um novo lote mantiver PARTIALLY_RECEIVED.
- [x] Preservar bloqueios existentes de exclusão/troca de origem de StockItems com movimentos; entradas PURCHASE passam a compor esse mesmo histórico.
- [x] Não injetar PurchaseService em StockMovementService; manter dependências em um só sentido e preservar operações de vendas, reservas e movimentações manuais.

## Tarefa 6: endpoints e histórico

- [x] Implementar receive e consultas no PurchaseReceiptController, com @RequestBody @Valid, códigos HTTP e escopo por compra definidos acima.
- [x] Retornar o lote persistido com suas linhas, mantendo referência de volta ao cabeçalho ignorada na serialização e senha de User oculta.
- [x] Não criar PATCH/DELETE de recebimentos ou endpoints para mudar quantidades recebidas diretamente.

## Tarefa 7: revisão estática de aceitação

- [x] Conferir DRAFT vazio, custo explícito/zero/do catálogo, vínculo ausente/inativo, duplicidade, troca de fornecedor com itens e PATCH parcial preservando custo/destino.
- [x] Conferir fluxo DRAFT → SENT → CONFIRMED, congelamento dos dados, cancelamento antes de receber, rejeição de cancelamento parcial e exclusão somente de DRAFT vazio.
- [x] Conferir pedido de 10 unidades recebendo 4 e depois 6; o primeiro lote deixa PARTIALLY_RECEIVED e o segundo RECEIVED, com movimentos 4 e 6.
- [x] Conferir itens de depósitos distintos, item de outra compra, item repetido, excesso de quantidade, saldo Integer extremo e falha numa linha revertendo o lote inteiro.
- [x] Conferir retry idêntico, ordem diferente da lista com mesmo conteúdo, requestId reutilizado com conteúdo diferente e novo requestId após atendimento total.
- [x] Conferir recebimento concorrente com outro lote, edição/exclusão de item, venda, reserva e movimento manual, especialmente a ordem compra → itens → estoques.
- [x] Conferir seed histórico RECEIVED sem lotes novos, serialização sem ciclos e preservação de preços, quantidades reservadas e módulos existentes.
- [x] Comparar diretamente os arquivos com customer/customer.address e fazer revisão independente estática. Registrar que não houve testes, build, lint, banco nem comprovação de SQL/ORM/concorrência em runtime.

## Fora desta entrega

Sugestões automáticas, aprovação por alçada, envio externo ao fornecedor, frete/descontos/impostos, custo médio, atualização automática do catálogo, contas a pagar, documentos fiscais, devoluções, estornos e encerramento de saldo não entregue.

## Critério de conclusão

Compras manuais podem ser cadastradas, enviadas/confirmadas e recebidas parcial ou integralmente por item e depósito. Cada recebimento tem histórico próprio e entradas comerciais atômicas; retries não duplicam mercadoria. Totais e contadores são calculados pelo backend, e nenhum recebimento altera preços históricos ou reservas de vendas.

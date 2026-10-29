# Plano de implementação: regras de negócio de pedidos

> Execução: usar superpowers:executing-plans após revisão e aprovação das decisões deste documento. Esta etapa cria somente o plano. Executar primeiro a etapa 1; etapas 2 e 3 dependem de módulos e decisões adicionais, indicados abaixo.

**Objetivo:** controlar o ciclo de vida dos pedidos, proteger seus dados comerciais e preparar a integração com preços, usuários e estoque.

**Arquitetura:** OrderService coordena mudanças do pedido; OrderItemService coordena seus itens, mantendo o recálculo transacional já implementado. Repositories consultam e bloqueiam dados. As integrações futuras devem participar da mesma transação da operação comercial que as dispara.

**Tecnologias:** Java, Spring Boot, Spring Data JPA, PostgreSQL e BigDecimal.

**Especificação:** regras pendentes listadas na conversa, com as decisões propostas neste documento. As propostas abaixo ainda não são decisões comerciais confirmadas pelo usuário; a aprovação deve considerar explicitamente suas consequências.

## Restrições globais

- Seguir AGENTS.md e o padrão de customer e customer/address.
- Preservar as regras locais de total automático e desconto AMOUNT/PERCENTAGE, ainda sem commit.
- Não criar/executar testes automatizados, build ou lint sem solicitação explícita; revisar diretamente os trechos alterados.
- Não criar frontend, autenticação, módulos financeiros ou faturamento como efeito colateral.
- Não fazer commit, push ou recriar banco neste plano sem nova solicitação.
- Manter o contrato de PATCH: campo null ou omitido preserva o valor atual.
- Preservar URLs e respostas HTTP existentes. Novas rejeições de negócio usam IllegalArgumentException; recursos ausentes usam ResourceNotFoundException, com mensagens em inglês e ponto final.

## Situação de referência antes da implementação

- Total automático, desconto em valor/percentual, validação monetária e bloqueios transacionais já implementados localmente.
- O cadastro ainda recebe status e o PATCH aceita qualquer transição.
- Itens e dados do pedido podem ser modificados em qualquer status.
- A exclusão já rejeita pedidos com itens, mas não considera status.
- price_table_items existe no schema, sem entidade/repository correspondente.
- User e stock_movements ainda não possuem implementação Java.
- Estoque é separado por produto e depósito. OrderItem referencia apenas Product; não identifica de onde deve sair a mercadoria.

## Decisões propostas para aprovação

### Ciclo de vida

- Todo pedido novo começa em PENDING; retirar status de RequestOrderDTO e defini-lo no construtor de Order.
- Permitir PENDING → CONFIRMED e PENDING → CANCELED.
- Permitir CONFIRMED → CANCELED. Planejar CONFIRMED → INVOICED na etapa de integração; na etapa 1, rejeitar a tentativa de marcar INVOICED, pois ainda não há operação de faturamento integrada.
- CANCELED e INVOICED não retornam a outros status por PATCH comum. O cancelamento de venda já faturada dependerá de um fluxo próprio de devolução/estorno, fora desta etapa.
- Informar o mesmo status, sem outros campos, é uma operação sem efeito. Não repetir efeitos futuros de estoque.

### Edição e exclusão

- Somente PENDING permite alterar cliente, pagamento, responsável, produto, quantidade, preço e desconto; permite incluir, excluir ou transferir itens.
- Transferir um item exige origem e destino PENDING, mantendo bloqueios dos pedidos por ID crescente.
- CONFIRMED permite apenas a transição de status autorizada; seus dados e itens ficam congelados. Para corrigir dados comerciais, cancelar e criar outro pedido; não haverá reabertura nesta proposta.
- CANCELED e INVOICED ficam disponíveis para consulta e não aceitam alterações comerciais.
- Excluir fisicamente somente pedido PENDING e sem itens. Não adicionar cascata; excluir os itens individualmente enquanto o pedido ainda estiver PENDING.
- Cancelamento preserva itens, preços e total para consulta histórica. Não zera o pedido nem remove itens.
- Um PATCH com alteração de campos e confirmação valida o pedido final: as alterações são permitidas apenas se o status atual for PENDING. Aplicar tudo ou reverter tudo.
- Rejeitar PATCH que combine cancelamento com alterações comerciais; o cancelamento deve expressar apenas essa ação.

### Confirmação, cadastros ativos e produtos repetidos

- Exigir cliente ativo no cadastro, na substituição do cliente e na confirmação.
- Exigir pelo menos um item para confirmar, com todos os produtos ativos e valores válidos.
- Permitir total zero, por exemplo com desconto de 100%, pois as regras atuais já permitem esse caso. Não criar mínimo financeiro sem decisão comercial.
- Exigir produto ativo ao incluir item ou substituir produto. Um produto inativado depois da inclusão pode ser removido do pedido pendente; a confirmação deve rejeitá-lo enquanto permanecer inativo.
- Propor uma linha por produto em cada pedido. Rejeitar duplicidade com mensagem explícita; não somar quantidades automaticamente, pois linhas podem ter preços/descontos diferentes.
- Na etapa 1, manter unitPrice informado pelo cliente da API, com as validações existentes. A política automática de preço pertence à etapa 2.
- Não exigir pagamento para criar ou confirmar nesta etapa. A obrigatoriedade antes do faturamento será definida junto ao fluxo de faturamento.

## Etapa 1: ciclo de vida e integridade comercial

### Tarefa 1: status inicial e transições

Arquivos: backend/src/main/java/com/pi/erp/order/{RequestOrderDTO.java,Order.java,OrderService.java}.

- [x] Retirar status e seu @NotNull de RequestOrderDTO; manter @NotNull em customerId.
- [x] Definir status = OrderStatus.PENDING no construtor de cadastro, preservando total inicial zero.
- [x] Remover de register a validação que exige status recebido.
- [x] Criar auxiliares privados validateStatusTransition(OrderStatus currentStatus, OrderStatus status) e validateEditableOrder(Order order) em OrderService.
- [x] Validar transição antes dos setters, usando o status atual obtido sob bloqueio e refresh.
- [x] Aceitar status repetido sem efeitos, rejeitar saltos e transições reversas, e bloquear a entrada em INVOICED até a integração correspondente.
- [x] Em update, validar a combinação de campos comerciais e status conforme as regras propostas; não permitir que a ordem dos setters contorne os bloqueios.

### Tarefa 2: confirmação e cliente ativo

Arquivos: backend/src/main/java/com/pi/erp/order/OrderService.java e backend/src/main/java/com/pi/erp/order/item/OrderItemRepository.java.

- [x] Criar List<OrderItem> findByOrderId(Long orderId) em OrderItemRepository.
- [x] Criar validateConfirmation(Order order) em OrderService: verificar o cliente final ativo, existência de itens, produtos ativos, integridade dos valores e ausência de produtos repetidos.
- [x] Fazer flush dos dados pendentes antes de consultas de confirmação quando necessário, mantendo rollback integral se a validação falhar.
- [x] Ao cadastrar ou trocar cliente, rejeitar cliente inativo. Usar Boolean.TRUE.equals(customer.getActive()) para o Boolean da entidade.
- [x] Não alterar pedidos históricos quando cliente ou produto forem inativados; validar a situação lida no momento da confirmação.
- [x] Confirmar não movimenta nem garante estoque na etapa 1; essa limitação deve permanecer explícita até a etapa 3.

### Tarefa 3: bloquear alterações de itens e duplicidade

Arquivos: backend/src/main/java/com/pi/erp/order/item/{OrderItemRepository.java,OrderItemService.java}.

- [x] Criar existsByOrderIdAndProductId(Long orderId, Long productId) e existsByOrderIdAndProductIdAndIdNot(Long orderId, Long productId, Long id).
- [x] Criar auxiliar privado validateEditableOrder(Order order) em OrderItemService; verificar PENDING depois de bloquear e atualizar o pedido, antes de qualquer setter, save ou delete.
- [x] Em register, validar pedido pendente, produto ativo e duplicidade sob bloqueio do pedido.
- [x] Em update, bloquear origem e destino na ordem já usada; validar ambos pendentes e duplicidade com IDs finais, excluindo o próprio item.
- [x] Na troca de produto, validar o novo produto ativo; permitir excluir item com produto que se tornou inativo.
- [x] Em delete, verificar o pedido pendente antes da exclusão e preservar o recálculo.
- [x] Não adicionar dependência de OrderService em OrderItemService: manter pequenos auxiliares locais e evitar ciclo de services.
- [x] Não criar constraint única automaticamente: o banco pode conter duplicidades antigas. A verificação de service fica serializada pelo bloqueio de pedido; uma constraint futura exige tratar dados existentes e autorização própria para schema.

### Tarefa 4: exclusão e cancelamento

Arquivo: backend/src/main/java/com/pi/erp/order/OrderService.java.

- [x] Na exclusão, exigir PENDING antes do existsByOrderId e manter a rejeição de pedido com itens antes do DELETE.
- [x] No cancelamento permitido, preservar os dados e alterar apenas status; rejeitar campos comerciais combinados.
- [x] Preservar as consultas existentes para pedidos cancelados/faturados e não excluir registros relacionados.

### Tarefa 5: revisão estática de aceitação

- [x] Revisar todos os caminhos de escrita, incluindo chamadas diretas dos dois services, para impedir bypass das validações.
- [x] Conferir cadastro PENDING sem status no JSON; confirmação sem itens; cliente/produto inativo; pedido de total zero; status repetido; transição reversa e tentativa de faturar antecipadamente.
- [x] Conferir PATCH comercial + confirmação com cliente final; cancelamento + alteração comercial; edição de confirmado/cancelado/faturado; exclusão de PENDING vazio e rejeição dos demais.
- [x] Conferir criação/troca de produto duplicado, edição do próprio item e transferência para pedido que já contém o produto.
- [x] Conferir que confirmar e alterar itens disputam o mesmo bloqueio de pedido; preservar a ordem de locks item → pedidos e pedidos em ordem crescente. Não adicionar locks de itens na confirmação enquanto o pedido já estiver bloqueado.
- [x] Comparar apresentação e responsabilidades com customer; registrar que a verificação foi estática e que concorrência/SQL não foram executados.

## Etapa 2: preços e usuário responsável

Esta etapa exige planos próprios de implementação dos componentes ausentes, antes de executar suas integrações.

- [x] Criar a dependência mínima User/UserRepository conforme escolha posterior do usuário, sem CRUD. Mapear Order.user como associação opcional, preservando a possibilidade de pedidos automáticos.
- [ ] Enquanto não houver autenticação, tratar userId como usuário informado, não como identidade comprovada do operador. Após autenticação, obter o operador do contexto autenticado e definir autorização para alterar responsáveis.
- [ ] Validar usuário existente/ativo quando informado. Movimentações de estoque exigem um operador válido, pois stock_movements.user_id é obrigatório mesmo quando orders.user_id é opcional.
- [x] Implementar PriceTableItem em price/item, com associação à tabela e ao produto, respeitando a unicidade já existente no schema.
- [x] Ao incluir item sem unitPrice, usar preço da tabela ativa/vigente do cliente se houver linha para o produto; caso contrário, Product.price. Limites de datas inclusivos; datas nulas significam ausência daquele limite.
- [ ] Proposta: continuar permitindo preço explícito no pedido PENDING. Aprovação de desconto/preço por cargo só será implementada depois de definir permissões com User.
- [x] Preservar o preço salvo após inclusão. Trocar cliente não reprecifica itens automaticamente; trocar produto deve resolver o preço do novo produto quando não houver preço explícito na mesma requisição.
- [x] A omissão de unitPrice no PATCH comum do mesmo produto preserva o preço atual. Não usar mudança na tabela ou no cadastro de produto para reescrever vendas antigas.

## Etapa 3: estoque e faturamento

Dependências e decisões comerciais precisam ser aprovadas antes de um plano executável desta etapa; não implementar essas estruturas automaticamente ao executar a etapa 1.

- [x] Origem definida pelo usuário: um depósito por item. OrderItem.warehouse, warehouseId nos DTOs/filtro e coluna obrigatória na V1 implementados; a reserva vinculada ao pedido/item ainda está pendente.
- [ ] Definir e persistir reservas por pedido/item e StockItem. Apenas reserved_quantity agregado não permite liberar com segurança a reserva de um pedido específico.
- [ ] Criar stock_movements com o operador obrigatório, saldos anterior/novo e vínculos existentes; reservas devem ter rastreabilidade própria, pois os tipos atuais de movimento não incluem RESERVATION.
- [ ] Proposta: reservar ao confirmar, usando disponível = quantity − reservedQuantity, e rejeitar falta de disponibilidade integral. Não permitir saldo disponível negativo.
- [ ] Proposta: ao faturar, reduzir quantity e reservedQuantity pelas reservas do pedido e registrar OUTBOUND/SALE. Não executar baixa por simples edição livre do enum.
- [ ] Proposta: ao cancelar CONFIRMED, liberar apenas as reservas do próprio pedido, sem registrar uma entrada física fictícia. Cancelar PENDING não movimenta estoque.
- [ ] Executar status, reservas, saldos e movimentos na mesma transação; repetir a operação não deve duplicar reservas ou saídas. Definir locks dos saldos em ordem determinística e integrá-los à ordem de locks dos pedidos.
- [ ] Definir o evento real de faturamento e sua relação com documentos/financeiro. Só então habilitar CONFIRMED → INVOICED e decidir a obrigatoriedade de paymentMethod.
- [ ] Manter INVOICED sem cancelamento direto até existir fluxo aprovado de devolução/estorno, preservando histórico e referência aos movimentos originais.

## Critério de conclusão da etapa 1

Pedidos começam pendentes; transições e edições respeitam o estado; confirmação verifica cliente e itens; produtos repetidos são rejeitados; exclusão e cancelamento preservam integridade. Total e descontos continuam com as regras já implementadas. Integrações de preços, usuário e estoque permanecem explicitamente pendentes das etapas seguintes.

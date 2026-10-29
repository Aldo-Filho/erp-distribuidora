# Plano de implementação: total automático do pedido

> Para execução: usar superpowers:executing-plans, tarefa por tarefa, após aprovação deste plano. Não implementar nesta etapa.

**Objetivo:** manter `Order.totalAmount` igual à soma dos itens, com alteração dos itens e atualização do total na mesma transação.

**Arquitetura:** `OrderItemService` coordena a operação transacional e o recálculo. `OrderItemRepository` consulta a soma persistida; `OrderRepository` fornece bloqueio de escrita dos pedidos envolvidos. O total deixa de ser uma entrada da API.

**Tecnologias:** Java, Spring Data JPA, Spring Transactions, PostgreSQL e BigDecimal.

**Especificação:** cadastrar, editar e excluir itens deve recalcular o pedido atomicamente. Conforme solicitado, discountType permite selecionar desconto em valor ou porcentagem; os detalhes abaixo compõem a proposta de implementação.

## Restrições globais

- Seguir AGENTS.md e a organização de customer/customer.address.
- Não criar ou executar testes automatizados, build ou lint sem solicitação explícita.
- Não implementar estoque, faturamento, usuários ou transições de status neste escopo.
- Preservar alterações existentes. O usuário autorizou incorporar discount_type à V1, pois poderá recriar o banco; não manter a V3 aditiva.
- Preservar o PATCH: null ou omissão mantém o valor atual.

## Decisões propostas

- Criar enum DiscountType no package order/item com AMOUNT e PERCENTAGE, seguindo a convenção Java usada nos demais enums.
- `discount` guarda o valor informado; `discountType` define se representa reais ou porcentagem, sempre aplicado ao total da linha.
- Bruto da linha = quantity × unitPrice. AMOUNT: líquido = bruto − discount. PERCENTAGE: líquido = bruto − round(bruto × discount / 100, 2), usando HALF_UP no desconto calculado de cada linha. Total do pedido = soma dos líquidos já calculados em centavos.
- No cadastro, discount omitido começa em zero e discountType omitido usa AMOUNT. No PATCH, ambos preservam o valor atual quando nulos ou omitidos. Alterar apenas o tipo reinterpreta o desconto atual e exige validar a combinação final.
- Pedido sem itens tem total zero. Cadastro do pedido começa em zero.
- Quantidade deve ser positiva; preço e desconto devem ser não negativos. AMOUNT não pode ultrapassar o bruto da linha; PERCENTAGE deve estar entre 0 e 100, inclusive.
- Aceitar valores monetários representáveis em centavos e percentuais com até duas casas decimais, incluindo zeros decimais adicionais. Rejeitar precisão excedente na entrada, sem arredondamento silencioso; o arredondamento HALF_UP aplica-se apenas ao desconto monetário calculado do percentual. Validar o total contra a capacidade NUMERIC(12,2).
- Não modificar o preço histórico do item ao alterar o preço do produto.
- Preservar a possibilidade atual de mudar orderId no PATCH do item; recalcular origem e destino.
- Remover totalAmount de RequestOrderDTO e PatchOrderDTO. Trata-se de uma alteração explícita do contrato: clientes deixam de enviar esse campo. O campo permanece nas respostas da entidade.

## Pontos de revisão

- Exclusão do último item deve zerar o total.
- PATCH deve validar os valores finais, inclusive desconto existente após reduzir quantidade ou preço.
- Trocar discountType deve validar novamente o valor preservado; 150 em AMOUNT não pode ser reinterpretado como 150%.
- Mover um item deve atualizar os dois pedidos ou reverter toda a operação.
- Operações simultâneas não podem deixar total desatualizado nem sobrescrever dados do pedido.
- Falha na validação ou persistência deve reverter item e total juntos.

## Tarefa 1: tornar o total um dado calculado

Arquivos: backend/src/main/java/com/pi/erp/order/{RequestOrderDTO.java,PatchOrderDTO.java,Order.java,OrderService.java}.

- [x] Remover totalAmount dos records de entrada e o import BigDecimal quando ficar sem uso.
- [x] Ajustar o construtor Order(RequestOrderDTO data, Customer customer) para iniciar o total em BigDecimal.ZERO.
- [x] Remover de OrderService.update a atribuição de totalAmount recebido.
- [x] Revisar chamadas dos construtores e referências aos DTOs para manter compatibilidade interna.

## Tarefa 2: implementar soma e bloqueios

Arquivos: backend/src/main/java/com/pi/erp/order/OrderRepository.java e backend/src/main/java/com/pi/erp/order/item/OrderItemRepository.java.

- [x] Criar BigDecimal calculateTotalByOrderId(Long orderId) em OrderItemRepository, usando CASE por discount_type: AMOUNT subtrai discount; PERCENTAGE subtrai ROUND(quantity × unit_price × discount / 100, 2). Somar os líquidos das linhas e retornar zero quando não houver itens. Usar consulta SQL nativa PostgreSQL; para valores não negativos, ROUND numeric corresponde à política HALF_UP definida.
- [x] Criar Optional<Order> findByIdForUpdate(Long id) em OrderRepository com PESSIMISTIC_WRITE.
- [x] Criar Optional<OrderItem> findByIdForUpdate(Long id) em OrderItemRepository com PESSIMISTIC_WRITE para serializar alterações do mesmo item.
- [x] Usar consulta de bloqueio apenas da entidade desejada, evitando bloquear associações por joins implícitos.

## Tarefa 3: persistir o tipo de desconto

Arquivos: backend/src/main/java/com/pi/erp/order/item/{DiscountType.java,OrderItem.java,RequestOrderItemDTO.java,PatchOrderItemDTO.java} e backend/src/main/resources/db/migration/V1__create-table-erp.sql.

- [x] Criar DiscountType com AMOUNT e PERCENTAGE.
- [x] Incorporar à definição de order_items na V1 a coluna discount_type VARCHAR(20) NOT NULL DEFAULT 'AMOUNT' e CHECK limitado aos dois valores. Remover a V3, conforme autorização posterior do usuário para recriar o banco.
- [x] Usar AMOUNT por padrão nos inserts que omitem o tipo, incluindo o seed da V2.
- [x] Mapear discountType com @Enumerated(EnumType.STRING), coluna obrigatória e valor inicial AMOUNT em OrderItem.
- [x] Incluir DiscountType discountType em RequestOrderItemDTO e PatchOrderItemDTO, sem @NotNull, respeitando o padrão de cadastro e PATCH descrito acima.
- [x] Ajustar o construtor de OrderItem para aplicar AMOUNT quando o tipo estiver ausente no cadastro.

## Tarefa 4: coordenar alterações e recálculo

Arquivos: backend/src/main/java/com/pi/erp/order/item/OrderItemService.java e backend/src/main/java/com/pi/erp/order/OrderService.java.

- [x] Aplicar @Transactional a register, update e delete de OrderItemService.
- [x] No cadastro, bloquear o pedido antes de inserir o item.
- [x] Na edição e exclusão, buscar o item com bloqueio antes de ler os valores finais e o pedido de origem.
- [x] Bloquear os pedidos envolvidos por ID crescente quando houver origem e destino distintos; bloquear apenas uma vez quando forem iguais.
- [x] Garantir leitura atual dos pedidos após obter os bloqueios, inclusive quando já estiverem carregados no contexto JPA. Não persistir metadados antigos de uma associação carregada antes do bloqueio; usar refresh sob o bloqueio se necessário.
- [x] Combinar discountType e discount recebidos com os valores persistidos no PATCH. Validar quantidade, preço, tipo e desconto finais antes dos setters. Estender validateValues para aplicar limites por tipo e precisão das entradas.
- [x] Persistir a criação, edição ou exclusão e executar repository.flush() antes da consulta agregada, para a soma refletir o estado final da transação.
- [x] Criar private void recalculateTotal(Order order), consumindo calculateTotalByOrderId(order.getId()), validando capacidade monetária, atribuindo totalAmount e salvando o pedido.
- [x] Recalcular apenas o pedido afetado; em transferência, recalcular origem e destino dentro da mesma transação.
- [x] Tornar OrderService.update transacional e buscar o pedido com findByIdForUpdate antes de ler ou alterar seus campos. Isso evita que uma edição dos metadados sobrescreva um total calculado simultaneamente.
- [x] Usar o mesmo bloqueio em OrderService.delete. Preservar a restrição atual da FK para pedidos com itens; não introduzir cascata de exclusão.
- [x] Manter a dependência em um único sentido: OrderItemService usa os repositories; OrderService não depende de OrderItemService.

## Tarefa 5: revisão e cenários de aceitação

- [x] Revisar diretamente os arquivos alterados contra customer, verificando imports, annotations, nomes e localização das regras.
- [x] Conferir a fórmula: 3 unidades a 10,00 com desconto de linha de 5,00 resultam em 25,00; outra linha de 2 unidades a 7,50 resulta em pedido de 40,00.
- [x] Conferir PERCENTAGE: 3 unidades a 10,00 com desconto de 10% resultam em 27,00; 0,05 com desconto de 10% produz desconto arredondado de 0,01 e líquido de 0,04. Conferir 0%, 100%, rejeição acima de 100% e troca de tipo no PATCH.
- [x] Conferir os fluxos de cadastro, PATCH parcial, exclusão do último item, troca de produto e transferência entre pedidos.
- [x] Conferir rollback, ordem dos bloqueios, flush antes da soma e atualização de updatedAt pelo callback existente.
- [x] Registrar que os cenários foram revisados estaticamente; execução em banco e validação de concorrência só serão feitas se solicitadas. Não afirmar validação em runtime sem executá-la.

## Dados existentes

- A mudança mantém consistentes os pedidos afetados pelas operações novas; não corrige automaticamente todos os totais históricos.
- A definição atual da V1 usa AMOUNT como padrão. A V2 não precisa mudar, pois omite a nova coluna nos inserts.
- O usuário poderá recriar o banco para aplicar a V1 alterada. Este trabalho não recria o banco nem modifica uma instância existente.

## Critério de conclusão

Todas as escritas de itens pela aplicação atualizam os totais envolvidos na mesma transação, e nenhuma entrada de cadastro ou PATCH de pedido define totalAmount. O plano não cobre alterações diretas no banco ou outros futuros caminhos de escrita que não passem por esses services.

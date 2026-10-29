# Plano de implementação: preços e responsável do pedido

> Execução com superpowers:executing-plans. O usuário autorizou a próxima etapa e escolheu dependência mínima de User e integração com pedidos. Não criar CRUD de User, autenticação ou stock_movements.

**Objetivo:** validar o responsável opcional e resolver o preço omitido dos itens pela tabela do cliente ou pelo preço do produto.

**Arquitetura:** User/UserRepository representam usuários existentes; Order passa a referenciar User. PriceTableItem fica em price/item com CRUD no padrão customer/address. OrderItemService salva o preço resolvido em sua transação, preservando snapshots dos itens antigos.

**Tecnologias:** Java, Spring Data JPA, PostgreSQL, BigDecimal e LocalDate, sem novas dependências.

**Especificação:** etapa 2 de 2026-10-22-order-business-rules.md e escolha explícita do usuário por dependência mínima de User.

## Restrições

- Respeitar AGENTS.md; preservar todas as alterações locais e o plano de stock_movements de outra tarefa.
- Não criar/executar testes, build ou lint; revisar diretamente e com revisão estática independente.
- Não criar autenticação, permissões, endpoints de usuários, estoque, reservas ou faturamento.
- Não fazer commit/push, alterar migrations ou recriar banco.

## Tarefa 1: dependência de usuário e integração

- [x] Criar user/User.java e UserRepository.java conforme users; mapear role_id como Long enquanto Role não existir e ocultar passwordHash com JsonIgnore.
- [x] Mapear Order.user opcional, ajustar construtor/search/service e preservar userId nos DTOs e filtros.
- [x] Validar responsável existente e ativo quando informado e na confirmação se já houver responsável. Consultas históricas/cancelamento não exigem responsável ativo.
- [x] Preservar omissão no PATCH e permitir pedido automático sem usuário. Não implementar identificação autenticada do operador nesta etapa.

## Tarefa 2: PriceTableItem

- [x] Criar price/item/{PriceTableItem,PriceTableItemRepository,PriceTableItemService,RequestPriceTableItemDTO,PatchPriceTableItemDTO,PriceTableItemFilter,PriceTableItemController}.java.
- [x] Implementar CRUD em /priceTableItem com filtros por tabela/produto, unicidade tabela+produto e preço não negativo, em centavos e compatível com NUMERIC(12,2).
- [x] Serializar escritas pelo bloqueio da tabela; editar/excluir bloqueia primeiro o item. Transferências bloqueiam as tabelas por ID crescente.
- [x] PriceTableService.delete deve rejeitar tabela com itens antes do DELETE, sob bloqueio da tabela, sem cascata, evitando ciclo de locks na FK.
- [x] Permitir manutenção de tabelas/produtos inativos; só a resolução automática exige tabela ativa/vigente.

## Tarefa 3: resolução de preços

- [x] Criar Optional<BigDecimal> findApplicablePrice(Long customerId, Long productId, LocalDate date) em PriceTableItemRepository.
- [x] Tornar unitPrice opcional no cadastro: explícito prevalece; omitido busca tabela do cliente ativa/vigente com datas inclusivas; linha/tabela inaplicável usa Product.price. Preço zero é válido.
- [x] Passar o preço resolvido ao construtor de OrderItem e validar os valores finais antes dos setters/save.
- [x] No PATCH do mesmo produto, omissão preserva o preço, inclusive em transferência. Troca efetiva de produto sem preço explícito resolve o novo preço para o cliente de destino.
- [x] Alterar cliente ou preço de referência não reprecifica itens históricos. Não implementar autorização de alteração de preço por cargo.

## Tarefa 4: revisão estática

- [x] Conferir usuário nulo/inexistente/inativo, confirmação/cancelamento e ocultação da senha no JSON.
- [x] Conferir preço explícito, tabela inativa/futura/expirada, limites inclusivos, linha ausente, zero, fallback e PATCH parcial.
- [x] Conferir transferências, unicidade, locks, ausência de ciclos entre services e preservação das regras de total/desconto/status.
- [x] Registrar revisão independente e limitações: sem testes/build/lint, banco ou concorrência em runtime.

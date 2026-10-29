# Execução do plano: 2026-10-22-order-prices-and-user.md

- O usuário escolheu dependência mínima de User e integração com pedidos, sem CRUD de usuários.
- Preservar alterações locais anteriores, V1 e plano de stock_movements de outra tarefa. Não executar testes, build, lint, banco, commit ou push.
- Tarefa 1: User/UserRepository, Order.user, filtro, construtor e validações de responsável implementados. passwordHash oculto com JsonIgnore; roleId permanece escalar até Role existir.
- Decisão: responsável associado também deve estar ativo na confirmação, mas sua inativação posterior não bloqueia consulta/cancelamento.
- Tarefa 2: CRUD de PriceTableItem implementado com locks item → tabelas ordenadas, validação de preço/associações e unicidade. Exclusão de tabela com itens rejeitada explicitamente para preservar FK sem ciclo de locks.
- Tarefa 3: preço omitido usa tabela do cliente ativa/vigente ou Product.price; preço explícito prevalece. PATCH comum/transferência preserva preço, troca efetiva de produto resolve novo preço quando omitido.
- Não há mudança de schema nem endpoints de autenticação/usuários. A resposta de pedido passa de userId escalar a user associado, mantendo userId nos DTOs e filtros.
- Tarefa 4: revisão direta e revisão estática independente concluídas sem achados acionáveis. Conferidos usuário opcional/ativo, senha oculta, preço zero, preço explícito/automático, datas inclusivas, fallback, snapshots, PATCH, troca de produto, unicidade e locks.
- Não executados: testes, build, lint, banco, SQL nem concorrência/ORM em runtime. Alterações simultâneas de stock/movement e StockItemService foram preservadas e ficaram fora da revisão.

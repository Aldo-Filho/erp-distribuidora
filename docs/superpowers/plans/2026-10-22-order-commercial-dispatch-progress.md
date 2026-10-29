# Execução do plano: 2026-10-22-order-commercial-dispatch.md

- Execução autorizada pelo usuário. Compras será planejado depois; não implementar compras nesta entrega.
- Preservar o checkout e alterações locais anteriores. Sem testes, build, lint, commit/push, aplicação de migrations ou recriação de banco, conforme AGENTS.md/plano.
- Tarefas 1/2: reservas e vínculo único com movimento incluídos na V1; seed com reservado zero; entidades/repositories criados. Lock de item usa NO KEY UPDATE e estoque usa FOR UPDATE nativo sem joins.
- Tarefas 3/4: reserva/liberação ligadas às transições sob o lock do pedido. Escritas manuais em reservedQuantity e exclusão/troca de origem de saldos com reservas históricas rejeitadas.
- Decisão: prepareDispatch reutiliza no StockReservationService a validação de cobertura e o bloqueio ordenado dos saldos, também usados na liberação. Evita duplicar coordenação no service de saída.
- Tarefas 5/6: saída POST /order/{id}/dispatch, operador próprio, consumo de reserva e movimento OUTBOUND/SALE implementados em uma transação. Estado INVOICED representa saída comercial, sem documento fiscal.
- Decisão: movimentações manuais também validam o agregado das reservas antes de alterar saldo, preservando a nova invariante global. O POST manual continua rejeitando SALE/PURCHASE.
- Tarefa 7: revisão direta e revisão estática independente concluídas sem achados acionáveis concretos. Conferidos schema/seed, estados, cobertura, agregados, disponibilidade integral, release/consumo próprios, efeitos duplicados, locks/FKs, flush e dependências.
- Mantida a rejeição de INVOICED pelo PATCH; a mensagem orienta a usar a saída comercial. Repetir dispatch de pedido já INVOICED apenas retorna seu estado.
- Não executados: testes, build, lint, banco, migrations, binding HTTP ou validação de SQL/ORM/concorrência em runtime. Banco não recriado, sem commit/push. Compras não implementado; será planejado na próxima etapa.

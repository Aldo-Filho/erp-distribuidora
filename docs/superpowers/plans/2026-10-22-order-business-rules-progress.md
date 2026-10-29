# Execução do plano: 2026-10-22-order-business-rules.md

- Escopo autorizado: etapa 1. O usuário informou que User e stock_movements serão criados depois; etapas 2 e 3 permanecem pendentes.
- Preservar as alterações locais anteriores de total/desconto e a V1 modificada. Não fazer commit, push nem recriar banco.
- Conforme AGENTS.md, não criar/executar testes automatizados, build ou lint. A verificação será por revisão direta e revisão estática independente.
- Pré-revisão: confirmação e operações de itens devem usar o mesmo bloqueio de pedido; a confirmação não deve bloquear itens para evitar inverter a ordem dos locks.
- Tarefas 1 e 2: cadastro PENDING, transições, cliente ativo, confirmação e verificação dos itens implementados.
- Decisão: validateConfirmation recebe também o Customer final do PATCH; assim verifica troca de cliente + confirmação antes dos setters, em vez de mutar o pedido para validar.
- Tarefas 3 e 4: validação de pedido pendente centralizada no auxiliar findOrder de OrderItemService, sob lock/refresh; criação, edição, exclusão e ambos os pedidos de uma transferência passam por ele. Duplicidade rejeitada sob os mesmos locks.
- Decisão: transferir item para outro pedido também exige produto ativo, pois equivale a incluí-lo no destino. Editar quantidade/valor de uma linha antiga inativa continua permitido no pedido pendente; confirmar continua rejeitando esse produto.
- Cancelamento preserva itens/valores e não aceita mudanças comerciais na mesma requisição. Exclusão física exige pedido pendente e vazio.
- Confirmação não reserva, baixa nem garante estoque nesta etapa. INVOICED permanece bloqueado para novas transições até a integração.
- Tarefa 5: revisão direta e revisão independente estática concluídas sem achados acionáveis. Conferidos estados, dados finais do PATCH, cancelamento combinado, ativos, valores, duplicidade, transferências, exclusão e ordem dos locks.
- Não executados: testes, build, lint, SQL, banco, concorrência ou validação do ORM em runtime. As etapas 2 e 3 permanecem pendentes.

# Execução do plano: 2026-10-22-order-total.md

- Execução autorizada pelo usuário no checkout atual; preservar o módulo order ainda não versionado. Não criar commits ou mover arquivos existentes.
- Testes, build e lint não serão criados/executados, conforme AGENTS.md e plano aprovado. Verificação por revisão direta.
- Tarefas 1 a 3: DTOs de pedido sem total informado; enum, mapeamento e migration aditiva criados; repositories com soma e bloqueios adicionados.
- Decisão: usar SELECT FOR UPDATE nativo nos repositories para obter bloqueio de escrita apenas da linha principal, sem aplicar @Lock a consultas nativas e sem bloquear joins de associações eager.
- Revisão prévia: a soma depende de discount_type; a nova migration deve ser aplicada antes de utilizar a nova versão do módulo.
- Tarefa 4: operações de item transacionais, refresh sob bloqueio, pedidos bloqueados por ID crescente nas transferências, flush antes da soma e validação dos valores finais implementados.
- Revisão independente estática: cálculo, limites, PATCH, refresh, flush e transferências revisados; encontrado um ciclo de locks possível na exclusão de pedido com itens.
- Ajuste da revisão: OrderService.delete verifica existsByOrderId depois de bloquear o pedido e rejeita exclusão com itens antes da verificação FK. A restrição de exclusão foi preservada, com erro explícito de negócio e sem cascata; evita o ciclo de locks pedido → item versus item → pedido.
- Tarefa 5: revisão direta e revisão independente concluídas; sem execução de migration, banco, concorrência, testes, build ou lint. Essas verificações em runtime permanecem não realizadas.
- Autorização posterior: remover V3 e incorporar discount_type à V1, pois o usuário poderá recriar o banco. Documentação ajustada à decisão; banco não recriado.

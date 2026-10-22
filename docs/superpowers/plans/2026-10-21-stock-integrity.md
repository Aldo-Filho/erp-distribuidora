# Stock Integrity Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task after approval. Follow AGENTS.md. Do not create or run tests, build or lint. Implement directly in the existing project, preserve local changes and do not delegate without authorization.

**Goal:** Evitar perda de alterações simultâneas no estoque, proteger a identificação das movimentações históricas e permitir estoque sem quantidade máxima.

**Architecture:** Serializar a leitura, validação e gravação de cada item com bloqueio pessimista dentro da transação do service. Consultar a existência de movimentações no repository, sem criar um módulo de movimentações. Usar uma flag explícita de remoção de máximo no PATCH, seguindo o precedente removePriceTable de customer.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA e PostgreSQL existentes, sem novas dependências.

**Spec:** Revisão anterior de stock, AGENTS.md e decisão explícita do usuário: permitir cadastro sem máximo e remoção do máximo pelo PATCH. Plano aprovado pelo usuário e implementado diretamente no projeto, com revisão estática.

## Global Constraints

- Não criar nem executar testes, build ou lint; não aplicar migrations nem executar SQL no banco.
- Conferir o Git antes de implementar e preservar as alterações anteriores de todos os módulos.
- Ler os equivalentes de customer antes de alterar cada responsabilidade. Manter @Autowired, records simples, mensagens em inglês com ponto final e validações no service.
- Preservar URLs, retorno de entidades e códigos HTTP existentes. A ampliação do PATCH é a flag opcional removeMaxQuantity.
- Manter campos omitidos ou nulos do PATCH como preservação do valor atual, exceto pela intenção explícita de remoção.
- Não adicionar versão à entidade ou à resposta, não criar endpoints de transferência ou movimentação e não reescrever o frontend.
- Não alterar V1 ou V2: max_quantity já aceita NULL e o bloqueio proposto não exige coluna ou índice novo.
- Manter a unicidade de produto por armazém, as regras de quantidades não negativas, reservado menor ou igual ao saldo e mínimo menor ou igual ao máximo quando houver máximo.
- Não criar regras adicionais, como impedir saldo acima do máximo ou abaixo do mínimo.

## Decisões de comportamento

### Concorrência

- Usar uma consulta exclusiva para carregar o item com `LockModeType.PESSIMISTIC_WRITE` em update e delete.
- A transação de update começa antes dessa consulta e termina após a gravação. Calcular os valores finais somente depois de obter o bloqueio.
- Ao disputar o mesmo item, uma edição aguarda a transação anterior e passa a trabalhar com o estado atual em vez de reaplicar uma cópia antiga. Preservar o isolamento padrão existente; não introduzir nível global novo.
- Bloquear somente a linha do item envolvido. O cadastro de um item novo continua protegido contra duplicidade pela constraint uk_stock_item.
- Isto protege operações deste módulo que seguem o protocolo. Não detecta valores obsoletos enviados explicitamente por um formulário antigo nem controla gravações SQL externas que ignorem esse fluxo.
- Futuros services de movimentação devem obter o mesmo bloqueio antes de validar e gravar saldo ou histórico. A consulta de existência de histórico, isoladamente, não protege contra escritores futuros que ignorem esse protocolo.

### Histórico

- Com movimentações existentes, impedir a troca efetiva de produto ou armazém.
- Enviar os mesmos IDs atuais é permitido. Omitir IDs ou enviar null também preserva as associações.
- Sem movimentações, manter a possibilidade de trocar associações, respeitando a disponibilidade do par produto/armazém e a existência dos recursos.
- Mensagem proposta: `Cannot change the product or warehouse of a stock item with movements.`
- Manter a exclusão de registros com histórico impedida pela FK existente, usando o tratamento de conflito já disponível. Não apagar histórico em cascata.
- Alteração de saldo continua com o contrato atual. Criar automaticamente movimentações de ajuste exigiria regras de usuário, motivo e auditoria fora deste plano.

### Máximo opcional

| Entrada | Comportamento |
| --- | --- |
| Cadastro sem maxQuantity ou com null | Cadastrar sem máximo |
| Cadastro com máximo negativo | Rejeitar |
| Cadastro com máximo menor que o mínimo | Rejeitar |
| PATCH com máximo omitido ou null, sem remoção | Preservar o máximo atual |
| PATCH com maxQuantity preenchido, sem remoção | Validar e atualizar |
| PATCH com removeMaxQuantity=true e máximo omitido ou null | Remover o máximo |
| PATCH com removeMaxQuantity=true e máximo preenchido, inclusive zero | Rejeitar a combinação |
| Remover máximo de item que já está sem máximo | Permitir, mantendo null |
| PATCH que altera mínimo quando não há máximo | Validar mínimo não negativo, sem comparação com máximo |

- Quantidade, reserva e mínimo continuam obrigatórios no cadastro.
- Aplicar o mesmo cadastro sem máximo ao InitialStockDTO usado por product, para manter consistência entre os dois caminhos de criação de estoque.

## Arquivos e responsabilidades

1. `backend/src/main/java/com/pi/erp/stock/StockItemRepository.java`: leitura com bloqueio e consulta de existência de movimentações.
2. `backend/src/main/java/com/pi/erp/stock/StockItemService.java`: transação, proteção do histórico e cálculo final de máximo.
3. `backend/src/main/java/com/pi/erp/stock/RequestStockItemDTO.java`: retirar a obrigatoriedade do máximo no cadastro direto.
4. `backend/src/main/java/com/pi/erp/stock/PatchStockItemDTO.java`: acrescentar a flag de remoção ao final do record.
5. `backend/src/main/java/com/pi/erp/product/RequestProductDTO.java`: retirar apenas @NotNull de InitialStockDTO.maxQuantity, mantendo @PositiveOrZero e @Valid existentes.

Nenhuma classe nova de produção é necessária. ProductService já repassa maxQuantity ao service de estoque, sem precisar de alteração.

## Review Focus

1. Duas edições do mesmo item: leitura e cálculo dos valores finais devem ocorrer sob o bloqueio, dentro da mesma transação da gravação.
2. Item com histórico: rejeitar troca efetiva das associações antes dos setters, sem rejeitar IDs iguais aos atuais.
3. PATCH parcial: manter os campos omitidos e revalidar as combinações com o estado carregado após adquirir o bloqueio.
4. Remoção de máximo: distinguir manter, atualizar e remover, inclusive em combinações contraditórias e máximo zero.
5. Cadastro por product: aceitar máximo ausente sem enfraquecer os outros campos nem perder a transação de produto com estoque inicial.

## Task 1: Serializar as alterações do item

**Interfaces:** Produzir `Optional<StockItem> findByIdForUpdate(Long id)` no StockItemRepository. Consumir esse método em update e delete do service.

- [x] Adicionar @Lock(LockModeType.PESSIMISTIC_WRITE), @Query JPQL explícita por stockItem.id e @Param("id") ao método findByIdForUpdate. Usar imports explícitos e manter findById herdado disponível para leituras comuns.
- [x] Adicionar @Transactional ao update, preservando a anotação já existente em delete.
- [x] Substituir a leitura inicial desses dois métodos pela consulta com bloqueio, mantendo ResourceNotFoundException e sua mensagem atual.
- [x] Manter todas as validações, cálculos dos valores finais, setters e save dentro da transação de update, sem leitura prévia do mesmo item fora do bloqueio.
- [x] Preservar register e a transação externa de ProductService.register. Não atribuir ao bloqueio de linhas existentes proteção contra inserções duplicadas; essa função continua na constraint única.

## Task 2: Impedir alteração da identificação histórica

**Interfaces:** Produzir `boolean existsMovementsByStockItemId(Long id)` no StockItemRepository. Consumir esse método em update quando os IDs finais diferirem dos IDs persistidos.

- [x] Implementar a consulta nativa com SELECT EXISTS sobre stock_movements, filtrando stock_item_id por @Param("id"). Não criar entidade ou repository global de movimentações apenas para essa consulta.
- [x] Comparar warehouseId e productId finais com os IDs do item bloqueado, antes de resolver novas associações ou aplicar setters.
- [x] Se houver troca efetiva e movimentações, lançar IllegalArgumentException com a mensagem definida acima. Manter o HTTP 400 já adotado para regras do service.
- [x] Se não houver troca, dispensar a consulta de histórico e permitir edição normal das quantidades e limites.
- [x] Se não houver histórico, preservar as buscas de recursos e a checagem de duplicidade com exclusão do próprio ID antes de alterar a entidade.
- [x] Conferir que delete continua sem cascata para movimentações e usa o bloqueio da Task 1.

## Task 3: Permitir máximo ausente e remoção explícita

**Interfaces:** Acrescentar `boolean removeMaxQuantity` ao final de PatchStockItemDTO. Os demais campos e nomes permanecem iguais.

- [x] Retirar @NotNull de RequestStockItemDTO.maxQuantity, sem alterar os demais campos.
- [x] Retirar apenas @NotNull de RequestProductDTO.InitialStockDTO.maxQuantity, preservando @PositiveOrZero.
- [x] Adicionar removeMaxQuantity ao PATCH; omitido, seu valor deve permanecer false, como a flag equivalente de customer.
- [x] No início da validação de update, rejeitar removeMaxQuantity=true junto com maxQuantity não nulo: `Cannot provide a maximum quantity and request its removal at the same time.`
- [x] Calcular o máximo final com fluxo explícito: começar pelo persistido; se houver remoção, usar null; caso contrário, se houver novo máximo, usar esse valor.
- [x] Passar esse máximo final ao validateQuantities antes dos setters. Manter a validação de máximo negativo e a comparação com mínimo apenas quando o máximo não for null.
- [x] Preservar a aceitação de maxQuantity=null no service de cadastro e o repasse atual feito por ProductService.
- [x] Manter PatchStockItemDTO sem @Valid adicional no controller, pois as regras dessa alteração são tratadas no service e não adicionam constraints declarativas ao record.

## Task 4: Revisão estática e registro

- [x] Revisar diretamente os cinco arquivos de implementação e o diff, separando mudanças novas das anteriores.
- [x] Conferir por leitura o fluxo concorrente: uma edição de quantity seguida de edição de minQuantity não deve restaurar a quantidade anterior quando o segundo PATCH a omite.
- [x] Conferir que cada validação usa os valores finais e ocorre antes dos setters; mudança de associação rejeitada não deve alterar a entidade.
- [x] Percorrer textualmente todos os casos da tabela de máximo opcional e conferir os dois DTOs de cadastro.
- [x] Conferir por leitura a combinação de remoção de máximo e mudança de mínimo no mesmo PATCH, inclusive mínimo não negativo sem máximo.
- [x] Revisar a integração transacional com product e os métodos repository, incluindo nomes de parâmetros, retorno Optional e o nome real da tabela stock_movements.
- [x] Confirmar que os contratos e padrões de customer foram preservados, justificando @Lock e a consulta de histórico pela necessidade de stock.
- [x] Registrar os arquivos alterados e a ausência de testes, build, lint e execução de SQL. A revisão estática não comprova comportamento concorrente em runtime.

## Handoff

Plano implementado diretamente neste projeto. Escopo: cinco arquivos existentes, nenhuma migration nova ou alteração de schema. A regra de máximo opcional foi confirmada pelo usuário durante o planejamento; as duas correções de integridade seguem o feedback anterior.

## Registro da execução

- Task 1: concluída. findByIdForUpdate usa JPQL com PESSIMISTIC_WRITE; update e delete usam essa leitura sob @Transactional. Os valores finais são calculados depois da leitura com bloqueio.
- Task 2: concluída. existsMovementsByStockItemId consulta stock_movements por stock_item_id com SELECT EXISTS. A consulta é feita apenas em troca efetiva de associação, antes das buscas das novas associações e dos setters. IDs iguais aos atuais continuam permitidos.
- Task 3: concluída. maxQuantity é opcional nos dois caminhos de cadastro. removeMaxQuantity foi acrescentado ao PATCH; a combinação de remoção com valor informado é rejeitada antes dos setters, incluindo valor zero. Omissão ou null sem a flag preserva o valor persistido.
- Task 4: concluída por revisão direta do código e do diff. Foram conferidos parâmetros e imports, a transação dos chamadores, a preservação dos campos omitidos, a validação de mínimo/reserva e os nomes reais de tabela e coluna do histórico.
- Cinco arquivos existentes de produção foram alterados: StockItemRepository, StockItemService, RequestStockItemDTO, PatchStockItemDTO e RequestProductDTO. ProductService continua repassando o máximo e mantendo sua transação original.
- Nenhuma migration, entidade, controller ou arquivo de frontend foi alterado nesta execução. As alterações anteriores foram preservadas.
- Decisão de execução: seguir o plano aprovado no checkout atual, sem worktree ou commits; manter o registro neste documento, sem arquivos auxiliares de execução.
- Decisão de verificação: respeitar AGENTS.md e o plano aprovado; revisão pelo próprio implementador, sem delegação e sem criar ou executar testes, build, lint ou SQL.
- Limite: compilação, execução das consultas e concorrência não foram verificadas em runtime. Futuros escritores de movimentações devem adquirir o mesmo bloqueio antes de validar e gravar histórico/saldo, conforme a arquitetura do plano.

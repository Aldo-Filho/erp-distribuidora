# Plano de correção dos erros encontrados na auditoria do backend

> **Para execução:** usar `superpowers:executing-plans` para implementar as tarefas sequencialmente neste chat. Plano aprovado pelo usuário e executado neste chat. Evidências e limites em 2026-10-22-backend-audit-fixes-progress.md.

**Objetivo:** impedir perda de atualizações de usuário, restabelecer consultas e repetição de recebimentos e rejeitar quantidades fracionárias sem alterar dados parcialmente.

**Arquitetura:** aplicar bloqueio de linha na atualização de usuário, dentro de uma transação; completar o grafo de carregamento dos recebimentos mantendo respostas com entidades; desabilitar a conversão silenciosa de números decimais em inteiros na configuração Jackson. Não são necessárias novas dependências nem migrations.

**Tecnologias:** Java, Spring Boot, Spring Data JPA/Hibernate, PostgreSQL e Jackson já utilizados no projeto.

**Base:** solicitação do usuário para planejar os três reparos, `AGENTS.md`, código atual e `backend/target/audit-backend-2026-10-22.md`. Evidências HTTP/SQL nos arquivos `backend/target/audit-20261022-*-results.json` e `backend/target/audit-20261022-results.json`. Os arquivos em target são temporários: se não estiverem disponíveis, reconstruir os cenários documentados neste plano.

## Escopo e restrições

- Ordem: usuário → recebimentos → quantidades → regressão final.
- Preservar todas as alterações locais anteriores; conferir Git antes de executar e editar apenas o escopo abaixo.
- Antes de alterar, reler os equivalentes de customer/customer/address e os arquivos de destino. Manter nomes, organização, injeção por campos, mensagens em inglês e tratamento centralizado.
- Preservar URLs, entidades de resposta e semântica de campos omitidos/null no PATCH.
- Não ativar open-in-view, adicionar DTOs de resposta de negócio, mudar associações globalmente para EAGER, criar abstrações de validação ou alterar schema/migrations para estes reparos.
- Não incluir autenticação, autorização, Swagger, paginação, frontend ou correções genéricas de concorrência de outros cadastros. São escopos separados.
- Não fazer commit/push nem formatação global automaticamente.
- A elaboração do plano não executou testes, build ou lint; a execução aprovada incluiu os retestes abaixo.
- Após aprovação da implementação, incluir os retestes específicos e a regressão em banco isolado: a solicitação explícita de testes da auditoria permanece aplicável a estes três reparos. Reutilizar os scripts temporários, sem criar uma suíte permanente como efeito colateral.
- Não acessar o banco normal `erp`. Reaproveitar o método de banco/porta isolados da auditoria, mantendo logs próprios e encerrando os processos ao concluir. Não executar o runner antigo sem adaptar seus marcadores de encerramento e sua seleção de cenários.

## Decisões técnicas e alternativas

1. **Usuário:** preferir bloqueio pessimista da linha, já usado em pedidos/estoque, em vez de introduzir `@Version`. A versão também poderia impedir saves obsoletos, mas exigiria alteração de schema e definir um novo fluxo de conflito. Para este reparo, serializar as escritas mantém o contrato atual com menor mudança.
2. **Recebimentos:** usar grafo de carregamento completo nas consultas específicas. Não manter a sessão aberta durante a resposta nem modificar o fetch padrão de todas as entidades.
3. **Inteiros:** desabilitar a conversão decimal→inteiro no Jackson da aplicação, em vez de repetir desserializadores/annotations em cada DTO. Trata-se de uma mudança deliberada de entrada aceita: números JSON fracionários não poderão mais alimentar Integer/Long, inclusive IDs, mínimos e máximos. BigDecimal continua aceitando valores monetários decimais. A opção deve ser confirmada na versão Jackson 3/Spring Boot 4 efetivamente instalada, sem copiar configuração de versões anteriores às cegas.

## Foco da revisão

- Uma edição de nome não pode restaurar senha, perfil ou active lidos antes de uma atualização concorrente; exercitar na tarefa 1.
- Repetição de recebimento deve funcionar após compra concluída e após inativação do operador, quando UUID e conteúdo forem idênticos; exercitar na tarefa 2.
- Carregar items não basta: objetos internos, operador, compra e associações serializadas precisam estar disponíveis fora da transação; exercitar na tarefa 2 com vários itens.
- Campos omitidos/null e flags Boolean opcionais devem manter seu comportamento; exercitar na tarefa 3.
- Ajuste inteiro negativo continua permitido quando o domínio autoriza; moeda decimal continua válida; quantidade decimal não pode chegar ao service; exercitar na tarefa 3.

## Tarefa 1 — Proteger atualizações concorrentes de usuário

**Arquivos de produção:**

- Alterar `backend/src/main/java/com/pi/erp/user/UserRepository.java`.
- Alterar `backend/src/main/java/com/pi/erp/user/UserService.java`.
- Ler `backend/src/main/java/com/pi/erp/user/User.java` e o padrão `StockItemRepository.findByIdForUpdate`/`StockItemService.update`.

**Interface interna:** adicionar `Optional<User> findByIdForUpdate(Long id)` ao repository. Seguir o padrão existente de consulta nativa `SELECT * FROM users WHERE user_id = :id FOR UPDATE`, parâmetros nomeados e apresentação do repositório. O contrato público `User update(Long id, PatchUserDTO data)` continua igual.

- [x] Confirmar a reprodução existente: PATCH A troca senha e PATCH B altera nome com leitura concorrente; ambas as respostas podem ser 200 e o hash final volta ao original. Usar exclusivamente usuário de auditoria.
- [x] Adicionar a consulta de bloqueio ao repository.
- [x] Marcar `UserService.update` com `@Transactional` e buscar a entidade pelo método com bloqueio antes de combinar os campos finais do PATCH. Manter o bloqueio até validações, eventual geração de hash, setters, save e commit. O hash preservado deve vir do estado atualizado, não de uma leitura anterior ao bloqueio.
- [x] Preservar tratamento de duplicidade de email, papel inexistente, active=false, senha vazia, omissão/null e ocultação do hash. Não alterar o algoritmo Argon2 ou o cadastro de usuário.
- [x] Compilar e repetir o cenário coordenado de senha versus nome. Aceite: ambas as mudanças persistem; hash final diferente do original e compatível com a nova senha. Nenhuma resposta expõe senha/hash.
- [x] Repetir PATCHs simultâneos de nome versus active e nome versus roleId. Aceite: campos independentes são preservados. Para duas alterações do mesmo campo, prevalece a ordem de aquisição do bloqueio; não prometer mesclar valores contraditórios.
- [x] Conferir atualização de ID ausente (404), senha vazia (400), PATCH vazio/null e alteração sequencial válida.

**Resultado independente:** usuário deixa de perder alterações em PATCHs concorrentes que usam o service corrigido. Escritores externos e outros CRUDs não são cobertos por essa garantia.

## Tarefa 2 — Corrigir respostas de recebimentos

**Arquivos de produção:**

- Alterar `backend/src/main/java/com/pi/erp/purchase/receipt/PurchaseReceiptRepository.java`.
- Ler `PurchaseReceiptService.java`, `PurchaseReceipt.java`, `PurchaseReceiptItem.java` e as associações de PurchaseItem/StockItem. Alterar o service apenas se o carregamento necessário não ficar integralmente resolvido pelas consultas.

**Interfaces preservadas:** `findByPurchaseIdAndRequestId`, `findByPurchaseIdOrderByIdAsc` e `findByIdAndPurchaseId`; POST receive e GET receipts mantêm URLs, conteúdo e tipos de resposta.

- [x] Reproduzir consulta e retry sobre um lote já persistido, não somente sobre o objeto recém-criado. Confirmar no log a cadeia `PurchaseReceipt.items → PurchaseReceiptItem.purchaseItem → PurchaseItem.product` sem sessão.
- [x] Atualizar os três grafos para carregar `items.purchaseItem` e `items.stockItem`, usando `EntityGraphType.LOAD` para preservar também as associações EAGER dos mapeamentos existentes. Conferir que compra, operador e relacionamentos internos usados pelo JSON estão inicializados. Não assumir que declarar apenas items resolve a cadeia.
- [x] Manter `open-in-view=false`, transações e regras de idempotência atuais. Não alterar UUID, comparação de payload, contadores, saldos, status ou geração de movimentos.
- [x] Compilar e testar GET `/purchase/{id}/receipts` e GET `/purchase/{purchaseId}/receipts/{id}` após recebimento de vários itens e depósitos. Aceite: 200 com JSON completo, sem erro de proxy ou recursão e sem hash de usuário.
- [x] Testar POST inicial com UUID novo: 201. Repetir UUID/conteúdo: 200, mesmo lote e nenhum incremento adicional de saldo, contador ou movimento.
- [x] Repetir após conclusão da compra e após inativar o operador que fez aquele recebimento. Aceite: retry idêntico continua 200; UUID reutilizado com conteúdo diferente continua 400.
- [x] Repetir duas requisições simultâneas idênticas. Aceite: uma 201 e outra 200, um lote e uma entrada por item. Conferir diretamente no banco, além das respostas.
- [x] Conferir lote consultado por compra errada (204), recurso ausente conforme o contrato atual e rejeição de lote inválido sem efeitos parciais.

**Resultado independente:** consulta e repetição passam a devolver respostas válidas, preservando a proteção existente contra duplicação.

## Tarefa 3 — Rejeitar números fracionários para campos inteiros

**Arquivos de produção:**

- Alterar `backend/src/main/resources/application.yaml` na configuração Jackson existente, usando a opção suportada para desativar `ACCEPT_FLOAT_AS_INT`.
- Ler os DTOs Request/Patch de order/item, purchase/item, stock, o RequestStockMovementDTO e os records aninhados RequestPurchaseReceiptDTO.ItemDTO e RequestProductDTO.InitialStockDTO.
- Preservar `GlobalExceptionHandler.handleUnreadableMessage`, que já retorna 400 padronizado. Não adicionar constraints que recebam o valor depois de ele já ter sido truncado.

**Contrato numérico:** Integer/Long aceitam números JSON inteiros dentro da faixa do tipo. Rejeitar tokens decimais, incluindo 1.5, 1.0 e 1e0, em campos inteiros. Continuar permitindo BigDecimal nos campos monetários e manter regras de sinal/faixa nos services. Esta configuração não tem o objetivo de alterar a coerção de strings nem o tratamento de null.

- [x] Confirmar nos metadados/dependências locais a propriedade de configuração para Jackson 3 utilizada pelo Spring Boot atual. Aplicar somente a opção decimal→inteiro; não substituir o ObjectMapper completo nem desabilitar validações de null.
- [x] Repetir os quatro casos confirmados: POST `/orderItem` com 1.5; POST `/purchaseItem` com 2.5; receive com quantity 1.5; POST `/stockMovement` com 1.5. Aceite: 400 padronizado, nenhum registro/saldo/contador alterado.
- [x] Cobrir PATCH de quantidade em orderItem/purchaseItem, cadastro/PATCH de stockItem e estoque inicial aninhado em product. Aceite: nenhuma fração é truncada, inclusive mínimos, máximos e reservas.
- [x] Cobrir 1.0, 1e0, números fora da faixa do tipo e ID numérico fracionário em corpo JSON. Aceite: 400, sem seleção acidental de outro recurso. Não confundir com parâmetros de URL, cuja conversão segue o Spring MVC.
- [x] Validar inteiros positivos legítimos e o ajuste manual ADJUSTMENT com inteiro negativo permitido. Manter rejeição de zero/negativo onde a regra já os proíbe.
- [x] Validar unitPrice/unitCost/price com decimais válidos, respeitando as regras existentes de centavos. Aceite: comportamento monetário preservado.
- [x] Validar PATCH vazio, null, flags Boolean omitidas/false e quantidade obrigatória ausente. Aceite: regras existentes preservadas; não voltar ao defeito das flags opcionais primitivas.

**Resultado independente:** payload inválido é rejeitado antes de qualquer regra de escrita, sem arredondamento/truncamento silencioso.

## Tarefa 4 — Verificação integrada e entrega

**Evidências temporárias:** reutilizar/adaptar `backend/target/audit-20261022.cjs`, `audit-20261022-focused.cjs`, `audit-20261022-core.cjs`, `audit-20261022-associations.cjs` e o runner isolado. Gerar novos nomes de logs/resultados para preservar o histórico da auditoria.

- [x] Executar compilação, contextLoads e as verificações específicas das três tarefas no banco isolado.
- [x] Repetir a bateria dos módulos recentes com as expectativas corrigidas: consultas/retries 200, quantidades fracionárias 400 e campos concorrentes preservados.
- [x] Separar sondagens de Swagger da avaliação dos três reparos: seus 404 já conhecidos continuam fora deste escopo e não devem ser contabilizados como falha de uma correção entregue.
- [x] Executar uma rodada da regressão de 189 cenários dos cadastros/endereços e dos 18 cenários de vínculos/índices, já ajustados aos contratos atuais. Corrigir apenas regressões introduzidas por estes reparos.
- [x] Consultar invariantes de saldo físico/reservado, soma de reservas ACTIVE, receivedQuantity e unicidade dos movimentos de recebimento. Aceite: nenhuma inconsistência ou duplicação.
- [x] Comparar diretamente os arquivos alterados com as referências customer e com o Git inicial, conferindo imports, transações, consultas, organização e preservação das mudanças anteriores. Não aplicar formatação global.
- [x] Encerrar os processos de auditoria e entregar feedback com alterações, resultados reais e limitações. Não afirmar que testes não executados passaram.

## Critério de conclusão

Os três cenários originais deixam de falhar; seus casos de borda acima passam; a regressão não aponta defeito introduzido; não há alteração no banco normal, migrations ou frontend. A auditoria não pretende provar correção para todos os escritores externos, cenários de carga ou interleavings possíveis.

## Execução recomendada

Implementação sequencial neste chat, com revisão e retestes por etapa. As três mudanças são pequenas e já têm causas e reproduções conhecidas; não é necessária delegação para executá-las. Aprovação recebida e execução concluída; ver o registro de progresso.


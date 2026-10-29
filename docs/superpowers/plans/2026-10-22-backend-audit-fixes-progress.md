# Execução — plano: 2026-10-22-backend-audit-fixes.md

Plano aprovado pelo usuário neste chat. Execução sequencial com `superpowers:executing-plans`.

- Ruling: executar no checkout atual — o plano aprovado preserva os módulos ainda não commitados; uma worktree do HEAD não conteria essa base. Sem commit/push ou alteração das mudanças anteriores.
- Ruling: usar os scripts temporários e o relatório da auditoria como reproduções RED — as falhas foram observadas neste mesmo chat antes das correções; não criar suíte permanente nem repetir a bateria antiga desnecessariamente.
- Ruling: manter este registro em docs junto ao plano — segue os registros existentes do projeto e permite continuidade sem criar scripts adicionais de gerenciamento de worktree.
- Pre-flight: tarefas 1, 2 e 3 não compartilham interfaces novas. A tarefa 4 verifica o efeito combinado, incluindo a configuração Jackson nos mesmos endpoints.

## Implementação

- Tarefa 1: concluída. Consulta users FOR UPDATE e transação envolvendo toda a atualização. Cenário coordenado preservou senha nova e nome; o hash corresponde à senha nova (verificação Argon2 real). Edições independentes de active e role também preservadas.
- Tarefa 2: concluída. Grafos LOAD com items.purchaseItem e items.stockItem nas três consultas. Lista, detalhe, retry após conclusão, operador posteriormente inativo e retry simultâneo passaram, sem duplicação.
- Tarefa 3: concluída. Desativado accept-float-as-int no YAML. Propriedade confirmada nos metadados locais de spring-boot-jackson 4.1.0, com enum tools.jackson.databind.DeserializationFeature. Tokens 1.5, 1.0 e 1e0 rejeitados em Integer/Long; BigDecimal, nulls e flags opcionais preservados.
- Tarefa 4: concluída. Compilação/contextLoads, 541 verificações HTTP/SQL/estado e revisão final aprovadas. Sem mudança de migrations, frontend, dependências ou banco normal.

## Evidências anteriores às correções

- audit-20261022-results.json: 12 divergências reproduzindo os três defeitos.
- audit-20261022-focused-results.json: troca de senha e alteração de nome retornaram 200, mas o hash original foi restaurado.
- audit-20261022-app.log: falha ao serializar PurchaseReceipt.items.purchaseItem.product com proxy sem sessão.

Snapshots dos quatro arquivos de produção antes desta execução em backend/target/audit-fixes-before. Nenhum dado real do ERP utilizado.

## Verificação após as correções

Compilação Maven offline com Java 25 e target Java 21: BUILD SUCCESS. `ErpApplicationTests.contextLoads`: 1 executado, 0 falhas/erros. Flyway V1/V2 aplicadas em banco novo `backend_verify_1792715943529`, PostgreSQL isolado na porta 55432; aplicação na porta 18080.

| Bateria | Total | Aprovadas | Falhas |
| --- | ---: | ---: | ---: |
| Módulos recentes | 253 | 253 | 0 |
| Casos de borda adicionais | 76 | 76 | 0 |
| Concorrência de senha, hash e invariantes | 5 | 5 | 0 |
| Cadastros e endereços | 189 | 189 | 0 |
| Vínculos de fornecedores e índices | 18 | 18 | 0 |
| Total HTTP/SQL/estado | 541 | 541 | 0 |

Os totais incluem fixtures e asserções, não são 541 testes JUnit. Os cenários de concorrência são pontuais, não prova geral para todos os interleavings/escritores externos.

- Evidências: backend/target/fixes-20261022-maven.log; fixes-20261022-results.json; fixes-20261022-extra-results.json; fixes-20261022-focused-results.json; fixes-20261022-core-results.json; fixes-20261022-associations-results.json.
- Os probes de Swagger foram retirados da avaliação destes reparos porque suas rotas ausentes estão fora do escopo aprovado.
- Os casos de coerção verificaram retorno 400 pelo handler de desserialização, não apenas rejeição posterior por regra de domínio. Snapshot SQL antes/depois confirmou ausência de efeitos dos payloads inválidos.
- Log sem falhas inesperadas de serialização. Avisos SQL de exclusão protegida por FK são esperados nos cenários de conflito e retornaram o status esperado.
- Revisão final independente, somente leitura, sem achados acionáveis. Comparação dos quatro arquivos com snapshots mostrou apenas as alterações previstas. Annotations, imports e organização conferidos contra customer e padrões de bloqueio de estoque.
- Custos conhecidos: duas atualizações do mesmo usuário aguardam o bloqueio, inclusive durante geração Argon2; consultas de recebimentos carregam as associações necessárias ao JSON.
- Nenhum teste permanente criado. Scripts, snapshots e logs temporários continuam em target, ignorado pelo Git.
- Sem commit/push. Processos isolados encerrados ao final; banco de auditoria preservado para investigação.

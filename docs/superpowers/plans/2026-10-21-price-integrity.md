# Price Table Integrity Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task after approval. Follow AGENTS.md. Preserve local changes and implement directly in the project. Do not create or run tests, build or lint. Do not delegate without authorization.

**Goal:** Proteger a unicidade de nomes de tabelas de preço e impedir que edições simultâneas restaurem valores antigos de campos omitidos no PATCH.

**Architecture:** Manter a validação antecipada no service e adicionar um índice único com comparação equivalente no PostgreSQL. Adotar leitura com bloqueio pessimista dentro da transação de edição, seguindo a solução existente em stock, sem acrescentar versão aos contratos públicos.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL e Flyway existentes, sem novas dependências.

**Spec:** AGENTS.md e revisão do módulo price após a padronização solicitada. Plano aprovado pelo usuário e implementado diretamente no projeto, com revisão estática.

## Padronização já realizada

- PriceTable respeita active=false no cadastro e mantém true quando active é omitido ou null, seguindo customer.
- PATCH com nome vazio ou somente espaços é rejeitado com IllegalArgumentException, em vez de ser ignorado.
- Período e duplicidade são validados antes dos setters.
- Assinaturas, variáveis locais e chamadas de Specification foram alinhadas à apresentação de customer.
- Quatro arquivos existentes foram ajustados: PriceTable, PriceTableController, PriceTableRepository e PriceTableService. DTOs e filtro já seguem o padrão e foram preservados.
- Não foram criadas classes novas nem alterados schema, frontend ou URLs durante a padronização.

## Feedback que motiva este plano

1. **[P2] Unicidade de nomes sem proteção final no banco.** O service verifica existsByNameIgnoreCase, mas price_tables não possui constraint ou índice único para o nome. Dois cadastros ou renomeações concorrentes podem passar pela verificação. A comparação atual também distingue espaços nas extremidades.
2. **[P2] Edições concorrentes podem perder alterações.** update lê e salva o registro sem bloqueio ou versão. Uma requisição que altera somente o nome pode restaurar datas ou active de uma cópia anterior, desfazendo outra edição. Esse risco também exige validar o período com o estado atualizado.

## Global Constraints

- Conferir o Git e preservar todas as alterações anteriores, incluindo a padronização registrada acima.
- Manter os padrões de customer: @Autowired nos campos, records simples, responsabilidades no service, repository para consultas e mensagens em inglês com ponto final.
- Não criar nem executar testes automatizados, build, lint, formatação global ou comandos SQL.
- Preservar URLs /price-table, códigos HTTP, retorno de entidades e o contrato de findOne ausente retornar 204.
- Preservar active=false, obrigatoriedade do nome, datas opcionais e validação startDate <= endDate quando ambas estiverem presentes.
- Não tornar datas obrigatórias, impedir datas passadas ou exigir correspondência entre active e a data atual. Essas regras não existem no módulo.
- Campo omitido ou null no PATCH continua preservando o valor atual. Não acrescentar flags de remoção de datas neste plano.
- Manter as buscas por startDate e endDate como igualdade, sem reinterpretar os filtros como busca de vigência.
- Editar somente a V1 existente para o índice, conforme a decisão anterior do usuário de recriar o banco. Não criar outra migration, aplicar migrations ou modificar a V2.
- Preservar índices anteriores de supplier, product e warehouse. Não alterar regras de associação de customer ou implementar price_table_items.

## Regras propostas

- Nomes serão únicos usando lower(btrim(name)), ignorando maiúsculas e espaços comuns nas extremidades. `Atacado`, `ATACADO` e ` Atacado ` representarão o mesmo nome.
- Preservar a grafia armazenada, acentos, pontuação e espaços internos. A normalização serve somente à comparação.
- Cadastro e edição terão consultas compatíveis com a expressão do índice; edição exclui o próprio price_table_id.
- Duplicidade antecipada mantém IllegalArgumentException e a mensagem atual. Colisões na constraint usam o tratamento já existente de DataIntegrityViolationException com HTTP 409.
- Edição e exclusão obterão bloqueio da linha antes das validações e manterão a transação até concluir a escrita.
- O bloqueio protege os fluxos que o adotam; não identifica dados obsoletos enviados explicitamente por formulários antigos nem coordena SQL externo que ignore o protocolo.
- Exclusão continuará sujeita às FKs de customers e price_table_items. Não desvincular clientes nem apagar itens em cascata automaticamente.

## Arquivos previstos

1. `backend/src/main/java/com/pi/erp/price/PriceTableRepository.java`: consultas de nome normalizado e leitura com bloqueio.
2. `backend/src/main/java/com/pi/erp/price/PriceTableService.java`: usar as consultas e delimitar a transação de update.
3. `backend/src/main/resources/db/migration/V1__create-table-erp.sql`: índice único do nome de price_tables.

Não criar classes, endpoints ou campos de DTO para essas correções.

## Review Focus

1. Cadastro e renomeação simultâneos para o mesmo nome devem ter proteção final no banco.
2. Renomear sem alterar o nome, inclusive variando caixa ou espaços nas extremidades, deve excluir o próprio ID da comparação.
3. PATCH parcial deve combinar a entrada com o estado obtido após o bloqueio, mantendo alterações anteriores em campos omitidos.
4. Se a primeira edição alterar uma data, a segunda precisa validar o período contra essa data atualizada antes dos setters.
5. A V2 e os índices de outros módulos devem permanecer compatíveis e intactos; leitura de SQL não equivale à sua execução.

## Task 1: Garantir unicidade consistente do nome

**Interfaces:** Substituir as consultas derivadas por `boolean existsByNormalizedName(String name)` e `boolean existsByNormalizedNameAndIdNot(String name, Long id)` em PriceTableRepository.

- [x] Ler customer/CustomerRepository e o precedente de BrandRepository antes de alterar as consultas.
- [x] Usar @Query nativa com SELECT EXISTS, @Param explícito e comparação `lower(btrim(name)) = lower(btrim(cast(:name AS text)))`.
- [x] Na consulta de edição, acrescentar `price_table_id <> :id`.
- [x] Atualizar register para a nova consulta, mantendo sua mensagem e a validação de período.
- [x] Em update, validar nome enviado não vazio, calcular o nome final com entrada ou valor persistido e verificar duplicidade com exclusão do próprio ID antes dos setters.
- [x] Preservar o texto original ao salvar; não aplicar trim/lower ao valor armazenado.
- [x] Após CREATE TABLE price_tables, adicionar:

```sql
CREATE UNIQUE INDEX uk_price_table_name_normalized
    ON price_tables (lower(btrim(name)));
```

- [x] Inspecionar textualmente os cenários da V2: os nomes são gerados como Condição Comercial + cidade. Conferir cidades distintas e não presumir execução bem-sucedida.

## Task 2: Serializar a edição e exclusão de cada tabela

**Interfaces:** Adicionar `Optional<PriceTable> findByIdForUpdate(Long id)` ao repository; manter findById herdado para leituras comuns.

- [x] Usar @Lock(LockModeType.PESSIMISTIC_WRITE), @Query JPQL por priceTable.id e @Param("id"), com imports explícitos.
- [x] Adicionar @Transactional em update e manter a anotação atual em delete.
- [x] Trocar a leitura inicial de update e delete por findByIdForUpdate, preservando ResourceNotFoundException e a mensagem atual.
- [x] Calcular nome e datas finais somente após a leitura com bloqueio. Manter todas as validações e setters dentro da mesma transação da gravação.
- [x] Preservar o isolamento padrão existente. Não aplicar bloqueio à busca pública ou criar uma transação global para consultas.
- [x] Manter register sem bloqueio de linha inexistente; a proteção final do nome continua sendo o índice da Task 1.

## Task 3: Revisão direta das correções

- [x] Revisar os três arquivos previstos e seus equivalentes de referência, preservando as alterações já realizadas nesta tarefa.
- [x] Conferir correspondência entre índice, parâmetros e consultas normalizadas de cadastro e edição.
- [x] Percorrer por leitura: novo nome, nome próprio, nome de outra tabela, variações de caixa e espaços nas extremidades.
- [x] Percorrer por leitura: uma edição de active seguida de PATCH de nome, e uma edição de datas seguida de PATCH parcial. Campos omitidos devem usar o estado atualizado.
- [x] Conferir datas opcionais, datas iguais, início após fim e edição de apenas uma data. Rejeitar período inválido antes dos setters.
- [x] Confirmar que a FK continua protegendo os vínculos na exclusão e que o tratamento existente de conflito não foi substituído.
- [x] Revisar V1 e V2 textualmente, sem executar migrations ou modificar os dados da carga.
- [x] Atualizar este documento com os arquivos efetivamente alterados e os limites da verificação: sem testes, build, lint, SQL ou validação concorrente em runtime.

## Limitações mantidas e fora do plano

- Datas preenchidas não podem ser apagadas pelo PATCH atual: null significa manter. Adicionar remoção explícita exige definir essa necessidade de contrato.
- O módulo Java administra o cadastro da tabela; o schema possui price_table_items, mas ainda não há implementação Java desse recurso. Implementá-lo seria uma funcionalidade nova.
- A presença de vínculo com uma tabela inativa ou fora da vigência não é bloqueada em customer. Não introduzir essa regra sem definição do usuário.

## Handoff

Padronização concluída e revisada por leitura. As correções adicionais deste plano foram implementadas e revisadas por leitura. Nenhum teste, build, lint ou SQL foi executado.

## Registro da execução

- Task 1: concluída. Consultas nativas de nome normalizado no cadastro e edição, com exclusão do próprio price_table_id. O service usa o nome final do PATCH e a V1 inclui uk_price_table_name_normalized com a mesma expressão lower(btrim(name)). O texto salvo mantém sua grafia original.
- Task 2: concluída. findByIdForUpdate usa PESSIMISTIC_WRITE. update e delete carregam o registro por esse método dentro de @Transactional; o cálculo dos valores finais ocorre após obter o bloqueio.
- Task 3: concluída por revisão direta do código e do diff. Parâmetros, SQL, validação anterior aos setters, preservação de campos omitidos e contratos de leitura foram conferidos. As consultas antigas não têm referências remanescentes em price.
- A leitura da V2 mostrou 20 cidades distintas nos cenários e nomes derivados dessas cidades; não foi identificada colisão com a nova chave na carga lida. Não houve execução de SQL.
- Foram alterados somente PriceTableRepository, PriceTableService e V1 nesta execução, além deste registro. A padronização anterior, os índices dos demais módulos, as FKs e a V2 foram preservados.
- Decisão de execução: seguir o plano aprovado no checkout atual, sem worktree, commits ou arquivos auxiliares; usar este documento como registro.
- Decisão de verificação: respeitar AGENTS.md e o plano aprovado, com revisão pelo próprio implementador, sem delegação. Não criar nem executar testes, build, lint ou SQL.
- Limites: compilação, aplicação do índice, execução das consultas e concorrência não foram verificadas em runtime. A proteção final do índice depende de recriar o banco com a V1 atualizada.

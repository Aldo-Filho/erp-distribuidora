# Supplier Address Uniqueness Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task after approval. Follow AGENTS.md; do not create or run tests, build or lint without an explicit user request. Do not delegate work unless authorized.

**Goal:** Corrigir os falsos positivos na comparação de endereços de fornecedores e impedir duplicatas em requisições simultâneas.

**Architecture:** O service continua responsável pelo fluxo de cadastro e PATCH. O repository consulta a chave completa proposta, usando a mesma comparação normalizada de um novo índice único no PostgreSQL. A consulta antecipada fornece a mensagem de negócio; o índice garante a integridade em concorrência.

**Tech Stack:** Java 21, Spring Data JPA, PostgreSQL e Flyway existentes, sem dependências ou extensões novas.

**Spec:** Correções da revisão de supplier aprovadas pelo usuário, com as regras de AGENTS.md. Após o planejamento, o usuário escolheu alterar a V1 existente e recriar o banco, sem criar outra migration. A implementação foi autorizada; a aplicação da migration não foi solicitada.

## Global Constraints

- Preservar as alterações existentes e o padrão de customer, adaptando-o ao domínio de fornecedores.
- Não criar nem executar testes automatizados, build ou lint.
- Não alterar URLs, DTOs públicos, exclusão de endereços ou os demais módulos.
- Alterar somente a seção de endereços de fornecedores da V1 existente. Não criar V3 nem modificar V2.
- Não executar a migration nem modificar dados do banco nesta implementação.
- Não implementar validação de CNPJ ou unicidade de fornecedor preferencial: esses assuntos ficaram fora das duas correções solicitadas.

## Regra aprovada de duplicidade

- A chave será: fornecedor, tipo de endereço, país, estado, cidade, rua, CEP, número e complemento.
- Fornecedores diferentes podem ter o mesmo endereço. O mesmo fornecedor continua podendo ter vários endereços do mesmo tipo, desde que a chave completa seja diferente.
- Bairro permanece obrigatório conforme o contrato atual, mas não integra a chave: corrigir o bairro de um local não deve transformá-lo em outro endereço.
- Comparar os textos sem distinção de maiúsculas, removendo espaços comuns nas extremidades. Usar a expressão SQL `lower(btrim(coalesce(valor, '')))` tanto na consulta quanto no índice; não misturar essa comparação com outra normalização em Java.
- Complemento nulo e complemento vazio são equivalentes. A expressão também evita que nulos legados nos demais textos contornem a unicidade; não altera a obrigatoriedade atual dos DTOs.
- Não remover acentos, hífens, pontuação ou espaços internos. Não tentar interpretar abreviações como Rua/R. ou Brasil/BR.
- Essa normalização é para comparação; não reescrever automaticamente os textos já armazenados. Preservar a limpeza de complemento vazio para null feita pelo service.

## Review Focus

Estes são critérios para revisão direta do código e do SQL, sem executar cenários ou criar testes:

1. Mesmo CEP e número em ruas, cidades ou países diferentes não podem gerar falso positivo.
2. A chave completa igual deve ser rejeitada, inclusive com diferenças de maiúsculas, espaços nas extremidades e complemento nulo/vazio.
3. PATCH deve combinar entrada e estado persistido, excluir o próprio ID e preservar campos omitidos ou nulos.
4. Duas gravações simultâneas da mesma chave não podem persistir duas linhas; a barreira final deve estar no banco.
5. O índice deve ser criado na V1, antes da carga V2 no banco novo; nenhuma rotina deve apagar ou mesclar endereços automaticamente.

## Task 1: Corrigir a comparação no repository e no service

**Files:**
- Modify: `backend/src/main/java/com/pi/erp/supplier/address/SupplierAddressRepository.java`.
- Modify: `backend/src/main/java/com/pi/erp/supplier/address/SupplierAddressService.java`.

**Interfaces:**
- `boolean existsDuplicateAddress(Long supplierId, String addressType, String country, String state, String city, String street, String zipCode, String number, String complement)`.
- `boolean existsDuplicateAddressAndIdNot(Long supplierId, String addressType, String country, String state, String city, String street, String zipCode, String number, String complement, Long id)`.
- Os métodos substituem as consultas derivadas atuais. Usar parâmetros nomeados explícitos e consulta nativa com `SELECT EXISTS`, pois a comparação deve reproduzir exatamente as expressões do índice.
- Converter o enum para seu nome textual no service antes da consulta nativa. No PATCH de dados legados, se o tipo final for nulo, passar null em vez de chamar `name()`; o cadastro continua exigindo o tipo pelo DTO atual.

- [x] Substituir as duas consultas atuais pela chave proposta, aplicando a normalização SQL definida acima a todos os componentes textuais, inclusive ao tipo do endereço.
- [x] Ajustar `register` para consultar todos os campos da chave antes de salvar, mantendo a busca do fornecedor e a normalização atual do complemento.
- [x] Ajustar `update` para calcular também os valores finais de país, estado, cidade e rua, junto aos valores já calculados de tipo, CEP, número e complemento.
- [x] Validar campos obrigatórios e duplicidade antes dos setters. Manter a exclusão do próprio ID e a mensagem `Supplier address already exists.`.
- [x] Preservar `@Transactional`, campos públicos, rotas e a possibilidade de repetir o tipo com endereços diferentes.
- [x] Revisar diretamente os critérios 1, 2 e 3 de Review Focus, comparando expressões, parâmetros e valores finais usados pelo cadastro e pelo PATCH.

## Task 2: Adicionar a garantia de unicidade no PostgreSQL

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__create-table-erp.sql`, inserindo o índice após a criação de `supplier_addresses`.
- Read only: `V2__insert-initial-data.sql` para compatibilidade com os dados iniciais existentes.

**Interfaces:**
- Índice único proposto: `uk_supplier_address_identity`.
- Chave: `supplier_id` e expressões normalizadas para `address_type`, `country`, `state`, `city`, `street`, `zip_code`, `number` e `complement`.
- O índice será a proteção contra concorrência; `@Transactional` e `existsDuplicateAddress` isoladamente não garantem unicidade.

- [x] Adaptar a execução à decisão posterior do usuário: adicionar o índice diretamente à V1, para uso em banco novo, sem rotina de migração ou limpeza de endereços legados.
- [x] Criar um índice único por expressões, usando exatamente a comparação do repository. Não depender de `NULLS NOT DISTINCT`, colunas auxiliares ou extensões.
- [x] Manter a criação do índice na transação normal da migration, sem `CONCURRENTLY`. O PostgreSQL continua responsável por detectar conflitos durante sua construção.
- [x] Preservar o tratamento centralizado atual: duplicidade detectada no service retorna 400 com mensagem de negócio; uma colisão de gravações rejeitada pelo banco segue o tratamento de `DataIntegrityViolationException`, com 409 e mensagem genérica existente.
- [x] Revisar diretamente os critérios 2, 4 e 5 de Review Focus. Conferir que os mesmos campos, normalização e tratamento de nulos aparecem no índice e nas consultas, e que o índice está definido antes da carga V2.

## Task 3: Revisão final e entrega

- [x] Revisar o diff dos dois arquivos Java, do trecho alterado na V1 e deste documento, preservando as alterações anteriores do usuário.
- [x] Conferir que não foram adicionados `@Size`, DTOs de resposta, novas rotas ou unicidade apenas por fornecedor + tipo.
- [x] Conferir que a exclusão com orphanRemoval e o cadastro transacional de fornecedor com endereço permanecem intactos.
- [x] Relatar o que mudou e deixar explícito que compilação, testes, concorrência real e aplicação Flyway não foram verificados.

## Limite da verificação

A revisão direta pode confirmar a coerência dos arquivos, mas não comprova execução do SQL, sucesso da migration ou comportamento concorrente em PostgreSQL. Qualquer validação em banco ou execução de testes deve ser solicitada separadamente, conforme as preferências do projeto.

## Estado

Implementação concluída e revisada diretamente. Testes, build, lint, aplicação Flyway e concorrência real não executados.

## Registro da execução

- Decisão: editar a V1 existente, conforme autorização posterior do usuário para recriar o banco; a proposta de V3 e a verificação de duplicados legados deixaram de se aplicar.
- Decisão: manter a implementação no checkout atual, preservando as alterações anteriores, sem commits ou criação de worktree.
- Decisão: respeitar a preferência explícita de não criar nem executar testes, build ou lint; a verificação desta tarefa se limita à revisão direta.
- Pré-revisão das interfaces: o repository e o índice usam os mesmos nove componentes da chave; o service fornece o nome textual do enum e os valores finais do PATCH.
- Detalhe de implementação: parâmetros textuais das consultas nativas possuem cast explícito para text, inclusive os que podem receber null. A expressão aplicada às colunas é igual à expressão do índice.
- Task 1: concluída; consultas nativas e valores finais do PATCH revisados diretamente.
- Task 2: concluída; índice adicionado somente à V1, antes da carga de dados, sem nova migration.
- Task 3: concluída; auto-revisão do repository, service, SQL e diff em relação ao início desta execução. Nenhuma validação de runtime realizada.

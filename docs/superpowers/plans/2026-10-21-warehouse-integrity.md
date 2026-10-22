# Warehouse Integrity Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task after approval. Follow AGENTS.md. Do not create or run tests, build or lint without an explicit user request. Do not delegate without authorization.

**Goal:** Corrigir a identificação de endereços de armazéns e garantir a unicidade de nomes e endereços também em cadastros simultâneos.

**Architecture:** Manter as validações antecipadas nos services e usar índices únicos no PostgreSQL como proteção final. Os repositories devem comparar os mesmos campos e aplicar a mesma normalização dos índices. Seguir customer na organização e no fluxo, respeitando as particularidades de warehouse.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, PostgreSQL e Flyway existentes, sem novas dependências.

**Spec:** Correções apontadas na revisão de warehouse e esclarecimento do usuário sobre complexos de armazéns na mesma rua, com números diferentes. Plano aprovado pelo usuário e implementado diretamente no projeto, com revisão estática.

## Global Constraints

- Conferir o Git antes de implementar e preservar todas as alterações existentes.
- Não criar nem executar testes automatizados, build ou lint. Fazer somente revisão estática dos trechos envolvidos.
- Seguir AGENTS.md, usando customer e customer/address como referência de estilo e responsabilidades.
- Editar a V1 existente, conforme a decisão anterior do usuário de recriar o banco. Não criar outra migration nem executar SQL, Flyway ou operações no banco.
- Preservar os índices de supplier, brand e category já incluídos na V1. Não modificar a V2.
- Manter os contratos de controllers, DTOs e PATCH, as mensagens de negócio aplicáveis e o tratamento centralizado de erros.
- Não alterar frontend, estoque, obrigatoriedade de complemento, edição do nome do armazém ou formato separado de cadastro de armazém e endereço.

## Regras propostas

### Nome do armazém

- Manter a regra atual de impedir nomes repetidos entre armazéns.
- Comparar com `lower(btrim(name))`: ignorar maiúsculas e espaços comuns nas extremidades.
- `Central`, `CENTRAL` e ` Central ` representam o mesmo nome para unicidade.
- Preservar a grafia armazenada. Não normalizar acentos, pontuação ou espaços internos.

### Endereço físico

- Manter a regra existente de no máximo um endereço por armazém, protegida por `UNIQUE (warehouse_id)`.
- Substituir a identificação atual, limitada a CEP, número e complemento, pela combinação de **estado, cidade, rua, CEP, número e complemento**.
- Propor que a repetição dessa combinação completa seja impedida entre armazéns diferentes. O exemplo do usuário permite unidades distintas no mesmo complexo; não exige cadastrar duas unidades com todos os identificadores físicos iguais.
- Não incluir `warehouse_id` nessa chave física: ele já é único e tornaria a proteção contra endereços repetidos entre armazéns ineficaz.
- Bairro permanece descritivo, fora da identidade, para que uma diferença nesse campo não permita duplicar o mesmo local.
- Aplicar `lower(btrim(...))` a cada componente, tanto nas consultas quanto no índice. Todos esses campos continuam obrigatórios e não nulos, conforme o schema atual.
- Não adicionar país ou tipo de endereço; warehouse não possui esses campos.
- Não interpretar abreviações, equivalência de CEP com e sem pontuação, acentos ou zeros do número. Essa padronização seria outro escopo.

| Situação, mantendo os outros campos iguais | Resultado proposto |
| --- | --- |
| Mesma rua, números 100 e 102 | Permitir |
| Mesmo número, complementos Galpão A e Galpão B | Permitir |
| Mesmo CEP, número e complemento, mas ruas ou cidades diferentes | Permitir |
| Todos os componentes da identidade iguais em armazéns diferentes | Rejeitar |
| Identidade igual, variando apenas maiúsculas ou espaços nas extremidades | Rejeitar |
| Editar o endereço sem mudar sua identidade | Permitir, excluindo o próprio ID da consulta |
| Cadastrar outro endereço para um armazém que já possui um | Rejeitar |

## Review Focus

1. A consulta antecipada e o índice devem usar exatamente a mesma identidade e normalização.
2. Na edição, combinar os valores recebidos e persistidos antes de validar duplicidade, excluindo o próprio endereço.
3. A verificação física não deve substituir a verificação independente de um endereço por armazém.
4. As validações devem ocorrer antes dos setters; valores omitidos ou nulos continuam preservando os dados atuais no PATCH.
5. Os índices devem constituir a proteção final contra concorrência. Uma consulta `exists` isolada não oferece essa garantia.
6. Revisar textualmente a compatibilidade da V2, sem presumir que ela foi executada.

## Task 1: Proteger a unicidade do nome

**Arquivos:**
- `backend/src/main/java/com/pi/erp/warehouse/WarehouseRepository.java`
- `backend/src/main/java/com/pi/erp/warehouse/WarehouseService.java`
- `backend/src/main/resources/db/migration/V1__create-table-erp.sql`

- [x] Ler os equivalentes em customer e o código atual de warehouse antes de editar. Consultar BrandRepository como precedente local da comparação normalizada.
- [x] Substituir `existsByNameIgnoreCase` por `existsByNormalizedName`, com `@Query` nativa, `SELECT EXISTS` e `@Param` explícito.
- [x] Comparar `lower(btrim(name))` com `lower(btrim(cast(:name AS text)))`, preservando o valor recebido para armazenamento.
- [x] Atualizar a validação em WarehouseService.register, mantendo sua exceção de negócio e o fluxo de validação antes de salvar.
- [x] Adicionar após a criação de warehouses:

```sql
CREATE UNIQUE INDEX uk_warehouse_name_normalized
    ON warehouses (lower(btrim(name)));
```

## Task 2: Corrigir a identidade física no cadastro e no PATCH

**Arquivos:**
- `backend/src/main/java/com/pi/erp/warehouse/address/WarehouseAddressRepository.java`
- `backend/src/main/java/com/pi/erp/warehouse/address/WarehouseAddressService.java`
- `backend/src/main/resources/db/migration/V1__create-table-erp.sql`

- [x] Ler os equivalentes de customer/address e o fluxo atual de cadastro, edição e associação de warehouse.
- [x] Preservar `existsByWarehouseId` e `existsByWarehouseIdAndIdNot`.
- [x] Substituir as duas consultas antigas de CEP/número/complemento por `existsDuplicateAddress` e `existsDuplicateAddressAndIdNot`, recebendo estado, cidade, rua, CEP, número e complemento; a segunda também recebe o ID excluído.
- [x] Implementar ambas como consultas nativas `SELECT EXISTS`, com parâmetros explícitos. Aplicar `lower(btrim(column)) = lower(btrim(cast(:parameter AS text)))` aos seis componentes e `warehouse_address_id <> :id` somente na edição.
- [x] No cadastro, verificar a identidade completa após resolver o armazém e verificar sua disponibilidade. Manter a mensagem de endereço já existente.
- [x] No PATCH, calcular também estado, cidade e rua finais, junto de CEP, número e complemento, combinando entrada e dados persistidos.
- [x] Validar os valores finais e a disponibilidade do armazém antes dos setters. Usar a consulta que exclui o próprio ID e preservar a possibilidade atual de trocar o armazém associado quando o destino estiver disponível.
- [x] Aplicar os valores validados e retornar `repository.save(address)`, sem normalizar o texto armazenado como efeito colateral.
- [x] Adicionar após a criação de warehouse_addresses, preservando sua constraint de warehouse_id:

```sql
CREATE UNIQUE INDEX uk_warehouse_address_identity
    ON warehouse_addresses (
        lower(btrim(state)),
        lower(btrim(city)),
        lower(btrim(street)),
        lower(btrim(zip_code)),
        lower(btrim(number)),
        lower(btrim(complement))
    );
```

## Task 3: Revisar o conjunto e registrar os limites da verificação

- [x] Revisar diretamente os cinco arquivos de implementação alterados, seus imports, nomes, mensagens e formatação, sem executar formatador.
- [x] Conferir por leitura as situações da tabela de regras, inclusive PATCH sem alteração de identidade e realocação para armazém já ocupado.
- [x] Conferir que os dois índices correspondem às consultas e que os índices já existentes de outros módulos permanecem intactos.
- [x] Inspecionar a V2 textualmente quanto aos nomes e identidades de endereço gerados. Não remover ou ajustar dados automaticamente caso apareça incompatibilidade; registrar a situação antes de ampliar o escopo.
- [x] Confirmar no tratamento centralizado existente que colisões de integridade seguem a resposta de conflito já adotada, sem criar tratamento genérico novo apenas para este módulo.
- [x] Revisar o diff e informar os arquivos efetivamente alterados, sem atribuir à revisão estática garantias de execução ou concorrência observada.
- [x] Registrar que testes, build, lint e migrations não foram executados. A V1 proposta destina-se ao banco recriado; não será aplicada sobre uma instalação existente nesta tarefa.

## Handoff

Implementação concluída nos cinco arquivos existentes previstos. Nenhum DTO, controller, entidade ou arquivo novo de migration foi criado ou alterado nesta execução. As consultas e os dois índices usam a mesma identidade e normalização, preservando as regras atuais de associação e PATCH.

## Registro da execução

- Task 1: concluída. WarehouseRepository usa SELECT EXISTS com lower(btrim(name)); WarehouseService utiliza a consulta e a V1 inclui uk_warehouse_name_normalized.
- Task 2: concluída. As duas consultas de endereço consideram estado, cidade, rua, CEP, número e complemento; o PATCH combina os valores finais e exclui seu próprio ID antes dos setters. A V1 inclui uk_warehouse_address_identity e mantém UNIQUE (warehouse_id).
- Task 3: concluída por revisão direta dos arquivos e do diff. Não foram encontradas referências aos métodos antigos dentro de warehouse. Imports, parâmetros, campos comparados e expressões dos índices foram conferidos.
- A leitura da V2 mostrou cidades distintas nos 20 cenários, nomes derivados dessas cidades e números de endereço calculados como 100 + item.n. Não foi identificada colisão com as novas chaves na carga lida; nenhuma migration foi executada.
- O tratamento centralizado existente mantém HTTP 409 para DataIntegrityViolationException, com mensagem genérica sem detalhes internos do banco. Não foi alterado.
- Os índices anteriores de supplier, brand e category foram preservados. Controllers, DTOs, entidades, frontend e V2 não foram modificados nesta execução.
- Decisão de execução: respeitar o pedido de alteração direta no checkout atual e usar este documento como registro, sem criar worktree, commits ou arquivos auxiliares de execução.
- Decisão de verificação: prevalecem AGENTS.md e o plano aprovado; revisão feita pelo próprio implementador, sem delegação e sem criar ou executar testes, build ou lint.
- Limite: compilação, execução das consultas, aplicação dos índices e comportamento concorrente não foram verificados em runtime. A proteção do banco exige sua recriação com a V1 atualizada.

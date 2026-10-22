# Product Integrity and Category Removal Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task after approval. Follow AGENTS.md. Do not create or run tests, build or lint without an explicit user request. Do not delegate without authorization.

**Goal:** Garantir a unicidade de nomes de marcas e categorias em gravações simultâneas e permitir remover explicitamente a categoria de um produto no PATCH.

**Architecture:** Manter a consulta antecipada nos services e adicionar índices únicos compatíveis no PostgreSQL. Ampliar o record de PATCH com uma flag de ação, seguindo o padrão de removePriceTable em customer, sem mudar o significado de campos omitidos ou nulos.

**Tech Stack:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL e Flyway existentes, sem novas dependências.

**Spec:** Dois pontos da revisão de product e regras de AGENTS.md. Plano aprovado pelo usuário para implementação direta, mantendo a decisão de editar a V1 para um banco novo. Execução de migrations não solicitada.

## Global Constraints

- Preservar alterações existentes e seguir a organização e o estilo de customer.
- Não criar nem executar testes automatizados, build ou lint.
- Considerar a decisão já expressa pelo usuário de recriar o banco e editar a V1 existente, sem nova migration. Não aplicar migrations nem alterar dados nesta tarefa.
- Preservar o índice de endereços de supplier já adicionado à V1 e não modificar a V2.
- Manter URLs, respostas atuais e DTOs de cadastro. A única ampliação pública proposta é a flag opcional de ação removeCategory no PATCH.
- Não alterar frontend, geração de SKU, campos fiscais ou regras de estoque. Não criar endpoints de edição de marcas e categorias.

## Regras aprovadas

### Unicidade de marcas e categorias

- Cada tabela terá nomes únicos dentro dela própria; uma marca e uma categoria podem ter o mesmo nome.
- Comparar nomes sem distinção de maiúsculas e ignorando espaços comuns nas extremidades, usando `lower(btrim(name))` no banco e nas consultas.
- Exemplos: `Acme`, `ACME` e ` Acme ` representam o mesmo nome.
- Preservar acentos, pontuação e espaços internos. Não tentar interpretar abreviações nem padronizar nomes além dessa comparação.
- Não reescrever os nomes recebidos ou existentes apenas para comparar. Manter records de entrada simples e a obrigatoriedade atual de nome não vazio.
- Duplicidade antecipada mantém IllegalArgumentException e as mensagens atuais. Colisões de concorrência seguem o tratamento existente de DataIntegrityViolationException, retornando 409 sem detalhes internos do banco.

### Remoção de categoria no PATCH

- Adicionar `boolean removeCategory` ao final de PatchProductDTO, com false quando omitido, como removePriceTable em customer.
- Campo categoryId omitido ou nulo, sem removeCategory: manter a associação atual.
- categoryId informado, sem remoção: buscar e associar a categoria; ID inexistente continua retornando recurso não encontrado.
- removeCategory igual a true: remover somente a associação do produto, sem excluir a categoria nem afetar outros produtos.
- removeCategory igual a true junto com categoryId preenchido: rejeitar com IllegalArgumentException antes de aplicar setters.
- Remover a associação de um produto que já está sem categoria é permitido e mantém null.
- Marca continua obrigatória e não ganha flag de remoção.

## Review Focus

Critérios para revisão direta do código e do SQL, sem executar cenários:

1. A comparação do repository e do índice deve ser igual, inclusive para maiúsculas e espaços nas extremidades.
2. Duas gravações concorrentes do mesmo nome devem encontrar a barreira final no índice único, não apenas no exists do service.
3. A carga V2 deve ser inspecionada textualmente quanto à compatibilidade com a nova chave; sua execução não será presumida.
4. Um PATCH antigo, sem removeCategory, deve manter o comportamento atual.
5. Remoção e associação simultâneas devem ser rejeitadas antes de qualquer alteração da entidade.
6. O índice de supplier e o cadastro transacional de produto com estoque devem permanecer intactos.

## Task 1: Sincronizar a verificação de nomes com a proteção no banco

**Files:**
- Modify: `backend/src/main/java/com/pi/erp/product/brand/BrandRepository.java`.
- Modify: `backend/src/main/java/com/pi/erp/product/brand/BrandService.java`.
- Modify: `backend/src/main/java/com/pi/erp/product/category/CategoryRepository.java`.
- Modify: `backend/src/main/java/com/pi/erp/product/category/CategoryService.java`.
- Modify: `backend/src/main/resources/db/migration/V1__create-table-erp.sql`.
- Read only: `backend/src/main/resources/db/migration/V2__insert-initial-data.sql`.

**Interfaces:**
- Substituir `existsByNameIgnoreCase(String name)` por `boolean existsByNormalizedName(@Param("name") String name)` em cada repository, com @Query nativa e SELECT EXISTS.
- Comparar `lower(btrim(name))` com `lower(btrim(cast(:name AS text)))`, nas tabelas brands e categories respectivamente.
- Criar os índices `uk_brand_name_normalized` em brands e `uk_category_name_normalized` em categories, ambos sobre `lower(btrim(name))`.

- [x] Conferir os usos das consultas antigas e substituir as chamadas nos dois services.
- [x] Manter as mensagens `This brand already exists.` e `This category already exists.` e salvar somente depois da verificação.
- [x] Adicionar cada índice à V1 depois da criação da respectiva tabela, sem modificar o índice existente de supplier.
- [x] Inspecionar a carga V2; se houver nomes repetidos pela nova regra, registrar a incompatibilidade para decisão do usuário, sem apagar, mesclar ou renomear dados silenciosamente.
- [x] Revisar os critérios 1, 2, 3 e 6 de Review Focus. Não substituir a comparação normalizada por UNIQUE simples, que não expressaria a mesma regra de maiúsculas.

## Task 2: Permitir remover a categoria do produto

**Files:**
- Modify: `backend/src/main/java/com/pi/erp/product/PatchProductDTO.java`.
- Modify: `backend/src/main/java/com/pi/erp/product/ProductService.java`.
- Read only: `backend/src/main/java/com/pi/erp/customer/PatchCustomerDTO.java` e `CustomerService.java` como referência da flag removePriceTable.

**Interfaces:**
- PatchProductDTO ganha `boolean removeCategory` ao final; os campos existentes permanecem com os mesmos tipos e validações.
- `ProductService.update(Long id, PatchProductDTO data)` permanece com a mesma assinatura.
- Mensagem para conflito de intenções: `Cannot provide a category and request its removal at the same time.`.

- [x] Adicionar a flag ao record e conferir possíveis chamadas ao construtor no projeto, sem adicionar testes.
- [x] Rejeitar o conflito entre removeCategory e categoryId antes dos setters.
- [x] Calcular a categoria final: null para remoção; findCategory para um novo ID; categoria atual para omissão. Buscar associações antes de alterar a entidade.
- [x] Preservar a validação de marca, os demais campos do PATCH, o retorno da entidade salva e as regras do cadastro com estoque inicial.
- [x] Revisar diretamente os critérios 4, 5 e 6 de Review Focus.

## Task 3: Revisão e entrega

- [x] Conferir o diff dos sete arquivos de implementação e preservar alterações anteriores.
- [x] Conferir imports explícitos, mensagens em inglês, nomes data/filter/id e organização dos métodos conforme AGENTS.md.
- [x] Confirmar que não foram introduzidos @Size, abstrações genéricas, alterações no frontend ou novas migrations.
- [x] Informar exatamente o que mudou e registrar que testes, build, lint, aplicação Flyway e concorrência real não foram verificados.

## Limite da verificação

A revisão direta confirma a coerência da implementação proposta, mas não comprova compilação, execução do SQL, comportamento concorrente ou aplicação da V2. Essas verificações dependem de solicitação explícita posterior.

## Estado

Implementação concluída e revisada diretamente. Testes, build, lint, aplicação Flyway e concorrência real não executados.

## Registro da execução

- Task 1: concluída. Services e índices usam lower(btrim(name)); mensagens de duplicidade preservadas. A inspeção textual dos 20 cenários da V2 não identificou nomes repetidos de marcas ou categorias; a carga não foi executada.
- Task 2: concluída. removeCategory segue o padrão de removePriceTable; conflito de intenções rejeitado antes dos setters. Nenhuma chamada direta ao construtor de PatchProductDTO foi encontrada no código Java consultado.
- Task 3: concluída por auto-revisão do diff em relação ao estado inicial desta execução. O índice de supplier e o cadastro transacional com estoque inicial foram preservados.
- Decisão: executar diretamente no checkout atual, preservando alterações anteriores, sem commits ou criação de worktree.
- Decisão: seguir a preferência explícita do usuário por revisão direta, sem criar ou executar testes, build ou lint; nenhuma migration foi aplicada.

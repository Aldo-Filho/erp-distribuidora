# Automatic Product SKU Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans após aprovação deste plano. Seguir AGENTS.md. Não criar ou executar testes, build ou lint sem solicitação explícita. Não delegar, aplicar migrations ou realizar commits automaticamente.

**Goal:** Gerar o SKU de novos produtos no backend, dispensando seu preenchimento no cadastro e preservando os códigos existentes.

**Architecture:** Uma sequência PostgreSQL fornece a numeração concorrente por meio de ProductRepository. SkuGenerator formata essa numeração e ProductService coordena a geração antes de salvar o produto, mantendo a transação com estoque inicial.

**Tech Stack:** Java, Spring Boot, Spring Data JPA, PostgreSQL e Flyway existentes; nenhuma dependência nova.

**Spec:** Solicitação do usuário, AGENTS.md e regras deste documento. Plano aprovado pelo usuário, incluindo formato PRD-000001, alteração do contrato de cadastro e criação da sequência diretamente na V1 para banco novo.

## Global Constraints

- Usar customer e customer/address como referência de organização, injeção e separação de responsabilidades, adaptando as diferenças necessárias à geração de SKU.
- Manter classes no pacote com.pi.erp.product; nomes e mensagens em inglês, quatro espaços em Java, imports explícitos nos services e alterações locais de formatação.
- Manter DTOs como records de transporte, com um campo por linha e sem construtores de normalização.
- Preservar validações existentes dos demais campos, @Valid no cadastro e no estoque aninhado, métodos search/register/update/delete e injeção de dependências por campos com @Autowired.
- Preservar os retornos de entidades, URLs, códigos HTTP, filtros e tratamento centralizado de erros. Não introduzir DTOs de resposta.
- Não alterar frontend, regras fiscais ou regras de estoque. Por autorização explícita do usuário, modificar V1__create-table-erp.sql considerando a criação de outro banco. Preservar V2 e não criar migration incremental.
- Não criar ou executar testes automatizados, build ou lint. Fazer revisão direta e informar seus limites.
- Preservar alterações do usuário: no início do planejamento havia exclusão de Insomnia_2026-10-06-22-12-46.yaml e arquivo não rastreado testeRequisicoesInsomnia.yaml. Reconsultar o Git antes da implementação.

## Comportamento proposto

- Formato: PRD-000001, PRD-000002 etc., com prefixo fixo e no mínimo seis dígitos. Após PRD-999999, gerar PRD-1000000, sem truncamento ou reinício.
- Geração exclusivamente no cadastro pelo backend. Remover sku de RequestProductDTO; o consumidor deixa de enviar o campo. O SKU gerado continua presente na entidade retornada pelo POST e nas consultas.
- A alteração da entrada do POST é intencional e foi aprovada com o plano. Não adicionar configuração global do Jackson para rejeitar propriedades desconhecidas; um campo JSON antigo não poderá determinar o SKU persistido, independentemente de ser ignorado ou rejeitado pela configuração existente.
- SKU não depende de nome, marca ou categoria e não muda em edições. PatchProductDTO já não contém sku e deve continuar assim.
- Produtos existentes, inclusive os códigos descritivos da V2, mantêm seus SKUs. Não fazer recálculo ou atualização em massa.
- Reservar a faixa PRD- seguida de dígitos para o gerador. No banco novo, iniciar a sequência em 1; conferir textualmente que os SKUs da V2 não usam essa faixa.
- A numeração pode ter lacunas em caso de rollback, falha ou exclusão. Não é um contador de produtos, não acompanha product_id e não promete números consecutivos entre produtos persistidos.
- Manter NOT NULL e UNIQUE de products.sku. Não usar count, MAX + 1, timestamp ou contador em memória. Não é necessário calcular um valor inicial com base em produtos existentes, pois a V1 será aplicada em um banco novo.
- O uso da sequência é específico do domínio e justifica uma consulta nativa no repository; customer não possui operação equivalente a copiar. O gerador é um componente pequeno de formatação, sem repository próprio, tabela de entidade ou CRUD.

## Alternativas consideradas

- **Sequência PostgreSQL + prefixo fixo — recomendada:** código legível, geração concorrente centralizada e nenhuma dependência adicional; exige migration e aceita lacunas.
- **UUID:** dispensa sequência, mas produz códigos maiores e menos práticos para leitura e digitação. Não encurtar UUID presumindo unicidade garantida.
- **Prefixos de marca/categoria:** acrescentam regras para abreviações, colisões e edição das associações sem necessidade informada. Não adotar neste plano.

## Arquivos e responsabilidades

| Ação | Arquivo relativo à raiz | Responsabilidade |
| --- | --- | --- |
| Alterar | backend/src/main/resources/db/migration/V1__create-table-erp.sql | Criar a sequência iniciando em 1 no banco novo. |
| Alterar | backend/src/main/java/com/pi/erp/product/ProductRepository.java | Consultar o próximo número da sequência. |
| Criar | backend/src/main/java/com/pi/erp/product/SkuGenerator.java | Formatar o número como SKU. |
| Alterar | backend/src/main/java/com/pi/erp/product/RequestProductDTO.java | Remover o SKU da entrada de cadastro. |
| Alterar | backend/src/main/java/com/pi/erp/product/Product.java | Receber o SKU gerado no construtor. |
| Alterar | backend/src/main/java/com/pi/erp/product/ProductService.java | Coordenar geração, persistência e estoque inicial. |

Ler ProductController, ProductFilter, PatchProductDTO e V2__insert-initial-data.sql para revisão de compatibilidade; nenhuma alteração funcional prevista nesses arquivos. Comparar os equivalentes de customer antes de concluir.

## Review Focus

Critérios para inspeção direta; não são cenários executados:

1. Cadastros simultâneos obtêm números distintos por nextval; UNIQUE permanece como proteção final.
2. Os SKUs descritivos da V2 são preservados, não ocupam a faixa PRD numérica e não colidem com o primeiro número reservado.
3. Falha no estoque inicial reverte produto e estoque na transação existente, embora possa consumir um número da sequência.
4. PATCH de nome, marca, categoria ou active mantém o SKU original.
5. Cadastro sem sku continua sujeito às validações existentes dos demais campos; o SKU está atribuído antes do primeiro save e retorna na resposta.

## Task 1: Disponibilizar a numeração no banco

**Files:** V1__create-table-erp.sql e ProductRepository.java, nos caminhos acima; V2__insert-initial-data.sql somente para leitura.

**Interfaces:** Sequência product_sku_seq, tipo BIGINT, incremento 1, mínimo 1 e NO CYCLE. Repository expõe Long nextSkuNumber(), com @Query nativa SELECT nextval('product_sku_seq').

- [x] Adicionar CREATE SEQUENCE product_sku_seq à V1, próximo da definição de products, com AS BIGINT, START WITH 1, INCREMENT BY 1, MINVALUE 1 e NO CYCLE. Preservar as demais tabelas, índices e constraints.
- [x] Conferir textualmente que nenhum SKU da V2 corresponde a ^PRD-[0-9]+$. Se houver conflito, registrar a incompatibilidade para revisão do plano antes de implementar, sem renomear dados silenciosamente.
- [x] Deixar o primeiro nextval retornar 1. Não introduzir setval, cálculo de MAX, rotina de reinicialização ou reinício após exclusão de produtos; a sequência permanece independente de product_id.
- [x] Adicionar nextSkuNumber ao ProductRepository, seguindo a apresentação das consultas nativas já usadas em product/brand e product/category. Não usar @Modifying para esta consulta SELECT.
- [x] Revisar SQL e consulta em conjunto: nomes idênticos, primeiro número igual a 1, compatibilidade com V2, ausência de alterações em seus SKUs e proteção contra reinício por NO CYCLE.

**Entrega:** Migration e contrato de acesso à sequência preparados; nenhuma aplicação Flyway nesta tarefa.

## Task 2: Integrar o gerador ao cadastro

**Files:** SkuGenerator.java, RequestProductDTO.java, Product.java, ProductService.java e ProductRepository.java, nos caminhos acima.

**Interfaces:**

- SkuGenerator: @Component com public String generate(long number). Formatação com Locale.ROOT e PRD-%06d, sem consultar o banco e sem dependências injetadas.
- ProductService: @Autowired private SkuGenerator skuGenerator; mantém public Product register(RequestProductDTO data).
- Product: construtor public Product(RequestProductDTO data, Brand brand, Category category, String sku), atribuindo this.sku = sku.
- RequestProductDTO: mesmos campos e constraints atuais, exceto remoção de sku e de seu TODO.

- [x] Criar SkuGenerator no pacote product, implementando somente a formatação definida. Não criar uma pasta global util nem uma abstração genérica de geração de códigos.
- [x] Remover sku de RequestProductDTO e adaptar o construtor de Product para receber o valor gerado separadamente. Remover somente os TODOs relativos a SKU; preservar callbacks, mapeamentos, active e demais atributos.
- [x] Em register, resolver marca e categoria antes de reservar a numeração. Obter long number = repository.nextSkuNumber(), calcular String sku = skuGenerator.generate(number) e construir Product com esse SKU antes de repository.save.
- [x] Retirar a consulta existsBySku(data.sku()) e sua mensagem do cadastro, pois o cliente não define mais a chave. Conferir todos os usos e remover existsBySku do repository se continuar sem consumidores; não substituir por laço de tentativas ou por checagem que prometa resolver concorrência.
- [x] Preservar @Transactional, savedProduct e a delegação para StockItemService com o ID salvo. Não criar uma segunda transação para produto nem alterar regras de estoque.
- [x] Buscar e adaptar chamadas aos construtores alterados no código existente, sem criar testes. As consultas feitas no planejamento encontraram somente a construção de Product no service e nenhuma construção direta de RequestProductDTO no Java consultado; confirmar antes de editar.
- [x] Conferir que ProductController segue retornando Product com HTTP 201, que o filtro por SKU continua funcional no código e que update não passa a chamar o gerador.

**Entrega:** Novo cadastro deixa de depender do SKU fornecido na entrada e passa a persistir o código gerado pelo backend.

## Task 3: Revisão e entrega

- [x] Comparar os trechos alterados aos equivalentes de customer: controller enxuto, coordenação no service, acesso ao banco no repository, record simples e entidade recebendo dados já resolvidos.
- [x] Revisar os cinco pontos de Review Focus pelo código/SQL, incluindo a expansão acima de seis dígitos. Não registrar essa inspeção como comprovação de execução ou concorrência real.
- [x] Conferir o diff somente dos arquivos desta implementação, sem formatação global ou alterações nos arquivos Insomnia do usuário.
- [x] Informar a mudança do contrato POST /product: cadastro sem sku e resposta com sku gerado. Não alterar frontend ou coleções de requisições nesta implementação; comunicar a necessidade de adequação caso algum consumidor ainda exija preenchimento manual.
- [x] Informar que testes, build, lint, aplicação da migration e verificação concorrente não foram executados. Não iniciar aplicação ou alterar banco automaticamente.

**Implantação futura:** Criar outro banco e aplicar a V1 modificada seguida da V2 antes de disponibilizar o backend novo. A edição da V1 não é uma atualização de banco já migrado; não executar Flyway repair nem apagar o banco atual. Importações futuras que precisem criar produtos na faixa PRD numérica devem usar o mesmo mecanismo. A criação do banco e a aplicação das migrations não fazem parte deste pedido de planejamento.

## Estado

Implementação concluída e revisada diretamente nos seis arquivos previstos. Testes, build, lint, aplicação Flyway e verificação concorrente não foram executados.

## Registro da execução

- Pre-flight: Task 1 produz product_sku_seq e Long nextSkuNumber(); Task 2 consome os mesmos nomes e tipos. Nenhum conflito entre interfaces identificado.
- Ruling: executar no checkout atual e registrar o andamento neste plano, preservando alterações existentes; não criar worktree, commits ou delegação para esta implementação localizada. A revisão é feita pelo próprio implementador.
- Ruling: seguir AGENTS.md e o plano aprovado para revisão direta, sem TDD, testes, build ou lint; essa revisão não comprova compilação, execução do SQL ou concorrência real.
- Task 1: complete por inspeção direta. A V1 define product_sku_seq com início em 1 e NO CYCLE. O repository usa SELECT nextval sobre o mesmo nome. A leitura dos 20 SKUs da V2 não encontrou códigos na faixa PRD numérica; a V2 não foi alterada ou executada.
- Task 2: complete por inspeção direta. SkuGenerator formata com Locale.ROOT e PRD-%06d; register resolve associações antes de obter o número e atribui o SKU antes de salvar. RequestProductDTO não recebe sku e Product recebe o código como parâmetro separado. Removidos existsBySku e os TODOs de geração; nenhum consumidor restante dessas referências foi encontrado no backend consultado.
- Task 3: complete por auto-revisão do diff e comparação com customer. Preservados @Transactional, estoque inicial, validações dos outros campos, entidade retornada, filtros, PATCH, callbacks e mapeamentos. O campo sku continua NOT NULL e UNIQUE. Nenhuma alteração funcional em controller, frontend, V2 ou arquivos Insomnia do usuário.
- Limites: a compatibilidade de código foi inspecionada; não houve requisições HTTP, compilação, execução de migrations ou comprovação de comportamento simultâneo. O novo banco precisa receber a V1 modificada antes do uso do cadastro automático.

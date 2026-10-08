# Auditoria do backend — 08/10/2026

O backend compila e inicia, mas apresenta falhas de validação e alguns comportamentos incorretos nas operações de atualização e exclusão. Os fluxos válidos funcionaram nos cenários cobertos; ainda não é possível considerar os módulos totalmente validados.

## Resultado

- Compilação Maven: sucesso, com os 78 arquivos Java do backend.
- Teste JUnit existente (`ErpApplicationTests.contextLoads`): 1 executado, sem falhas.
- Auditoria HTTP: **189 verificações, 169 aprovadas e 20 reprovadas**, sem interrupção da suíte.
- Banco vazio de teste: as duas migrações Flyway foram aplicadas com sucesso e a aplicação iniciou com validação do esquema Hibernate.

O teste JUnit existente usa a configuração padrão do projeto. A auditoria HTTP usou uma instância independente da aplicação na porta 18080 e um cluster PostgreSQL dedicado na porta 55432, com o banco `backend_audit`. Os cadastros e exclusões da auditoria HTTP ocorreram exclusivamente nesse banco. Não foram alterados registros do banco `erp` pelos testes HTTP.

A aplicação de teste e o PostgreSQL dedicado foram encerrados ao terminar. Os dados e logs de auditoria foram preservados em `backend/target`.

| Módulo                    | Aprovadas | Reprovadas |
| ------------------------- | --------: | ---------: |
| Marcas                    |        10 |          2 |
| Categorias                |        10 |          1 |
| Armazéns                  |        10 |          2 |
| Tabelas de preço          |        15 |          3 |
| Produtos                  |        17 |          4 |
| Estoque                   |        26 |          1 |
| Clientes                  |        20 |          2 |
| Endereços de clientes     |        10 |          2 |
| Fornecedores              |        26 |          2 |
| Endereços de fornecedores |        14 |          1 |
| Endereços de armazéns     |        11 |          0 |
| **Total**                 |   **169** |     **20** |

Esses números representam cenários desta suíte, não uma porcentagem de cobertura de código. Um módulo sem reprovações nesta rodada ainda pode ter problemas em cenários não cobertos.

## Falhas confirmadas e prioridades

### 1. As anotações de validação não estão sendo executadas — prioridade alta

O `pom.xml` declara `jakarta.validation-api`, mas não inclui o provedor de Bean Validation. O log contém `NoProviderFoundException`, informando que nenhum provedor está disponível.

Reproduções:

- `POST /brand`, `/category`, `/warehouse` e `/price-table` com `name: ""`: retornam 201 e persistem o cadastro.
- `POST /product` com nome vazio e os demais campos preenchidos: retorna 201.
- `POST /customer` e `/supplier` com `legalName: ""`: retornam 201.
- `POST /customerAddress` com rua vazia: retorna 201.
- `POST /stockItem` com `{}`: retorna 500 por `NullPointerException` ao tentar comparar `quantity`, que está nulo.

São **9 verificações reprovadas** relacionadas a essa causa. A API deveria rejeitar esses dados antes de executar as regras de negócio. A correção recomendada é usar o starter de validação do Spring Boot e manter `@Valid` nas entradas adequadas. Campos nulos ou vazios não devem depender apenas das restrições do banco.

### 2. Exclusão individual de endereço de fornecedor falha — prioridade alta

`DELETE /supplierAddress/{id}` retorna 500 para um endereço existente.

O mapeamento atual de `Supplier` é `@OneToMany`, com `List<SupplierAddress>` e `orphanRemoval = true`. Entretanto, `SupplierAddressService.delete` executa `supplier.setSupplierAddress(null)` antes de excluir o endereço.

O Hibernate lança:

```text
A collection with orphan deletion was no longer referenced by the owning entity instance
```

É necessário remover o endereço específico da coleção gerenciada, mantendo a própria coleção e os outros endereços do fornecedor. A exclusão do fornecedor completo, com cascade, passou nos testes.

O cadastro de um segundo endereço para o mesmo fornecedor foi tratado como válido nesta auditoria, pois o mapeamento atual é de um fornecedor para vários endereços. A descrição antiga da coleção do Insomnia sobre um único endereço por fornecedor precisa ser atualizada para refletir essa alteração.

### 3. Restrições de relacionamento viram erro 500 — prioridade média

O banco impede corretamente certas operações, mas a API não converte as exceções em respostas adequadas para o cliente.

Reproduções:

- Excluir um produto com item de estoque vinculado: 500.
- Excluir uma marca com produto vinculado: 500.
- Excluir uma tabela de preço com cliente vinculado: 500.
- Cadastrar outro endereço `MAIN` para o mesmo cliente: 500 por violação de `uk_customer_address_type`.

São **4 verificações reprovadas**. O banco preservou a integridade; o problema é o tratamento e a explicação do erro. Recomenda-se validar as operações conhecidas e tratar `DataIntegrityViolationException`, retornando um erro de negócio, por exemplo 409, com mensagem clara.

### 4. PATCH vazio apaga a descrição do armazém — prioridade média

Depois de salvar uma descrição, `PATCH /warehouse/{id}` com `{}` retorna 200 e grava `description = null`.

`WarehouseService.update` chama `setDescription(data.description())` sem verificar se o campo foi enviado. A correção deve preservar a descrição quando ela não vier no PATCH. Também é necessário alinhar a validação do `PatchWarehouseDTO` à política desejada para atualizações parciais: exigir `description` em todos os PATCHs tornaria `{}` inválido, em vez de preservar o registro.

### 5. Cadastro ignora `active: false` — prioridade média

Reproduzido em clientes e tabelas de preço: a requisição envia `active: false`, mas a resposta e o registro ficam com `active: true`.

Os construtores `Customer` e `PriceTable` atribuem `true` diretamente. A API deve usar o valor informado quando ele existir e aplicar o valor padrão apenas quando o campo for omitido. São **2 verificações reprovadas**. A mudança de situação pelo PATCH funcionou nos testes.

### 6. Valores negativos são aceitos fora do estoque — lacuna nas regras de negócio

Reproduções:

- `PATCH /product/{id}` com `price: -1`: retorna 200 e salva o preço negativo.
- `POST /product` com `cost: -1`: retorna 201.
- `PATCH /supplier/{id}` com `avgDeliveryDays: -1`: retorna 200.

São **3 verificações reprovadas** segundo a expectativa da auditoria de impedir valores negativos para esses campos. Essa expectativa é uma regra de negócio proposta; os DTOs atuais não a declaram. Ativar o provedor de validação, sozinho, não resolverá esses casos: é necessário adicionar as restrições apropriadas ou verificações no service, incluindo os PATCHs.

## Comportamentos que passaram

- Cadastro válido nos 11 módulos implementados e serialização das respostas.
- Buscas sem filtros e com filtros individuais ou combinados, nos cenários registrados na suíte.
- Busca por ID nos controllers que oferecem essa operação, incluindo o contrato atual de 204 quando não encontrado.
- Rejeição de marca, categoria, cliente, fornecedor, produto, armazém e tabela inexistentes nas operações testadas.
- Rejeição de parâmetros com tipos inválidos e enums desconhecidos.
- Duplicidade de SKU, CNPJ, nomes de marca/categoria/armazém/tabela e combinação produto/armazém.
- Atualizações parciais válidas, preservando os campos não enviados nos cenários testados, exceto a descrição do armazém.
- Controles de estoque: negativos, reserva superior à quantidade e mínimo superior ao máximo.
- Cadastro de produto com estoque inicial; estoque inválido desfaz também o cadastro do produto.
- Cadastro de fornecedor com endereço; falha de endereço duplicado desfaz também o cadastro do fornecedor.
- PATCH de endereço de fornecedor desconsidera o próprio endereço na busca de duplicidade e permite limpar o complemento.
- Exclusão individual de endereço de cliente e de armazém nos cenários testados.
- Exclusão de cliente e fornecedor removendo os respectivos endereços por cascade.
- Exclusão de registros sem os vínculos impeditivos usados nos testes.

## Escopo e limites

Foram testados os módulos com código implementado: marcas, categorias, produtos, estoque, armazéns, endereços de armazéns, clientes, endereços de clientes, tabelas de preço, fornecedores e endereços de fornecedores.

Produtos, estoque, armazéns e endereços de clientes ainda não oferecem `GET /{id}` nos controllers atuais. A ausência foi registrada como lacuna funcional, sem contar como reprovação de um endpoint existente.

Não foram executados testes de carga, concorrência, autenticação, autorização ou front-end. As demais tabelas da migração que ainda não têm módulo implementado não foram classificadas como funcionalidades disponíveis. A auditoria não prova ausência de outros defeitos.

Nenhuma correção foi aplicada ao código de produção nesta solicitação. Foram adicionados apenas os instrumentos de auditoria e este relatório, preservando as alterações anteriores do projeto.

## Evidências e reprodução

- `backend/reports/backend-audit-results.json`: requisição, corpo, resposta, status esperado e resultado de cada uma das 189 verificações.
- `backend/target/backend-audit-app.log`: inicialização, migrações e exceções da rodada final.
- `backend/target/backend-audit-build.log`: compilação da rodada final.
- `backend/target/surefire-reports/com.pi.erp.ErpApplicationTests.txt`: resultado do teste JUnit existente.
- `backend/tools/backend-audit.cjs`: cenários HTTP, com trava para usar exclusivamente `127.0.0.1:18080`.
- `backend/tools/run-backend-audit.ps1`: compila, prepara o cluster dedicado, recria exclusivamente `backend_audit`, inicia a aplicação de teste, executa os cenários e encerra os processos.

Para repetir neste computador, execute `./backend/tools/run-backend-audit.ps1` a partir da raiz do projeto. O script utiliza os caminhos locais do PostgreSQL 18 e do JDK 25. Ele retorna código diferente de zero quando algum cenário falha; os dados e logs gerados ficam em `backend/target`. O JSON em `backend/reports` é a fotografia desta rodada, não é substituído automaticamente pelo script.

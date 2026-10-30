# Execução — base integrada de Produtos

Plano: 2026-10-29-products-foundation.md. Execução autorizada no checkout existente.

## Restrições e estado inicial

- Git conferido: alterações anteriores em AGENTS.md e frontend, documentos aprovados não rastreados e imagens/componentes locais preservados.
- V1–V6 existentes; próximas versões reservadas nesta execução: V7 e V8.
- Sem testes, build, lint, serviços, SQL, aplicação de migrations, dependências, commits ou delegação. Nenhum cenário operacional executado.
- Ruling: instruções diretas prevalecem sobre os passos de TDD/worktree/commit/revisor delegado da skill executing-plans; revisão direta e este registro substituem esses passos, sem equivaler a validação em execução.

## Tarefas

1. Schema e precisão decimal: implementados e revisados estaticamente; V7/V8 somente preparadas. Backfill por identidade explícita, unidades desconhecidas nulas, opções antigas sem conversão confirmada e interrupção em preferências conflitantes.
2. Catálogo, identidade e situação: implementados e revisados estaticamente; SIMPLE/VARIANT, chave canônica, páginas agrupadas, adaptadores /product, situação efetiva, locks e exclusão protegida.
3. Apresentações, opções e preços: implementados e revisados estaticamente; múltiplas opções por vínculo, preferência única, preço por apresentação/tabela, identidade normalizada e flags de limpeza.
4. Estoque e reservas: implementados e revisados estaticamente; BigDecimal, reserva BASE agregada por estoque, comparação numérica e limites sem truncamento.
5. Vendas: implementadas e revisadas estaticamente; quantidades comerciais/BASE, snapshots, identidade por apresentação/depósito, arredondamento por linha e conferência do total na confirmação sem recalcular preço pelo catálogo atual.
6. Compras e recebimentos: implementados e revisados estaticamente; opção/snapshots, SENT como fronteira contratada, recebimento BASE e repetição numérica.
7. Permissões, serialização e revisão: implementadas e revisadas estaticamente; graphs LOAD com associações singulares para páginas e grafo de recibos preservado/ampliado. Roteiro manual preparado em [2026-10-29-products-manual-checklist.md](2026-10-29-products-manual-checklist.md).

## Decisões de execução e revisão

- Ruling: uma opção nova com campos completos é conversão conhecida; o adaptador de campos comerciais antigos cria opção pendente, sem presumir fator 1. Confirmar a unidade do catálogo não confirma opções legadas nem preenche snapshots históricos.
- Ruling: preservar a mudança de produto/fornecedor do vínculo antigo somente enquanto suas opções não tenham uso contratado. Opções usadas protegem a identidade histórica; nesses casos é necessário criar outro vínculo.
- Ruling: DELETE de SKU sem referências remove apresentações ainda sem uso e, em SIMPLE, o catálogo; referências operacionais continuam protegidas por FKs e transação. VARIANT não pode ficar vazio.
- Revisão detectou a necessidade de estender a ordem catálogo/SKU/estoque aos endpoints administrativos e movimentos manuais; ajustado sem conferir disponibilidade ativa para concluir compromissos contratados.
- Revisão de dependências locais: projeto usa Jackson 3 (`tools.jackson`); conversor de snapshots e chave canônica seguem essa versão, sem dependência adicionada.
- Ajuste necessário fora da matriz original: somente o caminho DELETE em SupplierService passa a delegar a limpeza de vínculos/opções ao ProductSupplierService. Isso mantém a exclusão antiga de vínculos sem uso e protege opções contratadas após a nova FK, sem reescrever o cadastro de fornecedor.
- Leituras anteriores ao lock são revalidadas depois de adquirir catálogo/SKU nos caminhos que permitem mover vínculos/estoque; mudança concorrente de origem pede repetição, sem gravar no SKU errado.
- `frontend/src/services/prices.js` consulta somente /price-table/search; preservado. Frontend, V1–V6, configurações de ambiente, ferramenta de auditoria, Insomnia e testes não foram alterados.

## Cinco pontos do coordenador — evidência por leitura

| Ponto | Trechos revisados | Limite da evidência |
| --- | --- | --- |
| Conversão única e parcial BASE | ProductQuantity, snapshots em OrderItem/PurchaseItem, StockReservationService, StockMovementService e PurchaseReceiptService | Cálculo e contratos inspecionados; saldo real e rollback não executados. |
| Mesmo SKU em apresentações/opções distintas | existsByIdentity e índices V8, conjuntos de identidade na confirmação, agregação BigDecimal por stockItemId | Concorrência e constraints ainda precisam ser observadas no banco isolado. |
| UN inteira e precisão decimal | ProductQuantity.requireBase/requireSignedBase/toBase; DTOs, entidades e operações de estoque/vendas/compras | Busca textual não encontrou quantidades operacionais Integer, Long::sum ou truncamentos intValue/longValue nos pacotes desses fluxos. Isso não é prova de compilação/execução. |
| Situação e compromisso contratado | Product.isActive/getOwnActive, atualizações de catálogo/SKU, confirmação de venda e validateReady(newCommitment) da compra | Transições e locks revisados; cenários funcionais permanecem no roteiro. |
| Legados preservados | V7/V8: cópias antes de drops, product_id/SKU/FKs mantidos, backfill numérico, snapshots ausentes nulos e conflito de preferência explícito | Scripts não aplicados. Dados reais, Flyway e Hibernate validate não conferidos em execução. |

## Comparação com customer/address

Pacotes por funcionalidade, records separados, campos `data`/`filter`/`id`, injeção por @Autowired, Specification.allOf, validações de estado nos services, normalização fora de DTOs, flags Boolean e respostas por entidades foram confrontados com os equivalentes atuais. Catálogo e demais recursos novos usam 404 quando ausentes; /product já mantinha 404. Endpoints anteriores com 204 quando ausentes continuam sem alteração. Transações/locks adicionais, snapshots/conversor e coleções de valores decorrem do domínio integrado, sem copiar regras de clientes.

## Verificações realizadas e pendências

- Realizados: leitura de Git/status/diffs, leitura direta de código/schema/documentos, inspeção dos contratos e consumidores, buscas textuais de tipos/cálculos/caminhos JPA antigos e revisão dos trechos alterados. Formatação aplicada somente aos arquivos Java desta etapa, com as dependências locais já instaladas; sem formatação global ou instalação.
- Não realizados: compilação/build, lint, testes automatizados, HTTP, serviços, SQL no banco, Flyway/migrations ou verificação operacional. Nenhum cenário manual foi marcado como executado.
- Pendentes: autorizar validação futura em ambiente isolado; aplicar/verificar V7/V8 juntas; conferir preservação de dados reais, carregamento serializado com open-in-view=false, paginação efetiva, permissões negativas, locks concorrentes, preferência única, idempotência e rollback. O roteiro manual detalha essas verificações e os contratos públicos alterados.

Base inteira implementada e revisada por leitura; comprovação operacional permanece pendente. Etapa 2 não iniciada. Esta entrega não declara o ERP pronto para produção.

## Etapa 2 — Catálogo operacional e vínculos

Plano: `2026-10-29-products-catalog.md`. Execução expressamente autorizada no checkout existente em 29/10/2026. O registro acima descreve a entrega anterior da base e permanece preservado.

### Restrições e interfaces conferidas

- Git/status e diferenças locais lidos antes das edições; AGENTS.md, identidade JamsHub, componentes compartilhados e trabalho da Etapa 1 preservados. Nenhum worktree, checkout de HEAD, commit ou delegação.
- Desenho, coordenador, plano do catálogo, progresso, plano da base e contratos Java consultados. Produtos usa `ProductCatalog.id` para agrupamento e `Product.id` para apresentações e fornecedores.
- Pre-flight: tarefas 1→2/3 consomem Page de catálogo/itens; 3→4/5 passam SKU concreto; 1→6 separa consultas products.read dos vínculos suppliers.read/update. Contratos correspondentes presentes na base local; isso não comprova execução.
- Ruling: instruções diretas prevalecem sobre TDD/build/lint, worktrees, commits e revisores delegados sugeridos pela skill executing-plans. Registro neste documento e revisão direta são a evidência permitida; não substituem validação operacional.

### Tarefas de implementação

1. Serviços e paginação: escritos em `services/products.js`, `productSuppliers.js`, `supplierCatalogLinks.js`, `Products/useProducts.js` e `useProductResource.js`. Todas as chamadas usam api.js; paginação 25/50/100, debounce 400 ms, cancelamento, geração e estados de erro/carregamento separados. `productFormData.js` valida texto antes da conversão numérica, com vírgula e zeros significativos tratados explicitamente.
2. Lista/detalhes/situação: escritos `ProductsPage`, `ProductDetailsModal`, `ProductVariants`, `ProductItemDetails`, componentes locais de apoio, rota `/products`, navegação e ícone Package sob products.read. Lista conta principais; simples acessa seu SKU interno. Confirmação literal do desenho acrescida da preservação de compromissos; situação própria/herdada distinta; nenhum DELETE de produto/variação.
3. Formulários: escritos `ProductForm`, `ProductVariantForm`, `ProductItemFields`, `productFormData` e `useProductForm`. Dados comuns no principal, apenas combinações existentes, SKU de leitura, custos/preços por SKU, PATCH parcial/flags, opcionais anteriores, rascunhos por usuário/registro, descarte, trava de envio e estoque inicial opcional BASE.
4. Apresentações: escritos `ProductPresentations` e `ProductPresentationForm`. Base controlada pelo servidor, equivalência positiva, preço próprio opcional/zero/removePrice, prévia de preço padrão e aviso de precedência da tabela do cliente. Sem saldo de embalagem ou alteração de contratos passados.
5. Fornecedores/opções: escritos `ProductSuppliers` e `ProductSupplierOptionForm`. Busca paginada de fornecedores; vínculo opcional sem duplicar par; preferência no vínculo; opções paginadas com custo/código/prazo/situação próprios e confirmação legada independente. Leituras suppliers.read e escrita suppliers.update; sem excluir vínculos/opções nem atualizar preços/custo de referência.
6. Marcas/categorias/vínculos: escritos `ProductClassificationsModal` e `Suppliers/SupplierCatalogLinksModal`, com integração pontual nos detalhes/lista de Fornecedores. Consulta/cadastro/exclusão de classificação conforme products.read/create/delete; vínculos independentes conforme suppliers.read/update. Sem products.read, seletor de novos vínculos explica a indisponibilidade; vínculos existentes podem ser consultados/removidos conforme permissão.
7. Revisão e roteiro: revisão estática própria concluída. Roteiro da Etapa 2 acrescentado ao checklist, com cenários de UI, permissões, respostas atrasadas e integração futura por contratos; todos permanecem não executados.

### Decisões de execução

- Ruling: edição SIMPLE envia dados comuns e comerciais juntos para PATCH `/product/{skuId}`, já transacional na base; evita dois saves independentes entre principal e filho. VARIANT usa PATCH de catálogo para campos comuns e PATCH do SKU para atributos/comerciais.
- Ruling: unidade já definida é somente leitura nesta interface; legado nulo pode ser confirmado expressamente. Não há troca de unidade, tipo, características ou reagrupamento na edição comum. Conversão de números de caixas para unidades continua pendência de desenho próprio.
- Ruling: estoque inicial usa seletores de depósitos existentes e exige stock.configure e warehouses.read na interface; o servidor mantém sua própria exigência stock.configure. Sem leitura de depósitos, cadastro continua sem estoque inicial. Nenhuma tela de Estoque foi antecipada.
- Ruling: peso mantém três casas existentes no schema e dimensões mantêm duas; esses campos não recebem a restrição de uma casa das quantidades BASE. Não criar regra de domínio nova a partir do padrão de clientes.
- Revisão parcial detectou submissão implícita pelos botões da paginação dentro do seletor de fornecedor: seletor ficou fora de um form HTML, e apenas o botão Vincular chama a gravação. Escolher página não é intenção de vincular.
- Revisão parcial corrigiu a escolha repetida de tamanho/página para não invalidar a lista sem disparar nova consulta. Detalhes de variação consultam GET `/product/{id}` ao abrir, em vez de usar apenas o objeto da linha.

### Limite da evidência desta etapa

Até este ponto: somente leitura estática, escrita de código e revisão direta. Não foram criados/executados testes, build, lint, servidor, navegador, HTTP, SQL, migrations/Flyway, dependências, commits, Insomnia ou ferramenta de auditoria. V7/V8 continuam somente preparadas. Pendências operacionais da base permanecem válidas.

### Revisão final própria da Etapa 2

- Final review: self-review por instrução expressa de não delegar. Código novo e diferenças próprias em rotas/menu/Fornecedores confrontados com os componentes atuais, AGENTS.md, desenho, plano e contratos Java. Não houve revisor independente ou validação funcional.
- Ajustes da revisão: detalhes e edição de variações buscam SKU atual por GET; formulários de edição sem alterações não geram PATCH; feedbacks de salvamento seguem o padrão de sucesso; menu de tamanho de página não é cortado pelo painel da lista; vínculos do fornecedor bloqueiam fechamento/troca de aba durante gravação.
- Rascunho de estoque inicial após perda de permissão não é omitido silenciosamente: a tela explica a pendência e permite desmarcar a inclusão antes de cadastrar somente o produto.
- Preferência é gravação comum, sem confirmação extra; inativação/ativação e exclusão/remoção de vínculos continuam usando useConfirm. Formulários comuns não receberam confirmação adicional de salvamento.
- Prévia monetária derivada usa centavos e décimos inteiros (BigInt), com HALF_UP, para evitar erro binário na exibição. É somente prévia do catálogo; nenhum saldo/conversão operacional ou payload de venda é calculado a partir dela. A API continua fonte de verdade e preços de tabela não são consultados nem alterados aqui.
- `ProductForm` SIMPLE usa o PATCH existente e atômico de Product; VARIANT não envia unidade/marca/categoria ao filho. Flags conferidas: removeCategory, removeWeightKg, removeDimensionX/Y/Z, removePrice e removeAvgDeliveryDays. Conversão legada envia confirmConversion/baseFactor/basePresentation/cost explicitamente, independentemente da unidade do catálogo.
- Permissões conferidas por leitura de SecurityConfig e controles da UI: `/products` sob products.read; POST de catálogo/variação/apresentação products.create; PATCH products.update; consulta/cadastro/exclusão de classificações products.read/create/delete. Vínculos/opções comerciais suppliers.read/update; vínculos marca/categoria suppliers.read/update e cadastros disponíveis products.read; estoque inicial stock.configure com consulta de depósitos warehouses.read. SecurityConfig não foi alterado nesta etapa.
- Identidade/estrutura preservadas: somente adição de ícone no mapa atual de AppLayout, rota/menu novos e integração pontual de callback/modal nos fornecedores. Nenhuma alteração no cadastro PJ/endereço, Clientes, autenticação ou CSS global. Estilos próprios em Products/products.css reaproveitam as tabelas, ações, confirmação, foco/rolagem e feedback existentes.
- Formatação localizada com Prettier já instalado, apenas nos arquivos novos e no ponto de extensão de estilos de Produtos. Isso não é build/lint nem comprovação em execução. Nenhuma dependência instalada.
- Buscas textuais e leitura direta conferiram ausência de fetch autenticado paralelo, window.confirm/alert, parseInt de quantidade e DELETE de produto/variação/opção/vínculo comercial nos serviços novos. Links de classificação usam DELETE somente para a associação ou a entidade permitida, com intenções distintas.

Etapa 2 implementada e revisada estaticamente nas sete tarefas. Validação visual no navegador, execução funcional, integração real com o backend, paginação SQL/serialização, permissões negativas, concorrência e persistência permanecem pendentes, junto das verificações da base. Nenhum cenário do roteiro foi marcado como executado; não há declaração de prontidão de produção.

### Ajuste solicitado — colunas operacionais na lista

- Colunas SKU, preço e estoque acrescentadas conforme referência visual enviada pelo usuário. Tipo aparece como informação secundária do nome, mantendo a linha principal por catálogo.
- SIMPLE consulta seu único SKU pela API paginada existente; VARIANT oferece acesso aos SKUs/preços/saldos nos detalhes, sem inventar um SKU, preço único ou estoque operacional para o agrupador. A lista de variações também mostra estoque por SKU.
- Com stock.read, consulta `/stockItem/search?productId=...` apenas dos SKUs exibidos; resumo mostra saldo, reservado e disponível em BASE, com soma em décimos inteiros somente para exibição. Abertura de detalhes usa Modal existente com dados por depósito, mínimo e máximo. Sem stock.read, a coluna e consultas de estoque não são montadas.
- Consultas de SKU e estoque são canceláveis; falha não vira zero. Estados de carregamento/erro e repetição de consulta preservados. Nenhum endpoint, permissão, saldo, reserva ou contrato de backend alterado.
- Revisão estática dos componentes/serviço/integração; sem testes, build, lint, servidor, HTTP, SQL ou validação no navegador. Não é uma tela de configuração/movimentação de estoque.

### Validação autorizada e correção do cadastro — 29/10/2026

- Após autorização expressa de testes, o erro 500 relatado em SIMPLE e VARIANT foi reproduzido em PostgreSQL isolado (`127.0.0.1:55434/products_verify`), com V1–V8 e validação Hibernate. Banco normal preservado.
- Causa confirmada: Spring Data interpretava o acessor calculado `ProductPresentation.productId` como atributo persistido ao gerar a consulta da apresentação base, produzindo UnknownPathException. Corrigido com JPQL explícito por `product.id`. Aplicado o mesmo tratamento aos acessores calculados catalogId em ProductRepository e productSupplierId no repositório de opções, preservando assinaturas e contratos públicos.
- Evidência antes da correção: três testes de cadastro retornaram 500 com a mesma consulta inválida; relatório local em `backend/target/products-before-fix.log`. Depois: `ProductCatalogIntegrationTests` executou 5 testes, 0 falhas/erros, cobrindo cadastro SIMPLE/VARIANT, SKU e busca, edição comum e de atributos, combinação duplicada, estoque inicial KG, apresentação com preço zero/remoção e rollback de entradas inválidas.
- O teste de estoque revelou que o reservado inicial precisa ser zero por regra atual de StockItemService; reservas são geradas pelos pedidos. A regra do backend foi preservada e a validação/mensagem do formulário foi alinhada. Frontend passou 12/12 testes, incluindo 3 novos cenários de payload/precisão/reserva; build Vite concluído.
- Limite da evidência: endpoints exercitados via MockMvc com segurança, sessão/CSRF, serviços e PostgreSQL reais. Tomcat de teste por socket encontrou falha local de loopback do JDK/Windows; não foi apresentado como HTTP por rede nem como teste visual. Navegador e todos os demais cenários do roteiro permanecem sem validação.
- Execução repetível: `backend/target/run-products-verification.ps1`, com guarda do banco isolado antes do contexto Spring, configuração de conexão somente na execução e encerramento do PostgreSQL em finally. Relatório final em `backend/target/products-integration.log`. Nenhuma migration alterada, nenhum serviço normal encerrado e nenhuma ferramenta de auditoria anterior executada.

# Produtos — catálogo operacional e vínculos

> **Para execução por agente:** usar superpowers:executing-plans sequencialmente em outro chat expressamente autorizado. Seguir [plano coordenador](2026-10-29-products.md), [base integrada](2026-10-29-products-foundation.md) e AGENTS.md vigente. Sem testes/build/lint, serviços, SQL/migrations, commits ou delegação automática.

**Objetivo:** entregar a experiência de Produtos no JamsHub, com lista agrupada, cadastro simples/com variações, apresentações de venda, opções de fornecedores e vínculos de marcas/categorias.

**Arquitetura:** consumir os contratos da base, preservando ProductCatalog como cadastro comum e Product como SKU. Reutilizar serviços autenticados e componentes atuais de Clientes/Fornecedores; componentes específicos permanecem juntos em Products. Não implementar telas completas de Estoque, Compras, Vendas ou Tabelas de preços.

**Tecnologias:** React, lucide-react, classes/Tailwind existentes e api.js com sessão/CSRF. Sem dependências novas. **Desenho:** [Produtos aprovado](../specs/2026-10-29-products-design.md). Estado: plano aprovado pelo usuário em 29/10/2026, execução não iniciada. Execução somente em outro chat expressamente autorizado.

## Restrições globais e pontos de revisão

Aplicar todas as restrições e cinco pontos de revisão do coordenador. Base integrada completa é pré-requisito, não uma suposição: conferir endpoints, corpos, respostas, permissões e progresso antes de começar. Não modificar contratos do backend para contornar um erro visual; corrigir na tarefa dona do contrato.

Lista principal conta cadastros principais, não SKUs. Produto simples não mostra formulário de variação artificial. UN aceita inteiros; demais unidades até uma casa. Preço/custo zero é valor informado. Ocultar um botão não substitui proteção na API.

## Estrutura de arquivos

| Ação | Caminho em frontend/src | Responsabilidade |
| --- | --- | --- |
| Criar | services/products.js | Catálogo, itens, apresentações, marcas/categorias |
| Criar | services/productSuppliers.js | Vínculos e opções de fornecedores, sem embutir em products |
| Criar | services/supplierCatalogLinks.js | Vínculos fornecedor/marca e fornecedor/categoria |
| Criar | pages/Products/useProducts.js | Busca paginada, filtros e cancelamento |
| Criar | pages/Products/ProductsPage.jsx | Lista principal, seleção e ações |
| Criar | pages/Products/ProductDetailsModal.jsx | Dados comuns, metadados e navegação de itens |
| Criar | pages/Products/ProductForm.jsx e productFormData.js | Cadastro/edição do principal e simples, payload parcial |
| Criar | pages/Products/ProductVariants.jsx e ProductVariantForm.jsx | Consulta paginada e manutenção dos SKUs de variações |
| Criar | pages/Products/ProductPresentations.jsx e ProductPresentationForm.jsx | Apresentações de venda por SKU |
| Criar | pages/Products/ProductSuppliers.jsx e ProductSupplierOptionForm.jsx | Fornecedores e opções por SKU |
| Criar | pages/Products/ProductClassificationsModal.jsx | Cadastro/consulta de marcas e categorias |
| Criar | pages/Suppliers/SupplierCatalogLinksModal.jsx | Vínculos de marcas/categorias no fornecedor |
| Alterar | pages/Products/products.css | Somente estilos específicos necessários; reaproveitar index.css |
| Alterar | routes/AppRoutes.jsx, routes/navigation.js e components/layout/AppLayout.jsx | Rota, menu e ícone de Produtos |
| Alterar | pages/Suppliers/SupplierDetailsModal.jsx e SuppliersPage.jsx | Abrir gerenciamento de vínculos, sem refazer cadastro/endereço |

Referências obrigatórias: services/api.js, services/customers.js, services/suppliers.js; pages/Customers e pages/Suppliers atuais; auth/drafts.js; components/layout/PageHeading.jsx; feedback/Modal.jsx, FilterSelect.jsx, RowActions.jsx, Pagination.jsx, ErrorAlert.jsx, useConfirm.js; record/RecordMetadata.jsx. Não copiar versões antigas dos componentes sobre alterações locais.

## Interfaces frontend entre tarefas

- products.search(filter, page=0, size=25, options): Promise<Page<ProductCatalog>>; findOne(id,options); register(body); update(id,body); items(catalogId,filter,page,size,options): Promise<Page<Product>>; registerItem(catalogId,body); updateItem(id,body).
- products.presentations(productId,filter,page,size,options), registerPresentation(body), updatePresentation(id,body); brands(filter,options), categories(filter,options), registerBrand(body), registerCategory(body), deleteBrand(id), deleteCategory(id). Não criar deleteProduct nem deleteCatalog para a tela.
- productSuppliers.search(filter,options), register(body), update(id,body), options(productSupplierId,filter,page,size,options), registerOption(body), updateOption(id,body). search usa o formato de vínculos preservado pela base; opções são Page.
- supplierCatalogLinks.brands(supplierId,options), categories(supplierId,options), registerBrand(supplierId,brandId), registerCategory(supplierId,categoryId), deleteBrand(supplierId,brandId), deleteCategory(supplierId,categoryId), usando endpoints existentes /supplier/{id}/brands e /categories.
- useProducts() segue useSuppliers: data/loading/error, filters/page/size, changeFilters/changePage/changeSize/reload. Filtros query, brandId, categoryId, active; busca por nome/SKU/características resolvida na API.
- ProductForm({ catalog, item, onSaved, onClose }); ProductVariantForm({ catalog, item, onSaved, onClose }); componentes de apresentações/fornecedores recebem productId concreto, nunca catalogId como substituto de SKU. ProductDetailsModal recebe catalogId e callbacks, não objeto de linha potencialmente desatualizado.

## Tarefa 1 — Serviços e fluxo paginado

**Arquivos:** services/products.js, services/productSuppliers.js, services/supplierCatalogLinks.js e pages/Products/useProducts.js.

**Consome:** contratos/entidades/permissões da base; **produz:** interfaces acima.

- [ ] Implementar chamadas exclusivamente por api.js, com signal para leituras e queryString já existente; não criar fetch autenticado paralelo nem reexportar preço/custo de fornecedor pelo serviço genérico de produtos.
- [ ] Adotar paginação real 25/50/100 e debounce400ms, invalidação imediata ao alterar filtros/página/tamanho, AbortController e contador de geração, como useSuppliers. Ajustar página quando totalPages diminuir.
- [ ] Separar estados loading/error/data; erro não equivale a array vazio e contagem indisponível não aparece como zero. Atualizar conteúdo e totalElements da mesma resposta.
- [ ] Fornecer rótulos fixos para UN/KG/G/L/ML/M e COLOR/SIZE/VOLTAGE/POWER/CAPACITY/MODEL em productFormData.js; sem CRUD de tipos.
- [ ] Formatar exibição pt-BR com até uma casa em quantidade e duas em moeda. Validar entrada textual antes de converter: rejeitar precisão excessiva, valores negativos não permitidos e texto inválido; não usar parseInt nem arredondar para fazer a validação passar. Permitir vírgula decimal local e enviar JSON decimal numérico aceito pelo backend.

**Revisão:** troca rápida de filtro não deixa resposta antiga prevalecer; página atual não é contagem geral; 1,25 não vira1,2; UN não aceita1,5; nenhuma conversão de estoque é recalculada no navegador como fonte de verdade.

## Tarefa 2 — Lista, detalhes e confirmações

**Arquivos:** ProductsPage.jsx, ProductDetailsModal.jsx, useProducts.js, products.css; routes/AppRoutes.jsx, routes/navigation.js e AppLayout.jsx.

**Consome:** produtos e useProducts; **produz:** rota /products com products.read.

- [ ] Inserir Produtos no menu com ícone Package da lucide-react, seguindo o mapa de ícones atual sem substituir identidade/logo/favicon. Preservar primeira rota permitida e comportamento responsivo.
- [ ] Renderizar PageHeading com totalElements/singular/plural e Novo produto conforme products.create. Painel de busca query, marca, categoria e situação; tabela compacta com nome, marca, categoria, simples/com variações, situação e RowActions.
- [ ] Cada principal aparece uma vez. Detalhes consultam dados atuais, exibem dados comuns e metadados reais; produtos VARIANT mostram lista paginada de variações/SKUs que pode filtrar pela busca original. A resposta não precisa carregar todos os filhos para abrir a lista.
- [ ] SIMPLE abre seu único item diretamente para preço/custo/apresentações/fornecedores; não apresentar uma variação denominada Padrão para o usuário editar. O ID usado nesses componentes é o Product.id concreto.
- [ ] Usar useConfirm para inativar principal com a mensagem literal do desenho sobre todas as variações, saldos e histórico. A confirmação também informa que compromissos já confirmados continuam podendo ser concluídos. Ativar exige confirmação específica, sem prometer reativar variações próprias inativas.
- [ ] Ações usam products.update e enviam apenas active no alvo certo. Mostrar estado próprio e indisponibilidade herdada nas variações; principal inativo impede ativação efetiva de filho, com explicação. Cancelar/Esc não envia; impedir confirmação duplicada.
- [ ] Após salvar/alterar situação, recarregar principal e lista de itens afetada. Erro mantém modal e dados; sem botão de exclusão física de produto/variação. Ocultar controles de estoque/fornecedores sem a permissão correspondente.

**Revisão:** mesmo item de cores diferentes não duplica principal; buscar SKU leva ao agrupador e destaca a variação correspondente; inativa anterior permanece inativa após ciclo do principal. Root.updatedAt não é exibido como data de atualização de preço/estoque dos filhos.

## Tarefa 3 — Cadastro simples/com variações, edição e legado

**Arquivos:** ProductForm.jsx, productFormData.js, ProductVariants.jsx e ProductVariantForm.jsx.

**Consome:** RequestProductCatalogDTO/RequestCatalogItemDTO e Product/ownActive; **produz:** corpos POST/PATCH parciais compatíveis.

- [ ] Cadastro pede nome, marca, unidade fixa, categoria opcional e tipo simples/com variações. Simples contém um conjunto de dados comerciais; variações selecionam características fixas e ao menos uma combinação com custo/preço próprios. SKU é somente leitura após cadastro, nunca obrigatório no formulário.
- [ ] Para VARIANT, incluir apenas as combinações realmente cadastradas, com exatamente os atributos escolhidos. Rejeitar combinação repetida conforme normalização do backend; nenhuma geração automática de todas as cores/tamanhos.
- [ ] Campos comerciais: custo de referência/preço base obrigatórios não negativos, zero válido; opcionais atuais mantidos. Para variação, COLOR/SIZE substituem os campos legados correspondentes como fonte de edição; para SIMPLE legado, preservar color/size sem converter automaticamente em variação. Unidade/marca/categoria não são editadas por filho.
- [ ] Formulário de edição cria PATCH apenas das mudanças. Omitido/null mantém, texto opcional vazio limpa; remover categoria usa removeCategory Boolean; limpar opcional numérico usa flag explícita definida no contrato. Sem mudanças fecha sem PATCH.
- [ ] Legado sem unidade mostra pendência explícita e exige confirmação da unidade representada pelos saldos anteriores antes de novas operações. Não sugerir unidade por nome/peso e não converter números ao salvar. Opções legadas de fornecedor possuem conferência própria, não são confirmadas automaticamente pela unidade do produto.
- [ ] Bloquear troca de tipo e conjunto de características após criação, reagrupamento e alteração de unidade usada. Explicar motivo com mensagem específica, mantendo edição dos campos permitidos. Não adicionar rotina de migração/reinterpretação de histórico na tela.
- [ ] Cadastro não exige fornecedor nem estoque. Preservar estoque inicial opcional com stock.configure por SKU, valores BASE e sem reserva manual de pedido; se houver campo reservedQuantity no contrato existente, manter seu significado/configuração atual, sem criar reserva comercial fictícia.
- [ ] Reutilizar mecanismo de rascunhos em memória por usuário/registro, limpeza ao sair da sessão e confirmação de descarte de Clientes/Fornecedores. Cancelar descarte preserva valores; erro preserva formulário; bloqueio de envio duplicado e controles acessíveis.

**Revisão:** agrupador sem custo/preço próprios; SIMPLE não exige criar filho manualmente; combinação Azul/M tem outro estoque de Azul/G; unidade fixa comum; flags explícitas distinguem manter/limpar e zero não é ausência.

## Tarefa 4 — Apresentações de venda

**Arquivos:** ProductPresentations.jsx, ProductPresentationForm.jsx e services/products.js.

- [ ] Consultar páginas de apresentações do Product.id selecionado. Mostrar nome, equivalência com unidade base, preço próprio ou cálculo pelo preço base do catálogo, situação e ações permitidas por products.create/update.
- [ ] Base é fixa em fator1, sem estoque próprio nem edição que transforme UN em embalagem. Embalagens compostas aceitam fator positivo compatível com a precisão da unidade; custo/preço não determinam equivalência.
- [ ] Preço próprio opcional pode ser zero; ao limpar enviar removePrice=true e exibir que o valor volta ao cálculo automático. Não copiar esse preço para tabelas de clientes.
- [ ] Para fator já usado em operação, orientar inativação e nova apresentação; não editar retroativamente. Inativar preserva histórico e não interfere em preço/fator contratado em linhas existentes.
- [ ] A prévia da tela representa preço padrão do catálogo. Informar de forma curta que a tabela do cliente pode definir outro preço na venda; não afirmar preço universal nem construir simulador de Vendas nesta etapa.

**Revisão:** pacote6/caixa12 usam um estoque, preço próprio pode diferir da soma, e não há botão para abrir caixa ou saldo de embalagem lacrada.

## Tarefa 5 — Fornecedores e várias opções de compra

**Arquivos:** ProductSuppliers.jsx, ProductSupplierOptionForm.jsx e services/productSuppliers.js.

- [ ] Carregar somente com suppliers.read e mostrar fornecedor, situação do vínculo, preferência e opções paginadas. Seleção de fornecedor usa busca paginada existente, não busca ilimitada de todos os fornecedores.
- [ ] Com suppliers.update, incluir vínculo sem duplicar par produto/fornecedor, cadastrar/editar/inativar opções e alterar preferência. Exibir uma indicação de preferência por fornecedor, sem um preferencial por embalagem.
- [ ] Opção mostra nome, quantidade BASE por embalagem, custo comercial, código no fornecedor, prazo específico e situação. Mesmo fornecedor pode ter pacote6 ecaixa24. Preço de venda e custo de referência não são atualizados ao salvar opção.
- [ ] Prazo omitido significa usar prazo geral do fornecedor; zero é prazo informado; limpar prazo usa removeAvgDeliveryDays. Exibir não informado se ambos ausentes, sem inventar previsão garantida.
- [ ] Opção legada pendente exige conferência expressa de unidade/fator/custo antes de nova compra. UI não altera o significado dos recebimentos anteriores nem escolhe uma opção pela posição da lista.
- [ ] Preferência é opcional; marcar outro troca atomicamente pela API e recarrega os vínculos. Informar opção inativa e indisponibilidade herdada do vínculo, fornecedor ou produto; não ativar essas outras identidades como efeito de salvar opção.
- [ ] Sem exclusão física de opção/vínculo na tela; manter histórico e contratos antigos. Ao abrir componente por outro SKU, cancelar consulta antiga e impedir que resposta/opção anterior seja gravada no novo produto.

**Revisão:** dois fornecedores/duas embalagens cada permanecem distintos; permissões de Products não bastam para ler custos de compra; mudança de preferência mantém no máximo um e não muda custo ou apresentação contratados.

## Tarefa 6 — Marcas, categorias e vínculos do fornecedor

**Arquivos:** ProductClassificationsModal.jsx, services/products.js, services/supplierCatalogLinks.js, pages/Suppliers/SupplierCatalogLinksModal.jsx, SupplierDetailsModal.jsx e SuppliersPage.jsx.

- [ ] Disponibilizar consulta/cadastro de marca/categoria no contexto de Produtos usando APIs atuais; permitir atualizar seletores após incluir sem perder o formulário. products.read consulta, products.create cadastra, products.delete exclui quando sem referências; não criar renomeação/ativação que o desenho não aprovou.
- [ ] Exclusão de marca/categoria pede confirmação específica e trata conflito de vínculo sem apagar produtos/fornecedores em cascata. Reutilizar unicidade de nome normalizado existente, preservando mensagens específicas.
- [ ] Nos detalhes do fornecedor, abrir modal de vínculos com marcas/categorias; suppliers.read consulta e suppliers.update inclui/remove associação. Remover vínculo não exclui marca/categoria nem produtos do fornecedor.
- [ ] A pesquisa dos cadastros disponíveis de marcas/categorias continua exigindo products.read. Se houver suppliers.update sem essa leitura, manter a proteção e explicar a indisponibilidade do seletor; não ampliar SecurityConfig para contornar a combinação de permissões. Associações já visíveis no contexto do fornecedor mantêm suppliers.read.
- [ ] Vínculos são explícitos e independentes: adicionar produto não cria automaticamente marca/categoria do fornecedor; remover categoria do produto não remove o vínculo fornecedor/categoria.
- [ ] Reutilizar Modal/FilterSelect/RowActions/useConfirm e estados de erro/carregamento. Não reescrever formulário PJ/endereço ou ferramenta backend/tools/backend-audit.cjs.

**Revisão:** identidade de cadastro e vínculo não se confundem; usuário sem products.delete pode ter suppliers.update para remover uma associação, mantendo as entidades originais.

## Tarefa 7 — Revisão estática e roteiro manual preparado

**Arquivos:** todos os alterados nesta etapa; docs/superpowers/plans/2026-10-29-products-progress.md e 2026-10-29-products-manual-checklist.md.

- [ ] Comparar implementação com componentes atuais e AGENTS.md: identidade JamsHub, paleta, menu, PageHeading, filtros, linhas/ações responsivas, confirmações, foco/rolagem e RecordMetadata. Revisar só diff próprio, sem formatação global.
- [ ] Preparar cenários manuais: SIMPLE; VARIANT com duas características; busca por SKU; paginação; fornecedor opcional; múltiplas embalagens/fornecedor; preço zero/removido; unidade fracionada/inválida; legado pendente; descarte/erro; inativação com filhos anteriores inativos; permissões negativas e respostas atrasadas.
- [ ] Acrescentar roteiro integrado de compra/recebimento/reserva/despacho usando contratos da base, identificando que telas completas desses fluxos ainda serão entregues. Roteiro não autoriza HTTP/SQL/teste automático neste chat nem transforma leitura de código em aprovação de execução.
- [ ] Registrar arquivos, contratos e partes concluídas por revisão versus verificadas em execução; comunicar claramente qualquer verificação não realizada. Não marcar o ERP pronto para produção.

**Saída:** catálogo e vínculos utilizáveis por permissões existentes, apoiados na base integrada, com histórico preservado e documentação pronta para orientar validação autorizada futura.

# Migração de modais para páginas — plano de implementação

> **For agentic workers:** usar `superpowers:executing-plans` para implementar tarefa por tarefa. Não delegar nem criar commits automaticamente. As preferências de AGENTS.md prevalecem sobre sugestões de testes, build e lint das skills.

**Goal:** reduzir a alternância entre janelas nos fluxos extensos de Compras, Produtos, Histórico de estoque, Fornecedores e Clientes, preservando as operações atuais.

**Architecture:** páginas filhas dos módulos existentes, dentro de AppLayout, com rotas próprias. Separar o conteúdo dos formulários e consultas de seus invólucros Modal, mantendo invólucros apenas nos pontos que ainda exigem ações rápidas. Entregar um módulo funcional por vez, usando APIs atuais e Vendas como referência de navegação e composição, sem copiar suas regras comerciais.

**Tech Stack:** React, React Router com BrowserRouter, CSS/Tailwind e lucide-react existentes. Sem novas dependências.

**Spec:** escopo e decisões registrados neste documento, com base na análise visual aprovada no chat. Referência de Vendas: `docs/superpowers/specs/2026-10-31-order-entry-design.md`. Este é o plano geral solicitado; a implementação ainda não foi iniciada.

## Restrições globais

- Não criar ou executar testes automatizados, build ou lint sem solicitação explícita. Fazer revisão direta e conferências manuais no navegador.
- Conferir Git antes de cada entrega e preservar alterações existentes. Não fazer formatação global.
- Não alterar backend, contratos de API, regras comerciais ou política de permissões como efeito colateral.
- Reutilizar AppLayout, PageHeading, FilterSelect, RowActions, Modal, ConfirmationProvider/useConfirm e os estilos atuais.
- Novas páginas não acrescentam uma entrada lateral por registro: Produtos, Compras, Fornecedores e Clientes continuam sendo os módulos do menu.
- Depósitos, Cargos, Funcionários, Convites, política de acesso, sessões e conta permanecem no formato atual. A consulta de Vendas e seus fluxos posteriores também ficam fora desta migração.
- Manter modais de confirmação e tarefas curtas: classificação, seleção auxiliar, movimento de estoque, limites e formulário pontual de endereço. A ficha que os contém passa a ser página, evitando pilhas extensas.
- Manter popover de estoque na listagem. A consulta rápida não deve obrigar navegação para outra página.
- Preservar o funcionamento dos consumidores antigos até migrar suas chamadas; remover apenas os invólucros que efetivamente ficarem sem uso.
- Não prometer restauração após reload com `useDraft`: os rascunhos atuais ficam na memória da aba e são limpos em logout/troca de usuário. Não introduzir persistência de dados cadastrais em localStorage.
- Todas as novas rotas aceitam URL direta, validam ID inteiro positivo seguro e tratam carregamento, ausência do recurso, erro e permissão insuficiente.

## Decisões de apresentação

1. **Páginas para trabalho extenso; modais para ações pontuais.** Manter todos os modais conservaria os problemas observados. Converter todos geraria navegação excessiva para tarefas curtas.
2. **Compras:** catálogo e pedido lado a lado quando houver espaço, adaptados ao fornecedor; acompanhamento na página da compra, com seções para itens, recebimentos e histórico.
3. **Produtos:** ficha com seções para dados gerais, dados comerciais, variações, apresentações, fornecedores/opções e estoque. Não copiar o carrinho de Vendas para um cadastro.
4. **Histórico:** página geral acessada por Produtos e pelo modo de estoque; consultas contextuais curtas continuam disponíveis nas fichas/documentos.
5. **Fornecedores e Clientes:** ficha e edição como páginas; endereços e vínculos visíveis na ficha, com edição pontual pelo componente atual adaptado.

## Focos de revisão

1. Requisição com resultado desconhecido: não repetir automaticamente cadastro, inclusão ou transição; recebimento conserva seu requestId e a reconciliação atual — tarefas 2 e 3.
2. Acesso direto com permissões reduzidas: não expor preços/custos a quem só possui stock.read, nem executar consultas de outros módulos sem autorização — tarefas 1, 4 e 5.
3. Navegação, sessão expirada e formulários sujos: conservar rascunho na memória da aba e contexto de retorno; não declarar proteção de reload inexistente — tarefas 1, 2, 4, 6 e 7.
4. Estado alterado em outra sessão ou respostas fora de ordem: recarregar dados atuais, preservar campos digitados e deixar o backend decidir transições/totais — tarefas 2, 3, 4, 6 e 7.
5. Pouca altura/largura: controles acessíveis, layout empilhado quando necessário, sem ações cobertas por painel fixo ou modal secundário — todas as entregas.

## Rotas propostas

| Rota | Função | Acesso mínimo |
|---|---|---|
| /purchases/new | Iniciar compra | purchases.read + purchases.create |
| /purchases/:id | Consultar e acompanhar | purchases.read; ações conforme permissões/status |
| /purchases/:id/edit | Montar rascunho | purchases.read + purchases.update; status validado |
| /products/new | Cadastro | products.read + products.create |
| /products/:id | Ficha do catálogo | products.read |
| /products/:id/edit | Edição | products.read + products.update |
| /products/:id/variants/:itemId | Ficha do SKU/variação | products.read; validar pertencimento ao catálogo |
| /stock/history | Histórico geral/contextual | stock.read |
| /suppliers/new, /suppliers/:id, /suppliers/:id/edit | Cadastro, ficha e edição | suppliers.read; create/update conforme operação |
| /customers/new, /customers/:id, /customers/:id/edit | Cadastro, ficha e edição | customers.read; create/update conforme operação |

Seção ativa da ficha em query string com valores enumerados. Histórico aceita somente filtros reconhecidos pela API. IDs de catálogo, SKU e saldo são diferentes: não usar catalogId como productId do histórico, que identifica o SKU.

## Tarefa 1 — Navegação e contexto de retorno, integrada à primeira entrega

**Arquivos:** modificar `frontend/src/routes/AppRoutes.jsx`, `frontend/src/routes/navigation.js` e a listagem do módulo entregue; criar `frontend/src/hooks/useListReturn.js`.

**Interface proposta:** `useListReturn({ modulePath, listState })` fornece `entryState` para navigate e `returnToList()`; o estado inclui apenas filtros, página, tamanho e scroll, isolado por usuário/módulo. Cada listagem continua responsável por validar seus filtros e limites.

- [ ] Registrar apenas as rotas do módulo que estiver pronto. Manter os guards de consulta e conferir permissões de escrita na página e antes do submit.
- [ ] Estender safeReturn com caminhos explicitamente reconhecidos; não aceitar URLs externas, caminhos arbitrários ou prefixos amplos. Validar query strings por rota, sem usar um returnTo livre.
- [ ] Capturar contexto da listagem antes da navegação. Restaurar filtros/página antes de carregar a lista e scroll depois dos dados; ajustar página se ela deixar de existir. Entrada direta volta à listagem padrão do módulo.
- [ ] Manter rascunhos dos campos não secretos em useDraft. Botões próprios de sair/cancelar usam o descarte existente. Navegação por menu/Voltar conserva o rascunho em memória; fechamento/reload de formulário sujo exibe aviso nativo beforeunload, sem prometer recuperação após recarga.
- [ ] Não migrar BrowserRouter nem adicionar bloqueio global de rotas nesta entrega. Não colocar dados digitados em query string/history.state; ali só vai contexto de navegação.
- [ ] Conferir manualmente abrir por URL, voltar com filtros, voltar após login e tentar recurso inexistente. Usar conta de permissões reduzidas fornecida pelo usuário para validar restrições; não alterar cargos para criar esse cenário.

**Entrega verificável:** primeira página do módulo abre e retorna à lista com contexto, sem afetar os demais módulos.

## Tarefa 2 — Registrar e editar Compras

**Arquivos:** criar em `frontend/src/pages/Operations/`: `PurchaseEntryPage.jsx`, `PurchaseEntryHeader.jsx`, `PurchaseProductCatalog.jsx`, `PurchaseCart.jsx`, `usePurchaseEntry.js`, `purchaseEntry.css`. Modificar `PurchasesPage.jsx`, `TradeItemForm.jsx`, rotas e navegação.

**Interfaces propostas:** `usePurchaseEntry(id)` fornece dados da compra/itens, loading/error/busy e operações `reload`, `saveHeader(body)`, `addItem(body)`, `updateItem(itemId, body)`, `removeItem(itemId)`. `PurchaseProductCatalog({ supplierId, disabled, onSelect })` emite SKU, opção de compra, quantidade e depósito explicitamente escolhidos. `PurchaseCart({ purchase, items, busy, onEdit, onRemove })` apresenta valores persistidos. Reutilizar TradeItemForm com `purchase`, `embedded`, `initialSelection` e `writeItem`; adaptar a escolha inicial à supplierOption, sem tratá-la como apresentação de venda.

- [ ] Extrair PurchaseForm em conteúdo reutilizável e invólucro; manter fornecedor e previsão de entrega, suas validações e descarte. Criar rascunho apenas ao comando explícito de iniciar; após sucesso trocar /new por /:id/edit com replace.
- [ ] Exibir catálogo com busca, paginação, produtos simples/variações, opções ativas do fornecedor e indicação dos produtos sem opção utilizável. Respeitar permissões de leitura de produtos/fornecedores/depósitos; a falta de acesso não pode disparar consultas proibidas.
- [ ] Colocar quantidade e botão de adicionar alinhados à direita. Pré-selecionar opção/depósito somente quando inequívocos; nos demais casos abrir editor embutido na mesma página. Mostrar unidade da opção e equivalência BASE antes de adicionar.
- [ ] Manter editor em região estável, independente de busca/paginação, e preservar regras de custo automático/manual e snapshots. Trocar fornecedor não deve manter opção incompatível nem reescrever itens salvos silenciosamente; aplicar a regra atual do backend.
- [ ] Centralizar gravações no hook; bloquear duplo clique e escritas concorrentes. Após erro de rede com resultado desconhecido, mostrar consulta do estado e bloquear repetição automática. Correspondência por valores iguais não comprova identidade de uma inclusão; se persistir ambiguidade, exigir conferência explícita, sem inventar sucesso.
- [ ] Atualizar compra/itens após escrita; total oficial vem da API. Tratar repetição de produto conforme o comportamento atual de Compras, sem somar ou fundir automaticamente linhas com opções/custos/depósitos diferentes.
- [ ] Buscar status atual antes de oferecer edição; quando outro usuário avançar a compra, encerrar os controles de rascunho e direcionar à consulta preservando aviso e rascunho local.
- [ ] Conferir manualmente busca/paginação sem perder editor, opção com conversão, vários depósitos, custo automático/manual, edição/remoção, falha de carregamento e layout em 1366×768 e 780 px. Escritas de conferência usam somente registros demonstrativos autorizados, sem enviar compromisso ou movimentar estoque real.

**Entrega verificável:** montar compra em uma página sem abrir um seletor modal para cada item; consulta e recebimento antigos continuam disponíveis até a tarefa 3.

## Tarefa 3 — Acompanhamento e recebimento de Compras

**Arquivos:** criar `PurchaseDetailsPage.jsx` e `PurchaseDetailsContent.jsx` em Operations; modificar `PurchasesPage.jsx`, `PurchaseReceiptForm.jsx`, `DocumentStock.jsx` e rotas. Reutilizar StockHistory até a tarefa 5.

**Interfaces propostas:** `PurchaseDetailsContent({ id, onEdit, onChanged })` apresenta dados e ações, sem Modal próprio. `PurchaseReceiptForm` ganha `embedded=false`, mantendo o comportamento atual como padrão para consumidores não migrados.

- [ ] Mover detalhes para /purchases/:id: resumo, itens, quantidades comerciais e BASE contratado/recebido/pendente, recibos e metadados. Separar ações principais de consulta e ações destrutivas, preservando vermelho fixo no cancelamento.
- [ ] Preservar a sequência rascunho → enviada → confirmada → recebimento e todas as restrições existentes, inclusive cancelamento. Não executar transição automaticamente ao navegar, editar ou salvar cabeçalho.
- [ ] Exibir recebimento como seção da compra: receber somente o que chegou agora, manter restante pendente, observação e unidades BASE. Manter armazenamento da tentativa, requestId, verificação no histórico e repetição da MESMA tentativa conforme o código atual.
- [ ] Manter consulta de saldos e histórico vinculados ao documento; extrair conteúdo de DocumentStock apenas se necessário, preservando os consumidores em Vendas.
- [ ] Substituir os atalhos da listagem por navegação; retirar os invólucros de detalhes que ficarem sem chamadas, após revisar referências.
- [ ] Revisar diretamente cada condição de status/permissão com PurchasesPage e PurchaseReceiptForm originais. No navegador, conferir estados disponíveis sem confirmar envio, cancelamento ou recebimento de dados reais. Exercitar essas escritas somente quando houver dados descartáveis explicitamente autorizados.

**Entrega verificável:** compra pode ser acompanhada e recebida a partir de sua página, com a proteção contra recebimento duplicado preservada.

## Tarefa 4 — Produtos: cadastro, ficha e variações

**Arquivos:** criar `ProductEntryPage.jsx`, `ProductDetailsPage.jsx`, `ProductDetailsContent.jsx` e `ProductVariantPage.jsx` em Products. Modificar `ProductsPage.jsx`, `ProductForm.jsx`, `ProductDetailsModal.jsx`, `ProductItemDetails.jsx`, `ProductVariants.jsx`, `ProductPresentations.jsx`, `ProductSuppliers.jsx`, `products.css` e rotas.

**Interfaces propostas:** `ProductForm` ganha `embedded=false`, preservando catalog/item/onSaved/onClose. `ProductDetailsContent({ catalog, item, section, onSectionChange, onChanged })` compõe conteúdos atuais; ProductVariantPage carrega SKU e valida catálogo pai antes de renderizar.

- [ ] Extrair o formulário sem duplicar productFormData/useProductForm: tipo simples/variante, unidade e confirmação de legado, marca/categoria, características, custo/preço, medidas e estoque inicial continuam com suas regras atuais.
- [ ] Criar ficha por catálogo com resumo e seções Dados gerais, Dados comerciais, Variações (quando aplicável), Apresentações, Fornecedores/opções e Estoque. A seção Estoque deve usar ProductStockPanel diretamente; manter as ações rápidas do painel em Modal.
- [ ] Direcionar detalhes/edição da listagem e detalhes de variação para as novas páginas. Cadastro/edição de variação, apresentação e opção podem continuar como formulários pontuais sobre a página; não manter modal de detalhes sobre outro modal de detalhes.
- [ ] Preservar apresentações, fatores/conversões confirmados, estados próprios/herdados do SKU, fornecedor preferencial, vínculos, opções, custos e registros antigos. Nenhuma escolha de seção altera preços ou estados.
- [ ] Preservar ProductStockCell/Popover e acesso rápido ao estoque. O modo StockPage para quem só tem stock.read permanece acessível, sem exigir products.read e sem expor ficha comercial. Ficha de produto não substitui permissões de estoque/fornecedores.
- [ ] Salvar edição retorna à ficha de origem; novo cadastro abre a ficha apenas com ID confirmado. Respostas antigas de carregamento não sobrescrevem o produto atual ou formulário digitado.
- [ ] Conferir manualmente simples e variante, vínculo ao pai, unidade legada, seção sem permissão, apresentação/opção inativa, retorno à lista e cadastro auxiliar de marca sem perder o formulário. Revisar payloads e permissões antes/depois; não gerar estoque inicial apenas para inspeção visual.

**Entrega verificável:** gerenciamento de um produto ocorre em página própria, preservando as consultas rápidas da listagem e o acesso restrito de estoque.

## Tarefa 5 — Histórico geral de estoque

**Arquivos:** criar `StockHistoryPage.jsx` e `StockHistoryContent.jsx` em Operations. Modificar `StockHistory.jsx`, `ProductsPage.jsx`, `StockPage.jsx`, `ProductStockPanel.jsx`, `PurchasesPage.jsx`/`PurchaseDetailsContent.jsx` e rotas, conforme chamadas existentes.

**Interface proposta:** `StockHistoryContent({ filter })` compartilha carregamento/tabela; StockHistory permanece invólucro modal para consultas contextuais. StockHistoryPage valida query string e compõe filtros de página.

- [ ] Criar /stock/history, com PageHeading e filtros que a API já aceita: SKU/productId, depósito, tipo, motivo, intervalo, pedido/compra, operador/userId e stockItemId quando vindo do contexto.
- [ ] Usar stockOperations.history sem mudar contrato: hoje retorna array, não Page. Indicar total dos resultados recebidos; se houver paginação visual, declarar que é local. Paginação real de servidor fica para trabalho separado, sem afirmar que paginar localmente reduz carga da API.
- [ ] Preservar movimento, data/hora, motivo, observação, quantidade/unidade, saldo anterior/novo, documento e operador. Não inventar nomes de operadores quando a resposta só fornece userId.
- [ ] Botão geral de Produtos/StockPage navega à página; atalhos contextuais continuam filtrados por SKU, saldo ou documento. Validar e manter filtros na URL; limpar filtro retorna à consulta geral sem carregar dados comerciais proibidos.
- [ ] Conferir combinação de filtros, resultado vazio, API indisponível, datas inválidas, enum desconhecido, IDs distintos de catálogo/SKU/saldo e acesso apenas com stock.read. Conferir tabela no notebook sem cortar acesso a documento/operador.

**Entrega verificável:** histórico geral usa o espaço da página; consultas rápidas e Vendas continuam funcionando com o mesmo conteúdo compartilhado.

## Tarefa 6 — Fornecedores: ficha e edição

**Arquivos:** criar `SupplierEntryPage.jsx`, `SupplierDetailsPage.jsx`, `SupplierDetailsContent.jsx`, `SupplierCatalogLinksContent.jsx` e `SupplierAddresses.jsx` em Suppliers. Modificar SuppliersPage, SupplierForm, SupplierDetailsModal, SupplierAddressModal, SupplierCatalogLinksModal, supplierDetails.css e rotas.

**Interfaces propostas:** `SupplierForm` ganha `embedded=false`; `SupplierAddresses({ supplierId, onChanged })` compõe listagem e editor atual; `SupplierCatalogLinksContent({ supplierId, onChanged })` reaproveita busca e ações atuais sem Modal próprio.

- [ ] Migrar cadastro/edição sem modificar supplierFormData ou as regras de prazo, contatos, CNPJ e endereço inicial. Ficha exibe resumo, endereços, marcas/categorias e metadados em seções.
- [ ] Manter vínculos de marca/categoria independentes de produtos/opções de compra. Remover vínculo não pode excluir o cadastro associado. Preservar busca, mensagens e confirmações específicas.
- [ ] Acesso direto a Endereços na lista navega para a seção da ficha; editores de endereço abrem sobre a página e recarregam seu conteúdo ao salvar, sem fechar toda a ficha.
- [ ] Preservar razão social/CNPJ e demais campos atuais do fornecedor, estado ativo, permissões, CEP, cancelamento de descarte e retorno à lista filtrada. Não introduzir PF/PJ nem copiar regras exclusivas de clientes.
- [ ] Conferir fornecedor sem endereço/vínculos, múltiplos endereços, inclusão/remoção canceladas, validações e edição iniciada da ficha ou da lista. Revisar diretamente payloads e fluxos antigos antes de remover invólucros sem uso.

**Entrega verificável:** ficha/edição do fornecedor e seus vínculos deixam de exigir sequência de modais de detalhes.

## Tarefa 7 — Clientes: ficha e edição

**Arquivos:** criar `CustomerEntryPage.jsx`, `CustomerDetailsPage.jsx`, `CustomerDetailsContent.jsx` e `CustomerAddresses.jsx` em Customers. Modificar CustomersPage, CustomerForm, CustomerDetailsModal, CustomerAddressModal, customerDetails.css e rotas.

**Interfaces propostas:** `CustomerForm` ganha `embedded=false`; `CustomerAddresses({ customerId, onChanged })` compõe consulta e editor atual. Manter componentes de domínio separados dos de fornecedor, compartilhando apenas estruturas já comuns.

- [ ] Migrar formulário para página preservando PF/PJ, CPF/CNPJ, nome fantasia condicional, nascimento, contatos, tabela de preços, ativo e endereço inicial.
- [ ] Criar ficha com resumo, endereços, tabela de preços e metadados; Endereços na lista abre a seção correspondente. Consultas de preços continuam condicionadas a prices.read; edição preserva associação atual quando o usuário não pode consultá-la.
- [ ] Manter unicidade/tipos de endereço conforme regras atuais do cliente. Preservar limpeza de opcionais e demais payloads existentes; não unificar regras com fornecedores.
- [ ] Manter seleção de cliente no fluxo de Vendas funcionando como seletor auxiliar, sem obrigar a sair de uma venda para consultar o cliente.
- [ ] Conferir PF/PJ, tabela inativa ou indisponível, ausência de prices.read, erros de CPF/CNPJ/CEP, descarte cancelado e retorno após editar. Comparar condições e payloads diretamente com customerFormData/CustomerForm atuais.

**Entrega verificável:** ficha e formulário de clientes são páginas, preservando endereços, regras comerciais e os consumidores em Vendas.

## Tarefa 8 — Revisão de cada entrega e encerramento

**Arquivos:** somente os alterados na entrega e documentação de progresso a criar em `docs/superpowers/plans/2026-11-01-paginas-de-operacao-progress.md`.

- [ ] Revisar diff e referências: chamadas removidas, imports, rotas, permissões, payloads, foco, seletores e efeitos assíncronos. Não apagar estilos/componentes compartilhados ainda usados por outros módulos.
- [ ] Conferir manualmente 1366×768, desktop amplo, 780 px e 390 px; menu expandido/recolhido quando disponível. Verificar rolagem transparente, cantos de modais, teclado/Esc, foco e ações acessíveis. Painel fixo só quando couber na altura disponível.
- [ ] Reabrir Vendas, Depósitos e seletores compartilhados atingidos pelas extrações, sem executar gravações reais. A inspeção visual não comprova idempotência nem concorrência: registrar o que foi apenas revisado no código.
- [ ] Registrar por módulo: arquivos alterados, fluxos preservados, conferências realmente feitas, lacunas e limitações. Usar dados demonstrativos autorizados para qualquer gravação manual futura; não alterar usuários/permissões para viabilizar conferência.
- [ ] Não executar testes, build ou lint. Não declarar aprovação de validações que não tenham sido realizadas.

## Sequência de execução

1. Tarefas 1 + 2 + 3: Compras, com revisão da tarefa 8 antes de avançar.
2. Tarefa 4: Produtos, com revisão da tarefa 8.
3. Tarefa 5: Histórico, com revisão da tarefa 8.
4. Tarefa 6: Fornecedores, com revisão da tarefa 8.
5. Tarefa 7: Clientes, com revisão final da tarefa 8.

Cada etapa deve funcionar antes da próxima. Esta sequência evita transformar a migração em uma reescrita única da aplicação. O plano não inclui auditoria cadastral nova, mudanças de backend, filtros dependentes de contratos inexistentes ou conversão de todas as janelas indiscriminadamente.

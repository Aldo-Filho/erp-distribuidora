# Unificar Produtos e Estoque — plano de implementação

**Status:** implementação realizada; revisão estática concluída. Validação manual pendente.

**Objetivo:** uma única entrada “Produtos” no menu, reunindo cadastro e estoque sem duplicar telas de consulta.

**Abordagem:** adicionar Estoque aos detalhes de cada SKU e reutilizar as operações atuais. Não criar outro sistema de movimentação nem alterar backend, banco ou contratos. Manter os componentes e estilos do JamsHub.

## 1. Reutilizar as operações existentes

**Arquivos:** `frontend/src/pages/Operations/StockPage.jsx`; criar `frontend/src/pages/Operations/StockActions.jsx`.

- [x] Extrair somente `StockConfiguration` e `StockMovement` para o arquivo compartilhado, com seus auxiliares necessários. A página atual continua usando os mesmos componentes.
- [x] Preservar integralmente UUID, repetição após timeout, payload congelado, recuperação da operação, bloqueio de envio e confirmações existentes.
- [x] Permitir produto fixo na configuração iniciada pelos detalhes do SKU: preencher e bloquear o produto, sem obrigar nova seleção. Separar a chave do rascunho por usuário e SKU para não reutilizar dados de outro produto.

## 2. Adicionar Estoque aos detalhes do produto

**Arquivos:** criar `frontend/src/pages/Products/ProductStockPanel.jsx`; alterar `ProductItemDetails.jsx` e, se necessário, `ProductDetailsModal.jsx` na mesma pasta.

- [x] Criar seção Estoque em `ProductItemContent`, visível com `stock.read`. O componente é compartilhado por produtos simples e detalhes de variações; manter o estoque sempre associado ao SKU, nunca ao catálogo pai.
- [x] Mostrar depósitos, saldos físico/reservado/disponível e limites mínimo/máximo. Buscar todas as páginas necessárias, respeitando o formato `content` da API.
- [x] Reutilizar `StockHistory` com filtro fixo por produto ou estoque do depósito; impedir histórico de outro SKU por filtro residual.
- [x] Oferecer configurar depósito/limites com `stock.configure` e registrar movimentação com `stock.adjust`, mantendo exigências adicionais atuais de consulta de produtos/depósitos.
- [x] Após salvar, recarregar saldos e resumo da lista; ao fechar e reabrir o histórico, consultar os dados atuais. Exibir carregamento, erro com nova tentativa e estado vazio.

## 3. Simplificar o acesso na lista

**Arquivos:** `ProductsPage.jsx`, `ProductListRow.jsx`, `ProductVariants.jsx`, `ProductStockCell.jsx`, `products.css` em `frontend/src/pages/Products`.

- [x] Manter apenas o resumo de saldo disponível na lista; abrir os detalhes na seção Estoque ao acioná-lo, sem manter um segundo diálogo com as mesmas informações.
- [x] Encaminhar o SKU correto: produto simples abre diretamente; catálogo com variações exige selecionar a variação, sem misturar saldos de SKUs diferentes.
- [x] Manter histórico geral acessível por uma ação em Produtos para quem tem `stock.read`.
- [x] Preservar teclado, foco, descarte de alterações e funcionamento em telas estreitas. Não adicionar novas bibliotecas.

## 4. Retirar a entrada duplicada do menu

**Arquivos:** `frontend/src/routes/{navigation.js,AppRoutes.jsx,ProtectedRoute.jsx}` e `frontend/src/components/layout/AppLayout.jsx`.

- [x] Exibir uma única entrada Produtos para quem tem `products.read` ou `stock.read`. Aplicar o mesmo critério à rota, à página inicial e ao retorno após login; não apenas esconder o menu.
- [x] Para usuários com `stock.read` sem `products.read`, reutilizar a visão de estoque existente dentro dessa entrada, sem consultar endpoints de catálogo nem liberar ações de cadastro de produtos.
- [x] Manter `/stock` como redirecionamento protegido para `/products`, sem loop, preservando favoritos. Fazer essa troca somente depois de integrar os recursos de estoque.
- [x] Não conceder permissões novas. Usuários com apenas `products.read` continuam sem acesso a histórico ou movimentações de estoque.

## 5. Revisão e critérios de aceite

- [x] Revisar diretamente os contratos, imports, dependências entre componentes e cálculos de exibição, sem criar referências circulares nem alterar regras de persistência.
- [ ] Conferir manualmente: produto simples, variações, nenhum depósito, vários depósitos, erro da API, troca rápida de SKU e atualização após salvar.
- [ ] Conferir perfis com apenas produtos, apenas estoque, consulta sem escrita e acesso completo; validar menu, acesso direto e `/stock`.
- [ ] Conferir configuração, movimentação, cancelamento do diálogo e repetição após falha sem duplicar registros, exclusivamente em banco isolado e aplicação previamente confirmada contra ele; não usar o banco habitual para essa validação.
- [x] Seguir AGENTS.md: não criar/executar testes automatizados, build ou lint sem pedido explícito; registrar o que foi e o que não foi verificado. Sem commits ou deploy.

**Concluído quando:** existe uma única entrada no menu; cadastro, saldos, histórico e operações continuam acessíveis conforme as permissões; o fluxo de vendas/compras permanece funcional. Não prometer ausência de erros antes da validação.

## Registro da execução

- Uma entrada Produtos atende `products.read` ou `stock.read`; o perfil somente estoque reutiliza a consulta de estoque, sem consultar o catálogo.
- `/stock` redireciona para `/products`; menu, retorno do login, página inicial e aviso de Minha conta usam a mesma regra de acesso.
- Estoque por SKU integrado aos detalhes, com formulários extraídos e rotina de movimentação preservada; histórico geral continua acessível.
- Rascunho de nova configuração por SKU; produto fixo não pode ser trocado nessa configuração. Saldos são recarregados após salvar.
- Decisão: preservar o checkout atual e as alterações locais. Não criar worktree, executar testes, build, lint, serviços, operações de banco, commits ou deploy.
- Validação no navegador e operações no banco isolado não foram executadas nesta rodada.
- Revisão adicional somente leitura: nenhum achado relevante em imports, isolamento por SKU, recarga, permissões, rotas e recuperação da tentativa de movimentação. Isso não substitui validação prática.

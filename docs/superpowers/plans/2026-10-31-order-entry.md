# Registrar venda lado a lado — Implementation Plan

> **For agentic workers:** usar `superpowers:executing-plans` para implementar este plano tarefa por tarefa. Steps use checkbox (`- [ ]`) syntax for tracking. Não delegar nem criar commits automaticamente. As preferências de AGENTS.md prevalecem sobre ciclos de testes sugeridos por skills.

**Goal:** permitir montar e editar pedidos pendentes com catálogo e pedido na mesma página, preservando todas as funcionalidades atuais inventariadas.

**Architecture:** página dentro do AppLayout, catálogo independente e pedido persistido incrementalmente pelas APIs atuais. Coordenador controla fila de gravações, totais e reconciliação; os formulários de inclusão/edição continuam usando as regras existentes. A consulta atual permanece para etapas posteriores.

**Tech Stack:** React, React Router, CSS/Tailwind existentes, lucide-react, services atuais; sem novas dependências.

**Spec:** `docs/superpowers/specs/2026-10-31-order-entry-design.md`.

## Global Constraints

- Não criar/executar testes automatizados, build ou lint sem solicitação explícita.
- Preservar alterações existentes no Git; não formatar globalmente, não alterar Compras como efeito colateral.
- Manter estilos, componentes compartilhados, permissões, mensagens e lógica comercial do projeto.
- Pendente não reserva estoque; confirmar reserva; despachar registra saída; cancelar libera reservas existentes.
- Preços/totais salvos e transições são decididos pelo backend. Preço vigente não substitui preço antigo sem ação explícita.
- Rascunhos atuais de formulário são em memória, isolados por usuário; não prometer recuperação após reload.

## Review Focus

1. Repetir inclusão da mesma combinação sem duplicar item, perder desconto ou somar duas vezes após timeout — tarefas 3 e 5.
2. Trocar cliente, apresentação ou produto durante cotação sem usar resposta antiga nem reprecificar linha salva silenciosamente — tarefas 2 e 3.
3. Editar item existente com permissões reduzidas, preservando preço, desconto, identidade e snapshots — tarefas 3 e 4.
4. Pedido confirmado/cancelado em outra sessão ou estoque alterado antes da confirmação — tarefas 4 e 5.
5. Notebook com menu expandido, pouca altura e retorno após login sem perder navegação ou ocultar controles — tarefas 1, 4 e 5.

Estas condições têm conferências manuais específicas abaixo; não constituem autorização para testes automatizados.

## Arquivos e responsabilidades

Novos em `frontend/src/pages/Operations/`:

- `OrderEntryPage.jsx`: página e composição dos painéis, carregamento inicial e integração com permissões.
- `OrderEntryHeader.jsx`: cliente, pagamento e depósito sugerido; criação inicial e edição de cabeçalho.
- `OrderProductCatalog.jsx`: busca paginada, expansão de variações e inclusão sem sair da tela.
- `OrderCart.jsx`: linhas salvas, edição embutida, remoção, totais e ações do pedido.
- `useOrderEntry.js`: estado do pedido, fila de gravação, recarga e reconciliação.
- `orderEntry.css`: composição responsiva da página, sem sobrescrever outros módulos.

Existentes a modificar:

- `frontend/src/routes/AppRoutes.jsx`: rotas de registro e edição.
- `frontend/src/routes/navigation.js`: retorno seguro às rotas e seleção de Vendas na navegação.
- `frontend/src/pages/Operations/OrdersPage.jsx`: abertura da página de registro/edição e retorno à listagem.
- `frontend/src/pages/Operations/TradeItemForm.jsx`: opção de renderizar o formulário de venda embutido e iniciar com seleção explícita; preservar modo modal e Compras.
- `frontend/src/pages/Operations/OperationSelectors.jsx`: expor/reutilizar partes do catálogo sem tornar obrigatório o fluxo catálogo → outra tela de SKUs.
- `frontend/src/pages/Operations/salesErrors.js`: apenas mensagens adicionais necessárias aos novos estados de erro.

Referências que não precisam mudar: `services/operations.js`, `services/products.js`, `services/productStock.js`, `auth/drafts.js`, `useProductForm.js`, `RowActions`, `FilterSelect`, `Modal`, `useConfirm`, `StockHistory` e `DocumentStock`.

## Task 1: rotas e esqueleto funcional

**Files:** criar `OrderEntryPage.jsx`, `OrderEntryHeader.jsx`, `orderEntry.css`; modificar `AppRoutes.jsx`, `navigation.js`, `OrdersPage.jsx`.

**Interfaces:** `OrderEntryPage()` resolve ID da rota; `OrderEntryHeader({ order, defaultWarehouseId, onWarehouseChange, onCreated, onSaved, disabled })` trabalha com entidades existentes. Antes de criar, `order` é null.

- [ ] Abrir `/orders/new` por “Nova venda” e `/orders/:id/edit` por “Editar” apenas para pedidos pendentes. Manter consulta dos demais estados.
- [ ] Usar guarda de `orders.read`, verificar `orders.create` na criação e as permissões atuais de edição de pedido/itens nas ações. Não exigir `orders.update` para todas as tarefas de quem só adiciona/remove itens.
- [ ] Criar PENDING uma vez por ação explícita “Iniciar pedido”; após sucesso, substituir a URL. Bloquear cliques duplos e reconciliar falha incerta antes de permitir outra criação.
- [ ] Preservar filtros e página de origem usando estado da navegação e restaurá-los ao retornar; acesso direto sem origem retorna à listagem normalmente.
- [ ] Estender `safeReturn` para os padrões exatos `/orders/new` e `/orders/<id positivo>/edit`, com checagem de acesso; não liberar qualquer caminho pelo prefixo `/orders`.
- [ ] Conferência manual: criar uma venda, recarregar URL, voltar a uma listagem filtrada, retomar após login e abrir URL sem permissão. Cabeçalho selecionado não deve criar dois pedidos.

## Task 2: catálogo permanente e carregamento rápido

**Files:** criar `OrderProductCatalog.jsx`; modificar apenas os pontos necessários de `OperationSelectors.jsx`.

**Interfaces:** `OrderProductCatalog({ order, defaultWarehouseId, items, writePending, onAdd, onEditExisting })`. `onAdd(selection)` recebe `{ product, presentation, warehouseId, quantity }`, sem atribuir preço base como preço final.

- [ ] Reutilizar pesquisa/paginação existentes com debounce de 400 ms. Manter busca, página e resultados ao adicionar itens.
- [ ] Mostrar simples e variações com nome, marca, SKU/atributos; expandir SKUs dentro do catálogo. Não carregar só a primeira página e esconder o restante.
- [ ] Consultar apresentações paginadas, depósitos configurados e preços apenas quando necessários. Apresentação BASE válida pode ser sugerida; alternativa única segue a regra atual. Havendo ambiguidade, mostrar os controles no próprio produto.
- [ ] Aplicar depósito sugerido somente a novas inclusões quando válido para o SKU; não trocar linhas salvas. Sem depósito único/padrão válido, solicitar seleção embutida.
- [ ] Manter busca visível enquanto preços carregam. Limitar requisições auxiliares a quatro simultâneas; deduplicar dados de SKU/apresentações/estoque e cotação por contexto. Invalidar dados de preço ao alterar cliente.
- [ ] Exibir saldo como informação real, não como saldo já descontado do pedido pendente. Ausência de `stock.read` não deve inventar saldos nem introduzir bloqueios além dos atuais.
- [ ] Conferência manual: buscar rapidamente, mudar busca antes da resposta, selecionar variação da segunda página, alternar depósito e incluir três produtos sem reabrir catálogo. Inspecionar rede manualmente para conferir que o catálogo não é recarregado a cada inclusão e não aguarda todas as cotações para aparecer.

## Task 3: editor embutido com todas as regras atuais

**Files:** modificar `TradeItemForm.jsx`; criar `OrderCart.jsx`; integrar ao catálogo.

**Interfaces:** preservar props atuais e acrescentar `embedded = false`, `initialSelection = null` e `writeItem = null` a `TradeItemForm`. `initialSelection` é `{ product, presentation, warehouseId, quantity }`; `writeItem(itemOrNull, body): Promise<OrderItem>` recebe o payload já validado e delega à fila do coordenador. Sem callback, manter o caminho atual de persistência, incluindo Compras. `OrderCart({ order, items, pending, onUpdate, onRemove, onChanged })` recebe entidades salvas e callbacks de gravação do coordenador.

- [ ] Renderizar o formulário de venda na própria página quando `embedded`; manter wrapper Modal e descartes existentes nos usos antigos. Nenhum modal novo para a inclusão rotineira.
- [ ] Encaminhar a persistência de todos os editores embutidos por `writeItem`; não deixar chamadas diretas ao service escaparem da fila. A inclusão rápida usa a mesma validação e montagem de payload do editor, sem duplicar regras de preço/conversão.
- [ ] Iniciar o formulário com o SKU/apresentação/depósito selecionados; evitar reinicialização por respostas de preço ou recargas de outras linhas. Identificar cada edição pelo item e contexto do pedido.
- [ ] Preservar integralmente as regras de `TradeItemForm`: precisão comercial/BASE, apresentação ativa e pertencente ao SKU, reconsulta de apresentação antes de salvar, depósito configurado, validações de conversão e erros traduzidos.
- [ ] Preservar preço automático, preço salvo, preço manual autorizado, restaurar preço, consultar vigente e aplicar vigente com `orders.manualPrice`; manter descontos em valor/percentual com `orders.discount` e totais bruto/desconto/líquido.
- [ ] Na mudança de cliente, salvar cabeçalho e invalidar cotações de novas linhas; manter os preços das linhas já gravadas e o aviso atual.
- [ ] Para item repetido, localizar SKU + apresentação + depósito. Com `orders.update`, oferecer incremento explícito via quantidade final e preservar preço/desconto. Sem essa permissão, explicar a duplicidade. Campos comerciais locais diferentes direcionam à revisão da linha existente.
- [ ] Preservar a edição limitada de itens sem permissão de consultar produtos/depósitos, usando identidade e snapshots existentes. Não exigir novas consultas para alterar somente campos que já eram permitidos.
- [ ] Conferência manual: preço manual, desconto percentual/valor, preço antigo após mudar quantidade, troca de cliente, apresentação inativada durante edição, item legado e usuário sem `orders.manualPrice`, `orders.discount`, `products.read` ou `warehouses.read`. Confirmar que a inclusão de itens de Compras conserva o comportamento anterior.

## Task 4: coordenação, persistência e ações do pedido

**Files:** criar `useOrderEntry.js`; integrar `OrderEntryPage.jsx`, `OrderEntryHeader.jsx`, `OrderCart.jsx`; reutilizar regras de transição de `OrdersPage.jsx`.

**Interfaces:** `useOrderEntry(id)` retorna `{ order, items, loading, error, writePending, uncertain, reload, saveHeader, addItem, updateItem, removeItem, confirmOrder, cancelOrder }`. `saveHeader(body)`, `addItem(body)`, `updateItem(itemId, body)` e `removeItem(itemId)` aceitam os mesmos argumentos dos services existentes; confirmar/cancelar usam o ID do pedido do hook. Escritas retornam Promise; `uncertain` bloqueia nova escrita até reconciliação. Não criar novos endpoints.

- [ ] Separar estado do pedido do catálogo; serializar escritas por pedido e manter alvo absoluto de cada alteração de quantidade. Atualizar linhas e total pelos resultados/recarga do servidor.
- [ ] Após adicionar, manter o usuário na mesma página, preservar contexto e voltar foco ao fluxo de busca/inclusão. Mostrar andamento e erro por linha sem apagar o pedido.
- [ ] Remover itens com confirmação específica. Manter confirmação/cancelamento bloqueados enquanto há envio, editor não resolvido ou resultado incerto.
- [ ] Reutilizar confirmação e reconciliação existentes de status. PENDING confirma/reserva; confirmado despacha na consulta atual; pendente/confirmado cancela conforme permissões e libera reservas. Não confirmar e despachar numa única ação nova.
- [ ] Em timeout de inclusão/cadastro, reconciliar entidades antes de outra tentativa. Nunca fazer repetição automática de POST nem um novo incremento baseado em alvo não confirmado.
- [ ] Preservar “Manter pendente”, “Concluir edição”, consultas de saldos/reservas, histórico de saída e metadados. Ao concluir a montagem, abrir a consulta existente ou retornar à listagem; manter o despacho acessível nessa consulta.
- [ ] Ao encerrar explicitamente uma edição ou usar “Manter pendente”, resolver campos não enviados com salvar/descarte via `useConfirm`, permitindo cancelar a saída. Em outras navegações internas/back, manter rascunhos e a identidade da edição em `useDraft` para retomada. Usar aviso de saída/reload do navegador somente quando houver campos não enviados. Não migrar o BrowserRouter apenas para acrescentar um blocker incompatível; não prometer recuperação após reload, nem desfazer itens já gravados.
- [ ] Conferência manual: falha de rede após inclusão, dois cliques rápidos em incluir/confirmar, outra aba confirmando o pedido, perda de estoque na confirmação, descarte cancelado e sessão expirada. Conferir que editar/adicionar não reserva estoque.

## Task 5: integração visual e conferência de paridade

**Files:** `orderEntry.css`, `OrderEntryPage.jsx`, `OrdersPage.jsx` e ajustes localizados nos componentes novos.

- [ ] Manter AppLayout, menu de Vendas selecionado e paleta atual; não alterar favicon, dependências ou estilos de outras páginas.
- [ ] Usar duas colunas quando a área de conteúdo comportar 1000 px; abaixo, empilhar sem perder dados. Respeitar menu de 244/78 px, controles acessíveis, foco, teclado, dropdowns e modais auxiliares sem corte.
- [ ] Preservar linhas compactas e legíveis; não aplicar cegamente a altura da listagem aos editores de inclusão. Evitar editor expandido esconder pedido/ações.
- [ ] Conferir manualmente 1366×768 com lateral expandida e recolhida, desktop amplo e largura móvel de 390 px. Examinar também nomes longos, muitos itens, segunda página de variantes/apresentações e erro de preço.
- [ ] Percorrer cada linha do inventário da spec e registrar onde está disponível. Não remover o acesso antigo necessário a uma funcionalidade até que seu destino esteja implementado.
- [ ] Revisar diretamente diff, imports, condições de permissão e contratos; listar o que foi ou não observado no navegador. Sem testes automatizados, build ou lint por padrão.

## Critérios de conclusão

- Três produtos diferentes podem ser incluídos em sequência na mesma página, sem modal por produto e sem reiniciar busca/depósito.
- As operações e permissões do inventário possuem destino implementado e acessível.
- Preços, descontos, duplicidades, reservas e resultados incertos seguem as regras atuais.
- A tela funciona na largura/altura do notebook e ao retornar da autenticação.
- A listagem, a consulta de pedidos finalizados e o fluxo de Compras não são reescritos.

## Revisão deste plano

Inventário comparado por leitura com `OrdersPage.jsx`, `TradeItemForm.jsx`, `OperationSelectors.jsx`, services, rotas, rascunhos e contratos de pedido/item no backend. O desenho cobre as funções identificadas; diferenças do rascunho estão explicitadas na spec. Nenhum teste, build, lint, navegação de validação ou medição de desempenho foi executado. Estes documentos não iniciam a implementação.

# Progresso — páginas de operação

Plano: `2026-11-01-paginas-de-operacao.md`.

## Condições de execução

- Estado inicial: apenas o plano não rastreado. Sem alterações prévias de código.
- Implementação pelo próprio agente, sem delegação, commits, testes, build ou lint.
- Criação de branch local recusada pelo usuário; trabalho continua no checkout atual.
- Rascunhos são memória da aba; não recuperáveis após reload. beforeunload avisa sobre campos alterados.
- APIs/backend e regras comerciais não entram no escopo.

## Compras — tarefas 1, 2 e 3 / revisão 8

Implementado: rotas de cadastro, montagem e acompanhamento; retorno com contexto de listagem; catálogo paginado com expansão de SKUs/opções do fornecedor; editor de item independente da pesquisa; carrinho com total oficial; cabeçalho reutilizado; recebimento embutido preservando armazenamento, requestId e reconciliação existentes. Gravações da montagem são serializadas; resultado desconhecido bloqueia repetição até conferência explícita. Transições também bloqueiam após resultado desconhecido.

Revisão direta: payload do cabeçalho preserva userId e remoção explícita da data; TradeItemForm preserva custo automático/manual, fornecedor e conversão; condições de envio/confirmação/cancelamento/recebimento copiadas do conteúdo original. Componentes contextuais de saldos/histórico continuam com os mesmos consumidores.

Navegador: tentativa de abrir `http://localhost:5173/purchases` retornou `ERR_CONNECTION_REFUSED`. Nenhuma inspeção visual, gravação, simulação de concorrência ou validação de conta restrita foi feita. Layout responsivo revisado apenas no código. Conferência manual com servidor e sessão disponíveis permanece pendente.

Arquivos: PurchasesPage, PurchaseDetailsContent/Page, PurchaseEntryPage/Header, PurchaseProductCatalog, PurchaseCart, usePurchaseEntry, purchaseEntry.css, PurchaseReceiptForm, TradeItemForm, useProductForm, useListReturn, EmbeddedContent, rotas/navigation.

## Produtos — tarefa 4 / revisão 8

Implementado: cadastro/edição embutidos sem duplicação de productFormData/useProductForm; ficha com seções na URL; variação com validação de catalogId; navegação da listagem e das variações; estoque diretamente na seção; formulários pontuais de apresentação, opção e variação mantidos. Popover/célula de estoque e StockPage preservados. Contexto de busca/paginação capturado antes de abrir páginas.

Revisão direta: guards products.read nas novas rotas; permissão de escrita antes de renderizar e no formulário original; estoque/fornecedores continuam condicionados; getCatalogId no backend confirma a associação usada na ficha de SKU. Sem mudanças em payloads, custos, conversões ou estoque inicial. Nenhuma gravação de inspeção foi realizada. Validações visuais e conta reduzida pendentes; tentativas de acesso descritas no encerramento.

## Histórico — tarefa 5 / revisão 8

Implementado: StockHistoryContent compartilhado com o invólucro modal; /stock/history com filtros reconhecidos pelo StockMovementFilter, IDs seguros, enumerações e intervalo validados antes da consulta. Botões gerais de Produtos/StockPage navegam para a página. Atalhos contextuais de Vendas, Compras, SKU e saldo continuam filtrados no modal.

Revisão direta: API permanece array; total corresponde ao array recebido. Nenhuma consulta comercial auxiliar adicionada, rota exige apenas stock.read. Tabela preserva observação, unidade, saldos, documento e userId. Sem paginação fictícia de servidor. Conferência visual não realizada; ver encerramento.

## Fornecedores — tarefa 6 / revisão 8

Implementado: ficha por seções (dados, endereços, marcas/categorias, metadados), cadastro e edição em página; Endereços da listagem navega para a seção. Lista de endereços embutida, editor pontual em Modal, atualização local após salvar/excluir. Vínculos reutilizam as consultas e confirmações originais sem Modal externo.

Revisão direta: supplierPayload, supplierValues, validateSupplier e regras de CNPJ/prazo/CEP não foram alterados. Remover vínculo continua removendo somente a associação. Listagem restaura filtros NAME/DOCUMENT/CONTACT, situação, página e tamanho. Rascunhos mantidos em memória com descarte e aviso de reload. Sem escrita real ou validação visual.

## Clientes — tarefa 7 / revisão 8

Implementado: ficha com dados, endereços, tabela de preços e metadados; formulários embutidos, endereços pontuais sobre a página, atalhos da lista com navegação. Edição retorna à seção de origem; cadastro abre ficha somente com ID confirmado.

Revisão direta: customerPayload/customerValues/validateCustomer preservados; PF/PJ, documento, nascimento, opcionais, preço e endereço inicial continuam no formulário original. Consulta de tabela é condicionada a prices.read, associação existente continua preservada pelo payload original sem essa permissão. A regra de tipos únicos de endereço permanece separada da regra de fornecedores. Seleção de cliente em Vendas não foi alterada. Sem escrita real ou validação visual.

## Decisão de compatibilidade

A página de montagem de uma compra existente aceita purchases.create ou purchases.update: o fluxo anterior autorizava inclusão de item apenas com purchases.create. Exigir update adicional após criar o cabeçalho retiraria essa operação de um usuário já autorizado. A edição de cabeçalho/item continua exigindo update e a remoção exige delete. Esta exceção ao acesso mínimo sugerido para /edit preserva a política operacional existente; sua conferência com conta reduzida permanece pendente.

## Encerramento — tarefa 8

- Implementação das tarefas 1–7 e revisão direta de cada módulo realizadas. Revisão final pelo próprio autor, sem delegação conforme solicitado.
- Revisados diffs, referências removidas, rotas/IDs, retornos após login, filtros, permissões, descartes e payloads. Removidos ProductDetailsModal, SupplierDetailsModal e CustomerDetailsModal após confirmar ausência de consumidores.
- Ajustes da revisão: preservar catálogo/editor durante atualização da compra; esperar recarga após escrita sem transformar falha de consulta em falha de gravação confirmada; bloquear repetição desconhecida; manter seleção/recebimento digitado em memória; invalidar seleção após mudança de fornecedor; impedir confirmações/inclusões concorrentes por clique repetido; manter contagem real do histórico.
- A tentativa de gravação da compra é marcada na memória antes do envio, fora do estado React, para sobreviver à desmontagem da página. O bloqueio de escritas em andamento é compartilhado entre montagem e acompanhamento na aba; uma conferência não libera uma requisição ainda em andamento. Recebimento conserva sua proteção própria e o requestId persistido.
- Contexto de listagem usa a memória de drafts, isolada por usuário e limpa no logout. URLs/history.state transportam apenas navegação e seções, não os campos dos formulários.
- Formatação limitada aos arquivos de frontend alterados/criados. Nenhuma formatação de backend ou de outros módulos.
- Não foram criados/executados testes automatizados, build ou lint. Nenhum commit, operação comercial real ou mudança de usuário/cargo/permissão.

### Conferência manual: limite efetivamente observado

A primeira navegação a localhost:5173 retornou ERR_CONNECTION_REFUSED. Depois foi iniciado o Vite em 127.0.0.1:5173 (servidor de desenvolvimento, sem build); a ferramenta de navegador integrado retornou ERR_CONNECTION_TIMED_OUT. A tentativa pelo Edge local falhou ao vincular a janela minimizada, inclusive após solicitar sua abertura. O processo Vite iniciado nesta execução foi encerrado.

Portanto, nenhuma tela do JamsHub foi observada no navegador nesta implementação. Permanecem pendentes as conferências de 1366×768, desktop amplo, 780 px e 390 px; foco/teclado/Esc e sobreposição de modais; sessão/relogin; conta restrita; dados de produto simples/variante, PF/PJ e fornecedor; concorrência/idempotência e gravações demonstrativas autorizadas. As afirmações de preservação acima se baseiam na revisão de código, não em exercício manual desses fluxos. Vendas e Depósitos também não puderam ser reabertos para inspeção visual.

## Atualização após a abertura do navegador pelo usuário

Foi realizada revisão visual com a sessão de Administrador disponível, seguida de correções e revalidação pelo navegador. O registro anterior descreve a limitação da primeira entrega; os resultados atuais, as capturas e as limitações restantes estão em [2026-11-01-paginas-de-operacao-visual-review.md](2026-11-01-paginas-de-operacao-visual-review.md).

## Ajuste solicitado: Compras seguindo Registrar venda

Comparados no código e no navegador OrderEntryPage/Header, OrderProductCatalog, OrderCart e orderEntry.css com os componentes correspondentes de Compras. A solicitação do usuário definiu Vendas como referência direta de composição.

- Cadastro agora usa “Registrar compra”, cabeçalho e navegação no mesmo padrão de Vendas, com catálogo e resumo vazio disponíveis antes de iniciar. A criação continua dependendo exclusivamente do comando Iniciar compra, com fornecedor selecionado.
- Compra existente mostra resumo compacto de fornecedor/previsão. Alterar abre o formulário atual; cancelar volta ao resumo e o retorno à lista continua usando o descarte existente. Rascunhos de cabeçalho em memória são reabertos para revisão.
- Catálogo usa linhas com nome, marca, tipo e unidade, ação à direita e opções do fornecedor expandidas. O editor está dentro do catálogo e permanece independente da pesquisa/paginação.
- Resumo lateral segue OrderCart: itens, SKU/opção, depósito, comercial/BASE, custo, ações com ícones, total oficial, Manter rascunho/Acompanhar compra, atualização e metadados recolhíveis. Não foram copiados descontos, preços de venda, reservas nem transições de Vendas.
- Reutilizado o CSS existente de composição de Vendas, incluindo duas colunas conforme o espaço efetivo e resumo sticky somente quando cabe na altura. purchaseEntry.css mantém apenas as diferenças próprias de Compras. Os componentes e estilos de Vendas não foram modificados.

Conferências de navegador: cadastro de venda e compra comparados nas mesmas dimensões de 1366×768, com as mesmas larguras dos painéis; compra #3 consultada sem gravar; cabeçalho compacto e expansão/cancelamento; troca local de fornecedor com cancelamento do descarte preservando a escolha e descarte voltando ao fornecedor persistido; editor dentro do catálogo, quantidade 7 preservada ao pesquisar outro produto; saída pelo botão Compras com confirmação e preservação ao cancelar; layout em 780 px e 390 px empilhado e sem transbordamento horizontal do documento. O preenchimento automatizado do campo de data não alterou o valor nesta ferramenta; a conferência de descarte foi realizada pela seleção local de fornecedor.

Capturas salvas nos artefatos do chat: `compra-nova-padrao-vendas.jpg` e `compra-montagem-padrao-vendas.jpg`. Não foram salvos cabeçalhos/itens, iniciadas compras de inspeção, realizadas transições ou movimentado estoque. Backend, APIs, usePurchaseEntry e recebimento não foram alterados. Sem testes automatizados, build, lint ou commits. Verificação de gravações, permissões reduzidas e concorrência permanece fora desta conferência visual.

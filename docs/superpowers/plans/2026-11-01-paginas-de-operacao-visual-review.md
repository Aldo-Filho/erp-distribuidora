# Revisão visual das páginas de operação — 01/11/2026

Revisão solicitada após a implementação, usando a sessão já autenticada no navegador integrado. Conta observada: Administrador. Esta revisão complementa a inspeção de código e substitui a limitação de acesso ao navegador registrada na entrega inicial.

## Correções realizadas e revalidação

| Achado observado | Causa e correção | Conferência após a alteração |
| --- | --- | --- |
| Erro de ID inválido permanecia no histórico após corrigir o campo para vazio e aplicar novamente. | O envio válido apenas atualizava a URL; quando a URL não mudava, o componente não remontava e conservava o erro local. O envio válido agora limpa o erro explicitamente. | Repetida a sequência `-1 → Aplicar → vazio → Aplicar`: a mensagem desapareceu e a consulta permaneceu disponível. |
| Cadastro/edição repetiam o título da página dentro do formulário. | O conteúdo extraído do modal ainda desenhava seu próprio título. Adicionada opção de ocultar o título apenas no conteúdo embutido. | Novo produto, novo cliente e novo fornecedor observados com um único título. Invólucros de modal continuam com título. |
| Edição extensa exigia percorrer o formulário para encontrar o cancelamento. | Páginas de Produtos, Clientes e Fornecedores agora oferecem retorno no cabeçalho, acionando o mesmo descarte do formulário. | Retorno superior em produto alterado abriu confirmação; cancelar o descarte manteve o nome digitado; descartar retornou sem gravação. |
| Modal de endereço repetia “Editar endereço”; inclusão usava o título de edição. | O editor e o modal desenhavam títulos simultâneos. O modal passa a escolher Novo/Editar, e o título interno fica oculto nesse contexto. Nome repetido da pessoa também foi retirado da seção embutida. | Novo endereço de fornecedor e de cliente observados com título único e correto; Esc fechou sem envio. |
| Filtros de histórico ocupavam quase toda a altura do notebook. | A grade fixa de duas colunas foi substituída por colunas adaptadas ao espaço disponível. | No notebook, filtros em quatro colunas e início da tabela visível sem percorrer todo o painel. |
| Catálogo de compra exibia “Produtos do pedido”. | Texto ajustado para “Catálogo de produtos”. | Alteração restrita à apresentação, sem mudança de consulta ou inclusão. |

## Fluxos realmente observados

- Produtos: listagem, produto simples, ficha e seção de estoque, cadastro/edição sem salvar, seletor auxiliar de marca e Esc; nome digitado preservado ao fechar o seletor. Popover de estoque manteve os dois depósitos e saldos. Retorno à listagem conservou a página `2 de 2`.
- Compras: rascunho existente #3, cabeçalho, editor de item salvo, pesquisa de produto/opção do fornecedor, inclusão aberta sem envio, quantidade digitada preservada ao pesquisar outro produto, acompanhamento e quantidades contratadas/pendentes. Campos de inspeção foram descartados; nenhum item foi salvo.
- Histórico: resultados reais (67 movimentos no momento da inspeção), documento/operador, filtros, validação de ID e correção da mensagem persistente. Nenhuma paginação de servidor foi pressuposta.
- Fornecedores: listagem, seção de endereços, formulário pontual sem salvar, vínculos de marcas/categorias e cadastro sem salvar.
- Clientes: ficha PJ e tabela de preços, edição PF sem salvar, cadastro, endereços e tipos disponíveis no formulário pontual. Retorno manteve a busca `Balcao`.
- Vendas e Depósitos: listagens reabertas, com dados visíveis e sem transbordamento horizontal da página em 390 px. Operações comerciais dessas telas não foram executadas.
- Menu: recolhimento observado com largura final de 78 px; menu expandido/recolhido acessível no notebook e menu compacto no celular.
- URL inválida `/products/0`: exibiu identificador inválido.

## Responsividade e console

Foram usadas larguras CSS de 390, 780, aproximadamente 1366 (1365 por arredondamento do zoom do navegador) e 1920 px. As telas de cada fluxo acima foram inspecionadas em diferentes tamanhos; não foi executada uma matriz exaustiva de todas as páginas em todas as dimensões. Foram capturadas imagens e conferidas dimensões no DOM. Não foi observado transbordamento horizontal do documento nas telas medidas; tabelas mantêm sua rolagem própria.

O console já continha mensagens antigas de HMR sobre os modais removidos e o contexto de autenticação, anteriores a esta revisão. Não apareceram novos avisos/erros de console durante as navegações e revalidações realizadas. Isso não comprova ausência de erros em caminhos não exercitados.

Capturas da revisão: `historico-revisado.jpg` e `cliente-mobile-revisado.jpg`, salvas na pasta de artefatos de visualização deste chat.

## Limites

Não foram executados testes automatizados, build ou lint. Nenhum cadastro, item, transição, recebimento ou movimento foi gravado. Não foram alterados cargos, permissões ou backend, nem criados commits. O viewport foi restaurado ao tamanho original ao encerrar.

O catálogo observado continha produtos simples; variações existentes não foram exercitadas. A lista de compras observada oferecia rascunhos e compras enviadas; recebimento não foi habilitado por uma transição de inspeção. Permanecem sem validação prática: conta restrita a stock.read/sem prices.read, expiração e novo login, falhas de rede, respostas fora de ordem, concorrência, resultados desconhecidos e reconciliação/idempotência do recebimento. Esses pontos continuam dependendo da revisão direta de código e de uma conferência futura com os cenários apropriados.

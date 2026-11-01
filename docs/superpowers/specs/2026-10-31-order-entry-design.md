# Registrar venda: produtos e pedido lado a lado

Status: desenho escolhido pelo usuário; detalhamento proposto para implementação.

## Objetivo

Montar uma venda numa página ampla: buscar produtos, selecionar apresentação e depósito, adicionar vários itens, conferir preços e confirmar o pedido sem abrir um modal para cada inclusão. A alternativa escolhida foi catálogo à esquerda e pedido à direita. A alternativa de lista contínua permanece apenas como referência; em larguras estreitas, os dois painéis se empilham.

Referência visual: variante “Produtos e pedido lado a lado” do rascunho `registrar-venda.html` apresentado neste chat. Os produtos, preços e embalagens do rascunho são ilustrativos, não dados a copiar para o sistema.

## Escopo e navegação

- Manter Vendas como entrada no menu lateral e a listagem em `/orders`.
- “Nova venda” abre `/orders/new`; pedidos pendentes são montados em `/orders/:id/edit`.
- Manter a consulta atual para pedidos confirmados, despachados e cancelados, incluindo histórico e despacho. A listagem e seus filtros não são substituídos.
- Antes de criar o pedido, selecionar cliente e pagamento na própria página. A lista de produtos pode carregar, mas a inclusão depende de um pedido criado e do preço correspondente ao cliente.
- “Iniciar pedido” cria um único PENDING e substitui a URL por `/orders/:id/edit`. Nunca criar pedidos automaticamente a cada renderização ou mudança de cliente.
- Depois de adicionar um item, manter busca, página do catálogo, seleções e foco; atualizar itens e total do pedido, sem desmontar o catálogo.

## Organização da tela

- Cabeçalho compacto com número, situação, cliente, pagamento e depósito sugerido para novas inclusões.
- Catálogo à esquerda com busca por nome, SKU e características, paginação, identificação da marca, variações, apresentação, quantidade e ação de incluir.
- Pedido à direita com linhas salvas, quantidade comercial, equivalência BASE, depósito, preço, desconto, subtotal e total.
- Usar dados retornados pela API para os totais salvos. Prévias locais devem ser identificáveis e nunca substituir a resposta do servidor.
- Ao selecionar uma variação ou abrir “Preço e desconto”, expandir controles dentro da página. Manter o catálogo e o pedido acessíveis; não trocar a inclusão por outro modal.
- O depósito sugerido não altera itens existentes. Se só houver um depósito configurado e consultável para o SKU, pode ser selecionado automaticamente. Caso contrário, exigir escolha explícita válida.
- Em conteúdo disponível de pelo menos 1000 px, usar aproximadamente 55% catálogo e 45% pedido. Abaixo disso, empilhar catálogo e pedido, oferecendo atalhos “Ver pedido”/“Adicionar produtos” sem perder o estado. A largura efetiva considera a lateral expandida/recolhida.
- Resumo lateral pode ser sticky apenas quando couber na altura disponível. Em notebook baixo, usar rolagem da página para evitar painéis cortados e múltiplas barras obrigatórias.
- Manter paleta, ícones lucide-react, componentes e controles do JamsHub. Cancelamento permanece vermelho sólido; confirmações usam `useConfirm`.

## Regras e diferenças em relação ao rascunho

1. PENDING não reserva estoque. A consulta mostra físico, reservado e disponível reais, sem subtrair silenciosamente os itens do carrinho. É possível preparar uma demanda superior ao saldo quando isso já for permitido no fluxo atual; a confirmação continua validando disponibilidade no servidor. Falta de saldo não deve se tornar uma restrição nova de inclusão apenas por causa do rascunho.
2. A identidade de uma linha é SKU + apresentação + depósito. Repetir a mesma combinação deve localizar a linha existente. Oferecer “Adicionar à quantidade” via PATCH absoluto apenas com `orders.update`, preservando preço e desconto salvos. Não criar uma linha duplicada; sem permissão, indicar que a linha já existe. Com campos comerciais locais modificados, exigir revisão da linha existente, sem sobrescrevê-la.
3. Somar quantidade não recalcula automaticamente preços antigos. Desconto em valor continua sendo o valor salvo da linha; desconto percentual continua percentual. Mostrar o efeito no subtotal antes de aplicar.
4. Usar preço automático do cliente/apresentação para novas linhas e respeitar o preço salvo na edição. Trocar o cliente não reprecifica linhas existentes. Preservar consulta de preço vigente e aplicação explícita autorizada.
5. “Confirmar e reservar” não despacha nem emite NF-e. Despacho continua uma operação posterior, conforme as permissões e regras atuais.
6. “Manter pendente” mantém os itens já gravados; não desfaz a venda. Ao encerrar explicitamente uma edição, campos não enviados exigem salvar ou descartar. Outras navegações internas mantêm os rascunhos em memória para retomada, sem descartá-los silenciosamente. Não prometer persistência após recarregar para rascunhos que hoje existem somente em memória.

## Inventário de funcionalidades atuais e destino

| Funcionalidade identificada no código atual | Destino na nova experiência |
| --- | --- |
| Listagem, filtros de situação/pagamento, total e paginação | Continuam em `/orders`, com estado restaurado ao retornar. |
| Cadastro com cliente ativo, pagamento e operador autenticado | Cabeçalho inicial; mesma API de cadastro e permissões. |
| Trocar cliente/pagamento no pedido pendente | Cabeçalho editável; salvar alterações sem reprecificar itens antigos. |
| Buscar cliente com paginação e permissão `customers.read` | Seletor atual pode permanecer como diálogo auxiliar; não participa de cada inclusão de produto. |
| Catálogos simples e com variações; busca por SKU/características | Catálogo da página; expandir variações no próprio resultado. Não escolher um SKU arbitrariamente. |
| Apresentações ativas, paginação, fator e unidade BASE | Controles na linha expandida; manter todos os resultados acessíveis. |
| Escolha de depósito, físico/reservado/disponível | Depósito sugerido + opção por item; restringir a depósitos configurados quando o usuário puder consultar estoque. |
| Produto/apresentação inativos, unidade pendente e conversão alterada | Mesmas validações antes do envio e retorno de erro específico, sem apagar os outros itens. |
| Quantidade comercial e regras de precisão | Preservar unidade inteira para UN/embalagens e decimal permitido para apresentação base de outras unidades. |
| Preço automático segundo cliente/apresentação | API `orders.price`; não usar o preço base do catálogo como preço final. |
| Preço salvo, consulta de preço vigente, aplicar vigente, restaurar preço | Editor embutido com as mesmas permissões e intenção explícita. |
| Preço manual e retorno ao automático | “Preço e desconto”, condicionado a `orders.manualPrice`. |
| Desconto em valor ou percentual, bruto/desconto/líquido | Editor embutido, condicionado a `orders.discount`; preservar desconto salvo para quem não pode alterá-lo. |
| Editar quantidade, SKU, apresentação e depósito de uma linha | Linha do pedido com editor na própria página; mesmas permissões para alterar identidade. |
| Edição limitada sem `products.read`/`warehouses.read` | Manter os dados salvos/snapshots para campos permitidos; indisponibilidade do catálogo não bloqueia toda a página. |
| Duplicidade SKU + apresentação + depósito | Localizar linha e atualizar sua quantidade com permissão, conforme regra acima. |
| Remover item com confirmação | Ação da linha com `useConfirm` e `orders.delete`. |
| Montar pedido pendente, concluir edição e retomar posteriormente | Página de registro; retornar à consulta/listagem com dados atualizados. |
| Confirmar pedido e reservar estoque | Ação principal com confirmação, bloqueio durante gravações e revalidação no servidor. |
| Cancelar pendente/confirmado e liberar reservas | Cancelamento na consulta e na página se permitido; confirmação destrutiva e vermelho permanente. |
| Despachar confirmado e consultar saídas | Permanecem na consulta atual com `orders.dispatch`/`stock.read`. |
| Saldos e reservas do pedido | Manter `DocumentStock` acessível sem interferir na montagem. |
| Datas e metadados; snapshots de produto, apresentação e atributos | Resumo/detalhes do pedido e linhas existentes; não substituir histórico por dados novos do catálogo. |
| Proteção contra cliques duplos, erros e resultado incerto | Fila de escritas, estados de linha e reconciliação antes de repetir operações. |
| Rascunhos por usuário, descarte, expiração de sessão e erros traduzidos | Reutilizar infraestrutura; incluir retorno autenticado às novas rotas. |

## Estado, desempenho e integridade

- Separar estado do catálogo do estado do pedido. Uma inclusão não deve disparar novamente a pesquisa inteira nem todas as consultas de preço.
- Preservar debounce de 400 ms, cancelamento e proteção contra respostas antigas; cache limitado à página/sessão por catálogo/SKU e preço por pedido + cliente + SKU + apresentação. Invalidar ao trocar cliente, atualizar preço ou receber erro de cadastro alterado.
- Carregar variações/apresentações/estoque sob demanda. O catálogo aparece antes da conclusão das cotações; mostrar carregamento/erro por linha, nunca preço zero inventado.
- Limitar consultas auxiliares simultâneas; começar com até quatro. Não disparar consultas para todo o catálogo ou páginas invisíveis. Reutilizar o SKU resolvido de produto simples, evitando consultá-lo duas vezes ao selecionar.
- Serializar escritas do mesmo pedido e desabilitar confirmação/cancelamento enquanto houver envio ou resultado incerto. A busca pode continuar operável durante a gravação.
- POST de item e cadastro de pedido não possuem a mesma idempotência de movimentações de estoque. Em falha de rede, consultar o pedido e suas linhas e conferir o resultado; não repetir automaticamente nem recalcular um incremento cuja aplicação não foi confirmada.
- Em mudança de situação por outra sessão, atualizar o pedido, encerrar as escritas incompatíveis e preservar dados locais para revisão. Não apagar silenciosamente campos não enviados.
- Backend permanece responsável por preços finais, descontos, snapshots, disponibilidade, reservas e transições.

## Limites

Nenhuma funcionalidade atual inventariada será removida. A proposta não inclui emissão fiscal, recebimento financeiro, leitor de código de barras, nova auditoria, novos pacotes ou mudança de contratos de API. Otimizar primeiro o frontend com as APIs existentes; eventual endpoint agregado só com justificativa medida e escopo separado.

## Validação

Neste planejamento foram lidos código e contratos; nenhuma validação de navegador ou desempenho foi executada. Na implementação, respeitar AGENTS.md: não criar/executar testes automatizados, build ou lint sem pedido explícito. Usar revisão direta e uma conferência manual dos fluxos definidos no plano.

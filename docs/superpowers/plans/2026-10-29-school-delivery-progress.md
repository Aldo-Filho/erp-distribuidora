# Entrega escolar essencial — execução

Escopo aprovado no chat de origem: estoque/depósitos → venda/reserva/saída → compra/recebimento parcial. Checkout existente; alterações locais preservadas. Um executor, sem delegação.

Ruling: instruções expressas do usuário prevalecem sobre TDD, testes, build/lint, worktrees, commits e revisão delegada das skills. Evidência desta entrega: leitura e revisão estática própria. Não executar serviços, navegador, HTTP, SQL ou migrations.

## Progresso

- Contratos atuais, permissões, Git/status/diferenças, padrões Clientes/Fornecedores/Produtos e registros de validação anterior consultados.
- Estoque: escritos e integrados cadastro de depósito/endereço, configuração com saldo/reservado zero, limites, consulta de saldos e movimentos manuais com histórico.
- Venda: escritos e integrados lista, consulta, cadastro PENDING, edição de cabeçalho/quantidade/preço/desconto, inclusão/remoção de itens, confirmação/reserva, cancelamento/liberação, despacho e consulta de saldos/histórico.
- Compra: escritos e integrados lista, consulta, cadastro DRAFT, edição de cabeçalho/itens, opção comercial do fornecedor, SENT → CONFIRMED, cancelamento antes do recebimento, recebimento parcial/total BASE e recibos/histórico/saldos.

Ruling: cadastro do depósito e endereço são operações separadas conforme endpoints atuais; o depósito não exige endereço para existir. Formulário separado evita repetir cadastro caso endereço falhe. Complemento permanece obrigatório pelo contrato.

Ruling: não oferecer edição direta de saldo no PATCH de configuração; apenas mínimo/máximo. Estoque novo começa zero e entradas passam por stockMovement. Nenhuma alteração no backend ou schema prevista.

## Arquivos e integração

- Rotas `/warehouses`, `/stock`, `/orders`, `/purchases`, com entradas no menu e permissões de leitura correspondentes.
- `frontend/src/pages/Operations`: WarehousesPage, StockPage, OrdersPage, PurchasesPage, TradeItemForm, PurchaseReceiptForm, OperationSelectors, StockHistory, DocumentStock e operations.css.
- `frontend/src/services/operations.js`: chamadas autenticadas/CSRF pelo api.js atual; reaproveitado productStock.js para saldos por SKU.
- Extensões pontuais em AppRoutes.jsx, navigation.js e AppLayout.jsx; preservados todos os trechos preexistentes.
- Pagination.jsx: acrescentado somente type="button" aos dois botões, necessário para a paginação de apresentações dentro do formulário não disparar salvamento.
- Nenhum arquivo de backend, migration, credencial, configuração, coleção Insomnia ou teste alterado nesta entrega.

## Decisões e limites

- Ruling: reutilizar hooks de leitura/ação/formulários já existentes em Produtos, com identidades `school:*` distintas para os rascunhos. Sem nova dependência ou infraestrutura genérica. O prefixo interno de armazenamento em memória continua sendo o do hook atual; não interfere nos rascunhos de Produtos.
- Ruling: identidade do item (SKU/apresentação/depósito) fica fixa na edição simples. Quantidade, preço/custo e desconto podem mudar nos estados editáveis. Trocar identidade exige remover e adicionar item, com as permissões de exclusão e cadastro correspondentes. Evita alterar snapshots silenciosamente.
- Preço de venda omitido deixa a resolução cliente/apresentação no backend. Quantidades comerciais não são multiplicadas no navegador. Saldos disponíveis e pendências são subtrações somente de exibição, em décimos inteiros; saldos persistidos vêm da API.
- Cadastros legados com unidade pendente são bloqueados para novas quantidades. Opções de fornecedor sem conversão confirmada não podem ser escolhidas. A regularização explícita continua em Produtos; documentos históricos não ganham snapshots artificiais.
- Recebimento: tentativa exata (requestId, operador, notas e linhas BASE) gravada em sessionStorage por usuário/compra antes do envio; bloqueio de cliques concorrentes; erros de rede/servidor mantêm conteúdo imutável para repetição. Pode fechar, reabrir e recarregar na mesma aba. Falha no armazenamento impede iniciar o envio. Respostas de rejeição 400/403/404/409/422 liberam correção; 401 preserva tentativa para retomar após autenticação. Sucesso remove somente aquela tentativa. A recuperação local não atravessa encerramento da aba/navegador; recibos do servidor continuam consultáveis.
- Listas de estoque/depósitos/pedidos/compras/movimentos e itens usam endpoints legados List, sem paginação fictícia. Seletores de clientes/fornecedores/catálogos/SKUs/apresentações/opções usam paginação real existente. Filtros principais: SKU no estoque, nome no depósito e situação em vendas/compras.
- Sem linha do tempo inventada de transições: situação atual, metadados, snapshots de itens, recibos e movimentos são os dados disponíveis. Cancelar reserva não gera saída de estoque; conferir liberação em Saldos e reservas.
- Permissões: leitura em cada rota; transições de venda exigem orders.update + orders.confirm/cancel; compra exige purchases.update + purchases.send/confirm/cancel. Despacho/recebimento usam orders.dispatch/purchases.receive. Consulta dos seletores exige as leituras de cadastros correspondentes. Backend mantém sua proteção.

## Revisão estática própria

- Rotas, métodos HTTP, nomes de payloads/enums, precisões de ProductQuantity, filtros e permissões confrontados com controllers/services/DTOs atuais. Componentes e estilos confrontados com Clientes/Fornecedores/Produtos.
- Revisados preservação dos dados em erro, descarte por useConfirm, bloqueio de envio duplicado, cancelamento das consultas, estado de carregamento sem saldo zero inventado, fluxo de repetição de recibo e inclusão/remoção de itens.
- Corrigidos durante a revisão: classes de cabeçalho/ações para o padrão atual, botões de paginação sem submissão, restauração de apresentação do rascunho, descarte de seleção ao mudar página e dependência dos depósitos na consulta de saldos.
- Prettier local aplicado somente aos arquivos novos desta entrega. A leitura do formatador não é build, lint, teste ou execução da aplicação.
- Não foram criados/executados testes, build, lint, serviços, navegador, HTTP, SQL, migrations, instalações, commits, push/deploy, auditoria ou Insomnia. Revisão própria não comprova funcionamento em execução.

## Roteiro curto para cadastrar e demonstrar

Os dados abaixo são sugestões para cadastro manual; nenhum foi inserido automaticamente. Valores esperados pressupõem SKU novo e ausência de outros movimentos.

1. Entrar com conta autorizada. Em Depósitos, cadastrar `Depósito demonstração`. Endereço é separado e opcional para este fluxo; se informado, preencher todos os campos exigidos.
2. Em Produtos, cadastrar um produto simples em UN com preço de venda R$ 10. Usar cliente e fornecedor ativos cadastrados pelas telas existentes. Se desejar demonstrar compra por embalagem, vincular o fornecedor e criar opção `Caixa 6`, conversão explicitamente confirmada com fator 6 e custo R$ 30 por caixa.
3. Em Estoque, Configurar item: selecionar o SKU e o depósito, mínimo 0, máximo vazio. Começa físico/reservado/disponível 0/0/0. Em Entrada ou ajuste, entrada manual de 20 UN; conferir histórico e 20/0/20.
4. Em Vendas, Novo pedido, selecionar o cliente, salvar. Adicionar o SKU, apresentação BASE, mesmo depósito, quantidade comercial 3. Deixar preço em branco para preço automático; o total é o retornado pelo backend (pode variar com tabela do cliente).
5. Confirmar e reservar. Abrir Saldos e reservas: esperado físico 20, reservado 3, disponível 17. Despachar: esperado 17/0/17. Consultar Histórico de saída: movimento SALE associado ao pedido, anterior 20, novo 17. `Despachado` representa INVOICED comercial, sem NF-e.
6. Opcional para mostrar cancelamento: novo pedido de 2, confirmar, consultar reserva, cancelar e conferir retorno a 17/0/17, sem saída física adicional.
7. Em Compras, Nova compra, escolher o fornecedor da opção. Adicionar SKU, Caixa 6, depósito e quantidade comercial 2, custo em branco. Esperado contrato de 12 UN BASE e R$ 60 se custo da opção permanecer R$ 30. Registrar envio → Confirmar compra.
8. Receber em BASE: informar 5, registrar. Compra parcialmente recebida, recebido 5 e pendente 7; saldo físico esperado 22. Consultar recibo, entradas e saldos. Receber as 7 restantes: compra recebida, pendente 0, físico 29. Não informar 2 caixas no recebimento BASE.
9. Em resposta incerta de recebimento, usar Retomar recebimento/Repetir a mesma tentativa e conferir recibos antes de iniciar outra entrada.

## O que ainda precisa ser verificado

Executar manualmente este roteiro quando autorizado, com backend/ambiente disponíveis; conferir renderização e teclado em desktop/tela estreita, todas as requisições/serialização, persistência, permissões negativas, preço por cliente/apresentação, frações, rejeição por falta de saldo, snapshots de documentos contratados, recibo repetido e recuperação após perda de conexão. Nenhum desses cenários foi marcado como executado nesta entrega. A validação anterior de Produtos não valida automaticamente estas novas telas.

Financeiro, fiscal, dashboard, transferências, relatórios avançados e demais exclusões do plano aprovado permanecem fora do escopo. Sem declaração de prontidão de produção.

## Revisao do fluxo de Vendas — 30/10/2026

- Criacao: "Criar pedido e adicionar produtos" explicita a gravacao PENDING e abre a montagem. Lista oferece consulta e edicao separadas; o modo de consulta nao mostra botoes de alterar/remover itens.
- Edicao: "Alterar cliente e pagamento" descreve o escopo do formulario. PATCH do cabecalho envia somente diferencas e nao recalcula itens existentes; aviso aparece ao trocar cliente. Formularios sem alteracoes nao geram PATCH.
- Itens: adicionar/trocar produto tem rotulo e campo de leitura; em venda pendente, produto, apresentacao e deposito passam a ser editaveis pelo PATCH existente. Isso substitui, somente para Vendas, a decisao anterior de remover/adicionar para trocar identidade. IDs sao enviados apenas quando mudam; quantidade comercial e snapshots continuam tratados pelo backend. Preco omitido preserva o valor salvo sem troca de identidade comercial, ou usa a resolucao existente ao trocar produto/apresentacao. Compras conserva sua identidade fixa na edicao.
- Acoes: adicionar primeiro produto no estado vazio; adicionar outros no cabecalho dos itens; confirmar/reservar no rodape apenas com itens e permissoes; despachar apenas em CONFIRMED; historico de saida apenas em INVOICED. Cancelar pedido fica separado de Fechar/Concluir edicao, com confirmacao vermelha.
- Mensagens especificam o resultado de adicionar/editar/remover, confirmar, cancelar e despachar. Salvamentos de cada formulario continuam imediatos; Concluir edicao fecha a tela, sem um segundo salvamento ou confirmacao do pedido.
- Revisao estatica dos consumidores, permissoes, payloads e estados. Sem alteracoes no backend, banco, migrations, testes, build, lint, servidor ou navegador. Ainda verificar em execucao: criar e adicionar varios itens; fechar mantendo PENDING; trocar somente pagamento; editar quantidade e trocar SKU/apresentacao/deposito; conferir preco automatico/manual e descontos; confirmar, cancelar e despachar com as permissoes respectivas.

### Revisao funcional adicional — produto/apresentacao

- A mensagem original reune duas rejeicoes diferentes; sem executar a requisicao e consultar o registro, nao foi comprovado qual delas ocorreu no print. ProductPresentationService agora diferencia apresentacao de outro SKU de apresentacao inativa. O frontend traduz essas e outras rejeicoes operacionais para portugues.
- Corrigidos riscos confirmados no formulario: respostas de produto/apresentacoes/saldos so podem ser usadas no SKU/pagina de origem; troca de SKU limpa deposito, apresentacao e preco manual; com stock.read, deposito sem configuracao fica desabilitado; duplicidade da combinacao SKU/apresentacao/deposito e informada antes do envio. Nao se bloqueia pedido pendente apenas por quantidade acima do disponivel; a reserva continua sendo conferida atomicamente na confirmacao.
- Antes de salvar uma venda, com permissao products.read, a apresentacao e consultada novamente para conferir SKU, atividade e conversao. Inconsistencia, inativacao ou exclusao recarrega as opcoes e exige nova escolha, preservando os demais campos. A validacao final continua no backend.
- Encontrada e corrigida lacuna no backend: OrderService nao revalidava a atividade/pertencimento da apresentacao em PENDING → CONFIRMED. Agora o faz sob os locks existentes, sem alterar snapshots/precos e sem exigir atividade para despachar um pedido ja contratado. Sem alteracao em schema/migrations.
- Edicao de quantidade/preco/desconto de item existente pode usar o produto ja retornado no pedido quando products.read estiver ausente; trocar identidade exige as consultas necessarias. SIMPLE com unico SKU ativo nao exige segundo clique no seletor.
- Revisao direta e formatacao localizada; nenhum teste, build, lint, SQL, HTTP, servidor ou navegador executado. A revisao nao encerra a comprovacao funcional do modulo: ainda precisam ser exercitados o erro original com seus dados reais, preco de cliente/apresentacao, fracionamento, inativacao concorrente, reserva agregada, repeticao de operacoes e recuperacao de falha de rede.

### Precos visiveis na montagem da venda

- Nova consulta GET `/order/{id}/price?productId=...&presentationId=...` retorna somente o valor monetario, com orders.read pela regra atual de SecurityConfig. Leitura transacional, sem reservar/gravar; usa ProductPriceService existente, sem duplicar precedencia da tabela do cliente, apresentacao e preco do SKU. ID de apresentacao omitido consulta a BASE.
- Seletor de Vendas mostra preco BASE do cliente para produtos simples e cada SKU de variacao; agrupadores VARIANT indicam acesso aos precos por variacao, sem inventar preco unico. Leituras cancelaveis nos registros da pagina atual; erro nao vira preco zero. Os seletores de Estoque/Compras mantem seu comportamento anterior.
- Apresentacao BASE e selecionada automaticamente ao adicionar um produto novo quando disponivel. A apresentacao escolhida preenche o campo de preco via consulta do backend. Edicao sem troca de identidade mostra o preco ja salvo. Uma resposta tardia nao sobrescreve digitacao manual, e a consulta automatica nao torna o formulario alterado sozinha.
- Preco automatico e mostrado inclusive sem orders.manualPrice, em campo somente leitura. Com permissao, digitar troca para modo manual; botao permite restaurar o automatico/valor salvo. Somente sobrescrita manual envia unitPrice: preenchimento automatico nao exige permissao de preco manual nem congela/regrava contratos silenciosamente. O servidor resolve o valor novamente no salvamento automatico.
- Revisao estatica e formatacao localizada; sem testes, build, lint, banco, HTTP ou navegador. Endpoint Java precisa estar carregado no backend antes de validar a interface.

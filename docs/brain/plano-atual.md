# Plano ativo — Usabilidade do JamsHub

**Estado:** plano aprovado. Somente a Etapa 1 está autorizada para implementação, em outro chat; sua conclusão não foi confirmada. Esta revisão documental não inicia alterações no código.
**Objetivo:** comerciante iniciante consegue identificar a ação seguinte, entender os efeitos e concluir a tarefa com poucos cliques.
**Já aplicado separadamente:** organização do menu, botão Menu no celular e ajustes do dashboard padrão. Validar o resultado atual antes de repetir trabalho; o restante da Etapa 6 permanece pendente.

## Direção

- Uma ação principal por momento; voltar, editar e cancelar com nomes inequívocos.
- Nomes de pessoas, produtos, depósitos e caixas em destaque; códigos como informação secundária.
- Campos essenciais primeiro e detalhes opcionais recolhíveis, preservando valores.
- Resolver dependências dentro da tarefa, reutilizando formulários e preservando rascunho, itens, filtros e seleção.
- Uma confirmação informativa por decisão importante, com efeitos financeiros/estoque e restrições explícitos.
- Linguagem simples, sem erros internos ou em inglês; distinguir rejeição de negócio de falha de rede.
- Preservar paleta, tema, atalhos úteis e componentes existentes. Sem assistentes, tutoriais ou novas telas quando o próprio fluxo puder resolver.
- Teclado, foco e celular fazem parte do aceite de cada etapa.

## Achados cobertos

| Nº | Melhoria | Prioridade | Etapa |
| --- | --- | --- | --- |
| 1 | Explicar e resolver dependências da primeira compra; traduzir erro de estoque | Alta | 2 |
| 2 | Nomes e busca por nome no financeiro, em vez de códigos | Alta | 3 |
| 3 | Orientar cadastro de caixa antes do pagamento/recebimento | Alta | 3 |
| 4 | Explicitar efeitos da confirmação da compra e restrição após despacho | Alta | 4 |
| 5 | Remover segunda confirmação da venda | Média | 4 |
| 6 | Simplificar termos/campos e preencher fator unitário | Média | 2, 3, 5 |
| 7 | Menu móvel com nomes; implementação aplicada, aceite pendente | Média | 6 |
| 8 | Selecionar depósito por nome no painel | Média | 5 |
| 9 | Após estorno, orientar cancelamento e explicar impedimento de despacho | Alta | 1 |
| 10 | Mostrar recebimento pendente em caixas e unidades | Média | 5 |
| 11 | Quantidade independente por embalagem de compra | Média | 1 |

## Etapa 1 — Orientações e quantidades das operações

**Autorizada; conclusão não confirmada. Esforço: pequeno a médio.**
- [ ] Usar estado/motivo confiável do servidor para reconhecer estorno, inclusive no retorno do financeiro e na recarga; não inferir elegibilidade apenas pelo status Aberta.
- [ ] Mostrar “Recebimento estornado — cancele esta venda”; retirar despacho/nova baixa quando proibidos e apresentar cancelamento conforme permissão.
- [ ] Explicar rejeições conhecidas, preservando recuperação/idempotência quando a resposta realmente se perder.
- [ ] Separar quantidade por SKU/opção do fornecedor; adicionar, configurar ou editar uma embalagem não muda as outras, inclusive no leitor.
**Áreas:** Operations/OrderEntryPage.jsx, useOrderEntry.js, salesErrors.js, PurchaseProductCatalog.jsx; Finance/DocumentFinancialLink.jsx e FinancialTitleDetails.jsx; respostas da API se necessário.
**Aceite:** estorno aponta para cancelamento, sem recomendar despacho; cancelamento libera reservas. Uma caixa e duas unidades podem ser preparadas separadamente. Reenvio permanece protegido.

## Etapa 2 — Primeira compra no próprio fluxo

**Pendente. Prioridade alta; esforço médio.**
- [ ] Identificar antecipadamente falta de vínculo, opção ativa, conversão ou produto no depósito, conforme fornecedor/SKU/depósito selecionados.
- [ ] Oferecer o formulário existente com contexto preenchido; após salvar, atualizar e retomar a compra com quantidade e rascunho preservados.
- [ ] Configurar estoque zerado apenas por ação explícita; cancelar não grava. Sem permissão, explicar a configuração que deve ser solicitada ao administrador.
- [ ] Traduzir erros e manter validação de servidor se o estado mudar entre consulta e inclusão.
- [ ] Compra por unidade preenche/fixa equivalência 1; embalagem composta informa quantidade real.
**Áreas:** Operations/PurchaseProductCatalog.jsx, PurchaseEntryPage.jsx, TradeItemForm.jsx; Products/ProductSuppliers.jsx, ProductSupplierOptionForm.jsx, ProductStockPanel.jsx.
**Aceite:** preparar produto recém-criado e incluir na compra sem procurar outra tela, perder dados ou criar entrada de mercadoria pela configuração zerada.

## Etapa 3 — Financeiro compreensível

**Pendente. Prioridade alta; esforço médio.**
- [ ] Mostrar cliente/fornecedor, caixa e operador por nome; manter códigos secundários e ausência de identificação histórica explícita.
- [ ] Buscar pessoas por nome e consultar origem dos lançamentos conforme permissão; preservar paginação, filtros por documento e totais do servidor.
- [ ] Se necessário, enriquecer respostas/pesquisa com identificação mínima do contexto financeiro, sem uma requisição por linha nem exigir acesso aos cadastros completos ou ampliar permissões.
- [ ] Usar “Registrar pagamento/recebimento”, “Valor total” e “Data do pagamento/recebimento”; manter operação integral.
- [ ] Nomear a conta por direção: “Saiu de qual caixa ou conta?” / “Entrou em qual caixa ou conta?”.
- [ ] Sem conta ativa, orientar e permitir cadastro pelo formulário existente conforme permissão, selecionando a nova conta e retomando o lançamento. Diferenciar lista vazia de falha na carga.
**Áreas:** Finance/FinanceTitlesPage.jsx, FinancialTitleDetails.jsx, CashFlowPage.jsx, BankAccountsPage.jsx, financeLabels.js; backend/financial e serviços frontend.
**Aceite:** identificar de quem cobrar/a quem pagar e em qual caixa sem memorizar códigos; usuário restrito mantém seu escopo e cadastro retoma o lançamento.

## Etapa 4 — Confirmações objetivas

**Pendente. Prioridade alta nos efeitos, média na redução de cliques; esforço pequeno a médio.**
- [ ] Unificar a venda no formulário de pagamento/vencimento; mostrar cliente, total, método e efeitos. Ação final: “Confirmar venda e reservar estoque”.
- [ ] Na compra, informar valor/vencimento da conta a pagar e que estoque entra somente ao receber.
- [ ] No despacho, explicar saída do estoque, impossibilidade de cancelar depois nesta versão e documento demonstrativo sem validade fiscal.
- [ ] Destacar próximo passo e manter secundárias as demais ações; evitar explicações repetidas e preservar bloqueio durante envio, idempotência e recuperação.
**Áreas:** Finance/OrderConfirmationForm.jsx; Operations/OrderEntryPage.jsx, PurchaseDetailsContent.jsx e confirmações.
**Aceite:** uma confirmação cria uma conta e uma reserva; voltar não grava; usuário conhece os efeitos antes da decisão.

## Etapa 5 — Formulários e consultas simples

**Pendente. Prioridade média; esforço médio.**
- [ ] Usar “Código do produto” e “Unidade de controle do estoque”; apresentar equivalências concretas, mantendo siglas úteis como apoio.
- [ ] Separar dados essenciais de peso, dimensões e características opcionais. Nomear comprimento/largura/altura e unidade somente após confirmar a convenção existente; não reinterpretar medidas salvas.
- [ ] Mostrar comprado/recebido/pendente na embalagem e na unidade de estoque; avulsos mantêm equivalência exata, sem arredondar caixas.
- [ ] Exibir prévia “Receber 1 caixa = adicionar 6 unidades ao estoque”, preservando excesso/histórico.
- [ ] Selecionar depósito por nome e opção Todos no painel, preservando filtros salvos e permissões; substituir rótulos internos por nomes curtos com explicação contextual.
**Áreas:** Products/ProductForm.jsx, ProductItemFields.jsx, ProductSupplierOptionForm.jsx, ProductPresentationForm.jsx; Operations/PurchaseReceiptForm.jsx; Dashboard/DashboardFilters.jsx.
**Aceite:** duas caixas recebidas em duas entregas e quantidades avulsas mantêm precisão; campos opcionais preservam valores; depósito não exige código.

## Etapa 6 — Celular, teclado e aceite integrado

**Parcialmente aplicada no menu; aceite pendente. Prioridade média; esforço médio.**
- [ ] Validar o menu móvel existente: nomes completos, fechar por destino/Escape, retorno de foco e ausência de cortes/sobreposição.
- [ ] Conferir foco visível, ordem de Tab, Enter/Espaço e seletores; corrigir apenas lacunas do comportamento atual.
- [ ] Percorrer cadastro → configuração → compra → recebimento parcial → venda → financeiro/estorno/cancelamento, incluindo retorno às buscas, em desktop e 390 px.
- [ ] Manter tabelas com ações/contexto acessíveis; rolagem horizontal pode existir quando necessária.
- [ ] Validar prévia/impressão em navegador compatível, itens/totais/paginação/aviso demonstrativo; não tratar tentativa inconclusiva como defeito.
- [ ] Validar instalação nova e empresa inicialmente vazia em banco isolado, sem desvincular emissor principal; revalidar CEP/edição e snapshots emitidos.
**Áreas:** components/layout/AppLayout.jsx, index.css e estilos afetados; Fiscal/Settings conforme validação.
**Aceite:** encontrar módulos pelos nomes no celular e concluir tarefas centrais por teclado, sem novo tutorial/assistente.

## Execução e limites

- Executar uma etapa por vez e parar antes da seguinte, salvo autorização posterior. Este plano não autoriza testes automatizados, build ou lint.
- Quando solicitadas, verificações direcionadas cobrem elegibilidade após estorno, quantidade por opção, confirmação única e retomada de configuração. Integração automatizada e instalação limpa usam banco novo exclusivo.
- Preservar alterações locais, registros existentes, credenciais fora dos arquivos e dados de teste; não limpar automaticamente.
- Não mudar reserva, conversões, precisão, recebimento parcial, baixa integral, estorno antes do cancelamento, permissões ou proteção contra repetição.
- Não adicionar parcelamento, devoluções, encerramento de saldo parcial, emissão fiscal real, pagamentos externos ou mudanças nas migrations.
- Atualizar documentação somente para contratos, dados, regras ou decisões realmente alterados. Pendências gerais de validação ficam no contexto; prioridades no roadmap.


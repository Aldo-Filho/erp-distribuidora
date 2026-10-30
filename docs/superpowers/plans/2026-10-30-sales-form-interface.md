# Interface de criação de venda — plano de implementação

**Status:** implementação realizada; revisão estática concluída. Conferência manual no navegador pendente.

**Objetivo:** tornar a criação de venda clara, com menos informação simultânea e ações inequívocas.

**Abordagem ajustada:** manter um fluxo simples no formulário atual. Ao adicionar um item, exibir busca e lista de produtos no próprio formulário, sem abrir outro modal de seleção. Após selecionar, mostrar quantidade, apresentação, depósito, preço e total no mesmo lugar. Dados, itens e resumo são seções claras, sem criar um assistente obrigatório com novas etapas.

**Tecnologias:** React, lucide-react e estilos existentes do JamsHub.

## Limites

- Aplicar AGENTS.md e preservar alterações locais.
- Somente frontend de vendas; preservar contratos, permissões, cálculos, rascunhos e regras de estoque.
- Não criar uma operação única de salvamento: o backend atual cadastra pedido e itens separadamente.
- Não criar/executar testes, build ou lint sem solicitação explícita. Sem commits ou deploy.
- Não alterar a interface de compras ao modificar o formulário compartilhado.

## 1. Dados da venda

**Arquivo:** `frontend/src/pages/Operations/OrdersPage.jsx` — `OrderHeaderForm`.

- [x] Destacar cliente e forma de pagamento, com resumo compacto do cliente selecionado.
- [x] Usar “Continuar para produtos” e explicar brevemente que essa ação salva uma venda pendente.
- [x] Manter seleção e troca de cliente, validações e tratamento de falhas existentes.

## 2. Produtos e resumo

**Arquivo:** `frontend/src/pages/Operations/OrdersPage.jsx` — `OrderDetails`.

- [x] Exibir resumo de cliente/pagamento, lista de itens e total em hierarquia clara, sem barra de etapas desnecessária.
- [x] Destacar “Adicionar produto”; tornar edição e remoção ações secundárias.
- [x] Separar visualmente ações do pedido das ações dos itens; manter consulta de estoque acessível.
- [x] Informar o estado vazio e o próximo passo sem textos técnicos ou repetitivos.

## 3. Inclusão de item

**Arquivos:** `frontend/src/pages/Operations/TradeItemForm.jsx` e `frontend/src/pages/Operations/OperationSelectors.jsx`, apenas apresentação de vendas.

- [x] Substituir o botão que abre `SkuPicker` por busca e lista paginada de produtos dentro do formulário de item. Reutilizar as consultas existentes e preservar permissões.
- [x] Selecionar produto simples diretamente; exibir variações no mesmo espaço quando necessário, sem outra janela.
- [x] Após selecionar, mostrar resumo compacto do produto e campos de preenchimento; permitir trocar o produto reabrindo a lista no mesmo formulário.
- [x] Mostrar opções de apresentação e depósito conforme a necessidade; quando houver uma única opção válida, pré-selecioná-la e mantê-la visível, respeitando rascunhos e edição.
- [x] Agrupar em Produto e apresentação; Depósito e saldo disponível; Quantidade e preço.
- [x] Deixar a origem do preço clara. Apresentar alteração manual e desconto como opções secundárias, conforme permissões.
- [x] Destacar total do item; mostrar conversão para unidade base junto à quantidade e detalhes de saldo sob demanda.
- [x] Preservar preço contratado, duplicidade de itens, bloqueios durante salvamento e dados ao cancelar descarte.

## 4. Revisão e ações finais

**Arquivo:** `frontend/src/pages/Operations/OrdersPage.jsx`.

- [x] Apresentar cliente, pagamento, itens, descontos e total antes de confirmar.
- [x] Diferenciar “Fechar e manter pendente” de “Confirmar venda”; confirmação mantém a reserva atual do estoque.
- [x] Manter despacho como operação posterior explícita. Falhas devem preservar os dados e indicar como continuar.

## 5. Estilos e revisão

**Arquivo:** `frontend/src/pages/Operations/orders.css`.

- [x] Aplicar estilos restritos às vendas e responsividade, reutilizando Modal, FilterSelect e confirmações atuais.
- [x] Revisar diretamente permissões, carregamento/erro, salvamento duplicado, navegação entre etapas e persistência parcial.
- [ ] Na conferência manual, verificar busca, paginação, variações e troca de produto no mesmo formulário, teclado, telas estreitas, adicionar/editar/remover itens e retorno à venda pendente. Relatar o que efetivamente foi verificado.

**Critério de conclusão:** o usuário busca, seleciona e configura o produto sem sair do formulário de item; identifica o total e a situação da venda sem confundir inclusão de itens, confirmação e despacho.

## Registro da implementação

- Busca e lista de produtos integradas ao formulário de item; variações no mesmo espaço e seleção automática do SKU simples.
- Resumo do produto, total destacado, preço/desconto recolhidos e foco na quantidade após a seleção.
- Pré-seleção de depósito único quando há permissão e consulta de estoque concluída; valores de rascunhos e identidade de itens existentes são preservados.
- Compras continuam usando o seletor em modal. Nenhuma alteração no backend.
- Revisão adicional somente leitura não identificou erros concretos de JSX ou regressões em compras/permissões. Sem testes, build, lint, commits ou deploy.
- Decisão: manter o checkout atual e seguir a preferência explícita de revisão direta, sem testes automatizados.

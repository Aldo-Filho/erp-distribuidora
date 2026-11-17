# Regras de negócio vigentes

## Venda e financeiro

- Confirmar uma venda cria uma única conta a receber aberta, qualquer que seja o método de pagamento.
- Cada venda usa exatamente um método: Pix, boleto demonstrativo, dinheiro, cartão de crédito, cartão de débito ou transferência bancária.
- O pagamento é uma única baixa manual integral. Não há integração bancária, parcelamento, pagamento parcial ou confirmação automática nesta etapa.
- Dinheiro, Pix, débito e transferência vencem na confirmação; crédito e boleto recebem vencimento escolhido na confirmação.
- O boleto é demonstrativo, não processável e não usa linha digitável ou código de barras bancário válido.
- Um pedido pode ser cancelado somente antes do despacho. Se já estiver pago, o usuário precisa registrar um estorno manual antes de cancelar.
- O estorno preserva o recebimento original e cria uma saída financeira vinculada, com data e motivo. A conta volta a aberta para cancelamento; não admite uma segunda baixa nem despacho após o estorno. Uma nova operação exige um novo documento.
- Pedido despachado não pode ser cancelado. Devoluções não fazem parte desta versão.

## Integridade de clientes e estoque

- Clientes PF usam CPF e PJ usam CNPJ, com dígitos verificadores válidos em documentos novos ou alterados; CNPJ admite o formato numérico e o alfanumérico. Máscara e caixa não distinguem identidades. A validação é local e não comprova situação cadastral na Receita.
- Documentos antigos e fictícios não são substituídos automaticamente; alterações em outros campos não obrigam reenviar um documento inalterado.
- Configurar estoque com saldo zero exige `stock.configure`. Informar saldo inicial positivo exige também `stock.adjust` e cria um movimento de entrada com operador da sessão, saldo anterior zero e saldo final informado, na mesma transação do cadastro.
- Um item de estoque com saldo ou reserva não pode ser excluído nem mudar de produto/depósito; continuam valendo os bloqueios por movimentos e reservas históricas.

## Compras e estoque inteligente

- Compras confirmadas geram uma conta a pagar; a baixa é manual, única e integral. O vencimento usa a data prevista de entrega quando futura, ou o dia da confirmação. A categoria de despesa padrão é configurável.
- Compra paga só pode ser cancelada após estorno integral registrado, permitido antes de qualquer recebimento. O estorno mantém o pagamento, registra entrada na conta original e impede receber mercadorias ou pagar novamente esse documento; segue-se o cancelamento.
- A sugestão de compra é calculada por SKU e depósito com vendas dos últimos 90 dias, estoque disponível e reservado, mínimo do depósito, prazo de reposição e sete dias de segurança.
- A quantidade-alvo é o maior valor entre o mínimo configurado e o consumo previsto durante prazo de reposição mais segurança.
- O fornecedor preferencial ativo é usado primeiro; sem ele, usa-se a opção ativa de menor custo.
- O sistema gera rascunhos de compra agrupados por fornecedor. Nunca envia, confirma ou recebe uma compra automaticamente.
- Uma nova geração considera o saldo ainda não recebido das compras geradas em rascunho, enviadas, confirmadas ou parcialmente recebidas, usando os itens atuais; canceladas e recebidas não cobrem nova reposição. São criados novos rascunhos somente para a falta residual, preservando edições anteriores.
- Opções são comparadas pelo custo por unidade BASE, respeitando o fornecedor preferencial ativo elegível. Quantidades sobem até a apresentação comprável; prazo ausente usa zero mais os sete dias de segurança. Sem opção ativa com conversão confirmada, a necessidade é exibida sem gerar compra.

## Precisão e recebimento de compras

- O recebimento comercial informa apresentações inteiras maiores que zero. A alternativa BASE permite unidades avulsas: UN inteira; demais unidades com até uma casa decimal. Não se arredonda quantidade inválida silenciosamente. Limite operacional de quantidade BASE: 9999999999999,9; a quantidade recebida não pode exceder o pendente.
- O fator e a unidade contratados são snapshots do item; alterar a opção atual do fornecedor não modifica a conversão de um recebimento posterior. A entrada avulsa em BASE continua disponível também para apresentações BASE fracionáveis.
- Cada recebimento parcial/final mantém seu recibo e seus movimentos. A tentativa repetida com o mesmo UUID e as mesmas quantidades equivalentes em BASE retorna o recibo já registrado.
- `Registrar envio` é um registro interno e não envia mensagem ao fornecedor. A entrada no estoque acontece no recebimento. O encerramento do restante não entregue após recebimento parcial continua adiado; recibos e movimentos existentes não são excluídos ou estornados automaticamente.

## Código de barras

- Código de barras pertence ao SKU, não ao catálogo de produto.
- EAN/GTIN e identificadores internos precisam ser únicos. O código permanece reservado ao SKU original mesmo inativo, sem transferência ou reutilização; letras maiúsculas/minúsculas e espaços externos não criam códigos distintos. Zeros iniciais são preservados.
- EAN/GTIN exige comprimento e dígito verificador válidos. Cada SKU tem no máximo um código principal ativo; inativá-lo remove a marca de principal.
- Leitores funcionam como teclado: os fluxos de venda, compra e estoque resolvem o código para um SKU ativo.

## Aparência e dashboard

- Logo e paleta são configurações globais da empresa.
- O sistema oferece de cinco a dez paletas predefinidas compatíveis com a identidade visual.
- Tema claro, escuro ou conforme o sistema é preferência individual do usuário; mudar o tema não altera o tema dos colegas nem encerra sessões.
- Somente `company.appearance.manage` pode alterar a paleta ou substituir/remover o logo; a aparência é compartilhada por todos os usuários autenticados. Paleta e logo não alteram a identidade fiscal/cadastral existente da empresa.
- O dashboard é a página inicial e mostra indicadores definidos pelo cargo, sempre limitados pelas permissões efetivas.
- Cada usuário pode escolher, ocultar, ordenar e redimensionar blocos de um catálogo, salvar sua organização e restaurar o padrão inicial por perfil. Não há editor de fórmulas ou métricas arbitrárias. A personalização não concede permissões.
- No dashboard personalizável, faturamento inclui vendas confirmadas e despachadas, pela data de confirmação; rascunhos e canceladas ficam fora. Recebimentos e pagamentos são indicadores separados, pela data da movimentação financeira. Vendas históricas sem data confiável ficam fora das métricas por período, com aviso de cobertura. Cancelamento retira o valor do período original; o painel representa a situação atual.
- O painel oferece filtros por dia, semana, quinzena, mês, ano e intervalo personalizado, comparação com período anterior, ranking de produtos por quantidade/faturamento e preço médio de venda por produto. Os conjuntos iniciais combinam financeiro, vendas, compras e estoque conforme permissões efetivas. Preço médio é ponderado por quantidade BASE e unidade; valores de venda exigem também consulta financeira. Implementação ainda depende de aceite dinâmico.

## Fiscal e integrações externas

- A NF-e demonstrativa é emitida no despacho, usa snapshots e mostra em todas as partes que não possui validade fiscal. A emissão é única por venda e participa da mesma transação do despacho; falha reverte documento e baixa de estoque. Retry não duplica nem cria emissão retroativa para vendas antigas.
- O documento preserva dados e valores capturados no despacho, inclusive características e quantidades decimais; mudanças nos cadastros não alteram a cópia emitida. Dados ausentes são explicitados. Sem emissor fiscal configurado, a demonstração mostra o aviso e não inventa identificação nem adiciona bloqueio ao fluxo operacional.
- Consulta e impressão do demonstrativo exigem `orders.read`; emissão interna segue `orders.dispatch`. O documento não comprova pagamento nem apura tributos.
- Não serão geradas chave de acesso, autorização, QR code ou comunicação real com a SEFAZ.
- A arquitetura deve deixar uma fronteira para futuro provedor SEFAZ sem reescrever venda ou financeiro.
- WhatsApp, pagamentos reais, boleto registrável, HTTPS de produção e cookie `Secure` ficam para etapas futuras.

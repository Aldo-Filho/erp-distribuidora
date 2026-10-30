# Produtos — roteiro manual da base integrada

Estado: preparado, **nenhum cenário executado neste chat**. Esta lista não autoriza iniciar serviços, aplicar migrations, executar SQL/HTTP, testes, build ou lint. A validação futura precisa de autorização e ambiente isolado, com cópia controlada dos dados necessários. Etapa 2 (telas) permanece separada.

## Preparação futura

- [ ] Preservar cópia do banco e do checkout, inventariar a versão de Flyway e conferir novamente se V7/V8 continuam disponíveis.
- [ ] Conferir conflitos de fornecedores preferenciais por SKU. A V7 interrompe com mensagem específica se houver mais de um; uma pessoa deve escolher. Não remover preferências automaticamente. Se necessário, usar a versão anterior da aplicação e seus PATCHs antes da atualização autorizada.
- [ ] Revisar V7 e V8 juntas. Não iniciar o backend atualizado com apenas uma parte do schema. V1–V6 não são modificadas.
- [ ] Comparar antes/depois: product_id, SKU, FKs, saldos, reservas, recebimentos, situações efetivas, datas existentes, custos/preços, descontos e totais. Cada produto anterior recebe seu próprio catálogo SIMPLE.
- [ ] Confirmar que `unit` continua nula nos legados, opções antigas têm `conversionConfirmed=false` e fator/tipo desconhecidos nulos; snapshots históricos ausentes permanecem nulos. `baseQuantity=quantity` e fator histórico 1 preservam a semântica numérica anterior sem inventar embalagem.
- [ ] Inventariar `purchase_suggestion_items` e colunas fiscais da V1 como dependências futuras. Seus tipos/funcionalidades não foram ampliados nesta etapa.

## Contratos públicos e consumidores

| Contrato | Mudança e observação |
| --- | --- |
| `/productCatalog/search` | GET paginado por nome/SKU/marca/categoria/situação/query; ordenação name/id. Conta catálogos, não SKUs. |
| `/productCatalog/{id}` | GET (404 ausente), PATCH de dados comuns. POST na raiz cria agregado atômico, com items; não há DELETE novo de agrupador. |
| `/productCatalog/{id}/items/search` | GET paginado de SKUs, query/active, ordenação sku/id. POST em `/items` acrescenta variação. |
| `/product` | URLs preservadas; POST simples exige unit, nome e marca. Com catalogId cria variação, sem dados comuns no mesmo corpo. GET search continua lista; acrescenta catalogId. PATCH simples encaminha dados comuns/situação ao catálogo; VARIANT usa endpoint principal para esses dados. |
| Resposta Product | Mantém name/brand/category delegados; acrescenta catalogId, unit, attributes e ownActive. active é efetivo. Não embute catálogo inteiro ou opções de fornecedor. |
| `/productPresentation` | GET search paginado por productId/query/active, GET por ID, POST e PATCH. Base criada pelo servidor, fator 1, preço próprio nulo e situação controlada pelo SKU. Sem DELETE. |
| `/productSupplierOption` | GET search paginado por productSupplierId/query/active, GET por ID, POST e PATCH. Custo por opção, fator/tipo, código, prazo, situação e confirmação de conversão. Sem DELETE. |
| `/productSupplier` | Preserva par e campos antigos como acesso à legacyOption identificada. Novo vínculo pode incluir option; campos antigos criam opção pendente. Sem legacyOption, escrita antiga pede endpoint de opção/optionId. Não escolhe primeira opção arbitrária. |
| `/priceTableItem` | presentationId opcional; null representa preço base. PATCH omitido mantém e removePresentation=true volta à base. Não registrar preço específico da própria apresentação base. |
| `/stockItem`, `/stockMovement` | Quantidades, reservas, mínimos, máximos e saldos passam a BigDecimal em BASE. Movimento compensatório permanece o caminho para corrigir histórico. |
| `/orderItem` | quantity comercial; presentationId opcional seleciona base no cadastro. PATCH removePresentation=true volta à base. quantity e preço comercial permanecem distintos de baseQuantity. |
| `/purchaseItem` | quantity comercial e supplierOptionId. Omissão no cadastro só seleciona quando há exatamente uma opção ativa/confirmada do par. unitCost é por opção, não por base. receivedQuantity é BASE. |
| `/purchase/{id}/receive` | quantity recebida continua BASE. Preserva requestId e retorno do recibo anterior em repetição equivalente. |

Novas páginas: page 0..100000, size 1..100 (padrão 25), ordenação estável por nome/SKU e ID. Respostas seguem entidades/Page, sem DTOs novos de resposta de negócio. Flags opcionais são Boolean: omissão/null/false não removem; true com novo valor é contraditório.

Consumidores existentes: seleções operacionais de SKU, estoque inicial, estoque/movimentos, pedidos/reservas/despacho, compras/recebimentos, fornecedores e itens de tabelas. `frontend/src/services/prices.js` continua consumindo apenas `/price-table/search`. Catálogo do frontend, ferramentas ignoradas, Insomnia e testes não foram adaptados.

## 1. Um saldo base para diferentes embalagens

- [ ] Criar SIMPLE UN com custo/preço zero e sem fornecedor/estoque; conferir único SKU interno e apresentação base UN, sem variação artificial.
- [ ] Configurar estoque de 120 UN, reserva inicial zero, limites coerentes. Criar pacote 6 e caixa 12; não deve existir saldo por embalagem nem operação de abrir caixa.
- [ ] Comprar 2 caixas de 24: quantity=2, baseQuantity=48. Receber 48 BASE soma 48 ao mesmo saldo; vender 1 pacote 6 reserva/baixa 6 BASE, sem multiplicação dupla.
- [ ] Comprar 2 caixas de 12, receber 18 BASE: receivedQuantity=18 e pendência=6 BASE, PARTIALLY_RECEIVED. Receber as 6 restantes conclui RECEIVED.
- [ ] Confirmar que SENT não recebe diretamente. Inativar catálogo/opção/fornecedor após SENT, avançar para CONFIRMED e receber: situação posterior do cadastro não reabre compromisso contratado. Operador e autorização continuam necessários.
- [ ] Excesso em uma linha de recibo deve desfazer recibo, itens, movimentos e saldos de todas as linhas na mesma transação.

## 2. Linhas independentes, agregação e preservação comercial

- [ ] Vender uma caixa 12 e duas unidades avulsas do mesmo SKU/depósito em linhas diferentes: reservar 14 BASE, mantendo ambas as linhas e snapshots.
- [ ] Mesmo fornecedor oferecer pacote 6 e caixa 24, com custo/código/prazo próprios. Compra aceita ambas; recebimento soma entradas do mesmo estoque sem sobrescrever saldo ou pendência de outra linha.
- [ ] Repetir a mesma identidade de linha (documento/SKU/depósito/apresentação ou opção) retorna conflito; não soma silenciosamente. Também conferir a proteção concorrente do índice.
- [ ] Caixa 12 com preço próprio 54 vende por 54 sem tabela aplicável. Com tabela do cliente de 4 por base e sem preço específico, vende por 48. Preço específico da tabela prevalece; tabela sem preço permite fallback ao catálogo. Zero é preço definido.
- [ ] Alterar preço/fator/nome/atributos no cadastro depois da inclusão não modifica snapshots ou preço da linha. Editar só quantity preserva condições contratadas; mudar explicitamente produto/apresentação/opção recria snapshots daquela linha.
- [ ] Conversão usada não pode ser alterada: inativar e criar outra opção/apresentação. Não atualizar custo de referência do SKU por cotação ou compra.
- [ ] Prazo específico zero prevalece; removeAvgDeliveryDays=true usa prazo geral, inclusive zero; ambos ausentes significam não informado. Não gerar data garantida.
- [ ] Conferir arredondamento: quantity=1.5 e preço=0.03 dão bruto 0.05; desconto percentual de 10 dá desconto 0.01 e líquido 0.04. Java e consulta agregada devem coincidir. Conferir desconto AMOUNT contra bruto arredondado, percentual >100 e limite monetário.

## 3. Precisão e faixa

- [ ] UN rejeita 1.5; KG/G/L/ML/M aceitam 1.5 e 1.50 e rejeitam 1.25. Repetir em estoque inicial, configuração de limites, movimentos, venda base, compra base e recebimento.
- [ ] Embalagem composta exige quantity comercial inteira; fator e resultado respeitam a unidade e uma casa permitida. Fator fracionado de UN e fator/base inválidos são rejeitados.
- [ ] Verificar limite 9999999999999.9 inclusive fatores/resultados/agregados e rejeição acima da faixa, sem arredondar ou truncar. UN também continua inteira no limite.
- [ ] Reservado não supera saldo; saldo não fica negativo ou abaixo de reservas; mínimo não supera máximo. removeMaxQuantity=true limpa máximo, preservando null/omissão como manutenção.
- [ ] Campos inteiros externos às quantidades (IDs e prazos) continuam rejeitando JSON fracionado, inclusive 1.0 conforme configuração existente. A ampliação decimal não muda essa política.

## 4. Situação, histórico e catálogo agrupado

- [ ] VARIANT com COLOR/SIZE exige todos os valores. Azul/M e ` azul `/m são duplicatas; acentos e espaços internos continuam significativos. SKU não muda ao corrigir atributos.
- [ ] Nome, marca, categoria e unidade são comuns; não é permitido mudar tipo, conjunto de características após criação, transferir SKU de catálogo ou agrupar legados automaticamente.
- [ ] Inativar uma variação, inativar/reativar o principal: a variação própria inativa permanece inativa; as demais recuperam disponibilidade. Em SIMPLE, ownActive permanece true e active segue o principal.
- [ ] Inativação bloqueia inclusão/edição de compromisso e confirmação de PENDING/DRAFT. CONFIRMED continua despachável/cancelável conforme regras anteriores; SENT continua avançando para CONFIRMED. Não há liberação/cancelamento automático por inativação.
- [ ] Buscar SKU/valor de atributo retorna um principal, sem duplicar totalElements. Paginar com 25/50/100, conferir páginas completas/limites, nome/id e sku/id. Conferir que o carregamento EAGER de características/atributos não transforma paginação SQL em paginação em memória.
- [ ] Consultar produto/estoque/pedido/compra/recibo com a transação fechada (open-in-view=false): sem erro de serialização ou ciclos, preservando dados de catálogo/atributos necessários. Metadados do pai não são apresentados como atualização comercial dos filhos.
- [ ] DELETE antigo bloqueia SKU com referências e última variação; SIMPLE sem relações remove seu catálogo na mesma transação. Não há exclusão física nova na interface desta etapa.

## 5. Legados e concorrência

- [ ] Legado sem unit bloqueia novas operações; conferir fisicamente qual unidade os números antigos já representam antes de PATCH do catálogo. A confirmação não converte saldos. Se for necessário transformar caixas em unidades, manter pendência e obter desenho/autorização próprios.
- [ ] Confirmar unidade não confirma embalagem/custo legado. Na opção antiga, enviar confirmConversion=true, baseFactor, basePresentation e cost explícitos após conferência humana. Snapshots históricos ausentes continuam ausentes.
- [ ] Troca de unidade definida com estoque/operações/preços/embalagens/conversões em uso é rejeitada. Sem uso, corrigir só depois de conferir precisão; não alterar IDs/saldos por rótulo.
- [ ] Duas escolhas concorrentes de fornecedor preferencial deixam no máximo um por SKU. Sem preferência é válido; inativação não escolhe substituto. Índice parcial e liberação/flush antes da nova preferência devem atuar juntos.
- [ ] Repetir mesmo requestId com as mesmas linhas, operador e notas, usando 18 ou 18.0 e outra ordem de linhas: retorna o mesmo recibo, sem segundo movimento. Quantidade/notas/operador diferentes retornam conflito. Repetição equivalente continua válida após RECEIVED e desativação posterior do operador.
- [ ] Enviar o mesmo recibo concorrentemente e conferir uma única entrada; enviar recibos distintos concorrentes e conferir pendência, saldo e movimento por linha. Ordem de locks: documento, catálogos/SKUs, itens/estoques ordenados conforme o caminho, sem inversão entre estoque administrativo e edição de catálogo.

## Permissões negativas

- [ ] products.read permite catálogo/apresentações e custo de referência já exposto por Product; não permite ler opções/custos de fornecedores sem suppliers.read.
- [ ] POST catálogo/item/apresentação exige products.create; PATCH exige products.update; DELETE antigo de SKU exige products.delete. Estoque inicial exige adicionalmente stock.configure.
- [ ] Opções e vínculos exigem suppliers.read para GET e suppliers.update para escrita. Vínculos independentes fornecedor/marca/categoria permanecem inalterados.
- [ ] Tabela/preço específico exige prices.read/create/update existentes. Resolver preço na venda não abre leitura genérica de tabelas/fornecedores. Preço manual e desconto continuam exigindo orders.manualPrice/orders.discount.
- [ ] Leitura/configuração/ajuste de estoque, confirmação/despacho/cancelamento de vendas e envio/confirmação/recebimento/cancelamento de compras mantêm suas permissões próprias. Conferir 401/403 e CSRF.

## Registro de evidência futura

Para cada cenário executado futuramente, registrar ambiente, versões/migrations, entrada, resposta e estado anterior/posterior observado. Não marcar esta lista como aprovada por leitura de código. A entrega atual não declara validação operacional, Etapa 2 concluída ou ERP pronto para produção.

## Etapa 2 — Roteiro do catálogo e vínculos (preparado, não executado)

O texto anterior registra a entrega da base. O frontend do catálogo foi escrito na Etapa 2; nenhuma das verificações abaixo foi executada. Antes da validação autorizada, preparar a base inteira com V7/V8 em ambiente isolado e seguir as pendências anteriores. As telas completas de Estoque, Tabelas de preços, Vendas e Compras permanecem nas próximas etapas.

### Acesso, navegação e consulta

- [ ] Usuário com products.read vê Produtos com ícone Package e acessa `/products`; sem a permissão, não vê menu e a rota é protegida. A primeira rota permitida anterior continua sendo respeitada.
- [ ] Conferir JamsHub, favicon e menu existentes, larguras 244/78 px, menu recolhido em até 780 px, animação e movimento reduzido; abrir Produtos não muda a identidade das demais telas.
- [ ] PageHeading mostra totalElements do catálogo, singular/plural e “encontrados” ao filtrar. Durante carregamento ou erro, não exibe zero como total conhecido.
- [ ] Principal VARIANT aparece uma vez mesmo com vários SKUs. Buscar nome, SKU e característica; combinar marca/categoria/situação e limpar filtros. SKU correspondente aparece nos detalhes com busca original e destaque; limpar a busca permite consultar todas as variações.
- [ ] Usar 25/50/100 na lista, variações, apresentações, opções e seletor de fornecedor. Conferir requisições paginadas reais e contagem da mesma resposta. Se o total diminuir, ajustar para a última página válida. Selecionar novamente o mesmo tamanho não deixa carregamento permanente.
- [ ] Digitar rapidamente e alternar filtros/páginas/SKU com respostas atrasadas: consultas anteriores são canceladas e não substituem a seleção nova. Ao trocar SKU, opções/formulários anteriores não podem ser gravados no SKU novo.
- [ ] Simular falha de leitura: mostrar erro e Tentar novamente; não apresentar “nenhum cadastrado” como resultado de falha. Detalhes consultam dados atuais; metadados do principal, SKU e ausência de datas são distintos.

### Cadastro e edição

- [ ] SIMPLE começa pela escolha de tipo e permite nome, marca, categoria opcional, unidade, custo/preço base e opcionais. Salvar sem fornecedor e sem estoque inicial; receber SKU automático somente leitura. Não há variação artificial a criar ou editar.
- [ ] VARIANT com Cor/Tamanho: cadastrar apenas Azul/M e Azul/G, sem combinações automáticas. Cada SKU mantém custo/preço/estoque próprios; marca/categoria/unidade permanecem comuns. Nenhum preço ou saldo pertence ao principal.
- [ ] Rejeitar combinação incompleta e duplicata conforme trim/minúsculas, preservando acentos/espaços internos. Corrigir valores depois sem trocar SKU. Tipo, conjunto de características, reagrupamento e unidade já definida não são alterados pela tela.
- [ ] Informar custo/preço zero e confirmar JSON com zero. Remover peso/dimensões e conferir removeWeightKg/removeDimensionX/Y/Z; categoria usa removeCategory=true. Texto opcional vazio limpa; PATCH contém somente alterações. Sem mudanças não há PATCH.
- [ ] Preservar cor/tamanho de SIMPLE legado; VARIANT edita esses valores somente em attributes. Peso aceita três casas e dimensões duas, conforme armazenamento anterior, sem impor a precisão das quantidades BASE a esses campos.
- [ ] Com stock.configure e warehouses.read, cadastrar estoque inicial por SKU/depósito, em BASE. Conferir saldo/reservado/mínimo/máximo, máximo opcional, reservado não superior ao saldo e máximo não inferior ao mínimo. Essa configuração não cria reserva de pedido. Sem permissões, o produto pode ser cadastrado sem estoque.
- [ ] UN rejeita 1,5; KG aceita 1,5 e 1,50; rejeita 1,25 como quantidade BASE, negativos, separador de milhar e texto inválido. Nenhum campo usa truncamento ou arredondamento para fazer a entrada passar.
- [ ] Legado com unidade nula mostra pendência; não sugere unidade pelo nome ou peso. Exigir reconhecimento expresso da unidade que os números anteriores já representam. Se antes eram caixas e o destino são unidades, manter a pendência e não confirmar como simples troca de rótulo.
- [ ] Editar, provocar erro de salvamento e conferir dados/rascunho mantidos. Fechar/Cancelar/Esc pedem descarte quando há mudanças; cancelar descarte conserva campos. Sessão redirecionada preserva rascunho do mesmo usuário/registro; logout/troca de usuário o limpa pelo mecanismo atual.
- [ ] Clique repetido em Salvar envia uma única escrita; fechar durante salvamento fica bloqueado. Inclusão de marca/categoria sobre o formulário atualiza seletores sem perder os dados do produto. Cadastro comum não exige confirmação extra.

### Situação e preservação

- [ ] Inativar principal apresenta literalmente o aviso aprovado sobre todas as variações, saldos e histórico, acrescido da continuidade dos compromissos confirmados. Cancelar/Esc não envia PATCH; confirmar envia apenas active ao catálogo uma vez.
- [ ] Inativar uma variação isolada, depois inativar/reativar o principal: a flag própria dessa variação continua inativa. Outras próprias ativas recuperam disponibilidade. Exibir indisponibilidade herdada e impedir ativação própria enquanto o principal estiver inativo.
- [ ] Após alterar situação/salvar, recarregar lista/dados afetados. Sem exclusão física de produto, SKU, apresentação ou vínculo/opção pela interface. Datas do principal não são apresentadas como atualização de preço/estoque dos filhos.

### Apresentações e fornecedores

- [ ] Base é fixa em fator 1, controlada pelo servidor. Pacote 6 e caixa 12 compartilham o saldo BASE; não há abrir caixa nem saldo de lacradas. Preço padrão mostrado é uma prévia do catálogo com aviso sobre a tabela do cliente.
- [ ] Preço próprio 0 é mantido; limpar preço de apresentação envia removePrice=true e volta ao cálculo pelo preço base. Não cria/atualiza preço de tabela de cliente. Fator já contratado é rejeitado pela API; orientação é inativar e cadastrar outro, preservando histórico.
- [ ] Prévia monetária fracionada coincide com HALF_UP da base: preço 0,29 e fator 1,5 em unidade fracionável resultam em 0,44. Verificar também preço derivado acima do limite de venda; a prévia não vira fonte de quantidades ou preço contratado.
- [ ] Com suppliers.read, consultar vínculos e selecionar explicitamente fornecedor para suas opções paginadas. Sem a permissão, não carregar nem apresentar custos de compra/opções. Custo de referência continua sob a política existente de Products.
- [ ] Com suppliers.update, buscar fornecedor por nome em páginas, vincular sem duplicar par e cadastrar duas opções no mesmo vínculo (pacote 6 e caixa 24) com custos/códigos distintos. Navegar no seletor não envia cadastro; fornecedor selecionado é identificado pelo nome.
- [ ] Preferência é uma por fornecedor/SKU, opcional; marcar outro retira a anterior atomicamente na API. Conferir lista recarregada e zero/uma preferência, sem preço/custo alterado. Nenhuma embalagem tem preferência própria.
- [ ] Prazo da opção zero prevalece; vazio usa prazo geral, com removeAvgDeliveryDays=true no PATCH; ambos ausentes mostram “Prazo não informado”. Não inventar data garantida. Limpar código envia texto vazio; custo continua por embalagem.
- [ ] Inativar vínculo torna todas as opções indisponíveis sem alterar suas flags próprias. Fornecedor/produto inativo também aparece como motivo herdado; salvar opção não ativa essas outras identidades.
- [ ] Opção legada exige conferência independente de unidade/fator/tipo/custo e confirmConversion=true com esses valores explícitos. Confirmar unidade do catálogo sozinho não confirma opção nem altera recebimentos anteriores.

### Marcas, categorias e vínculos independentes

- [ ] products.read consulta; products.create cadastra; products.delete permite excluir quando sem referências, com confirmação específica. Duplicidade normalizada retorna erro sem apagar o nome digitado; conflito de exclusão preserva todos os dados. Não há renomeação/situação nem datas inventadas.
- [ ] Em detalhes do fornecedor, abrir Marcas e categorias; suppliers.read consulta vínculos e suppliers.update inclui/remove. Remover associação preserva a entidade marca/categoria e produtos. products.delete não é necessário para remover somente um vínculo.
- [ ] Usuário com suppliers.read/update sem products.read consulta/remove vínculos existentes, mas não pesquisa novos cadastros; seletor explica a permissão ausente. Não há ampliação de SecurityConfig.
- [ ] Vincular produto a fornecedor não gera marca/categoria; remover categoria do produto não remove fornecedor/categoria. Cenários de associação explicitamente independente permanecem assim depois de recarregar.

### Apresentação e interação

- [ ] Conferir colunas SKU, preço e estoque na lista: SIMPLE mostra seu SKU/preço; VARIANT abre seus SKUs/preços por variação, mantendo um principal na lista. Com stock.read, mostrar saldo/reservado/disponível e abrir depósitos com mínimo/máximo; sem a permissão, ocultar coluna e não enviar consultas de estoque. Erro não aparece como zero. Trocar filtros/páginas cancela leituras antigas de SKU/estoque. Nenhum saldo é gravado pela consulta.

- [ ] Conferir desktop amplo, 1200 px, 780 px e tela estreita: linhas de 88 px, ações compactas e menu de três pontos até 1200 px. Coluna de ações acessível à direita, menu sem cortes pela tabela e conteúdo secundário consultável.
- [ ] Navegar por teclado em FilterSelect, RowActions e modais; confirmações iniciam em Cancelar. Modais sobrepostos mantêm foco, Esc apenas no topo, bloqueio de fundo e posição de rolagem, sem deslocamento ao abrir/fechar.
- [ ] Feedbacks de sucesso usam o padrão verde e margem existente. Erros de envio preservam formulário; erros de consulta são distinguíveis de vazio; retry não altera a seleção de outro registro.

### Roteiro integrado futuro (telas completas ainda não entregues)

- [ ] Após autorização específica, usar os contratos da base para configurar estoque do SKU e vincular opção caixa 24. Criar compra de 2 embalagens: quantity=2 e baseQuantity=48; respeitar DRAFT→SENT→CONFIRMED e conferir recebimento sempre em BASE.
- [ ] Para 2 caixas de 12, receber 18 BASE e depois 6; conferir pendência, PARTIALLY_RECEIVED/RECEIVED, saldo e repetição de requestId numericamente equivalente sem duplicar movimento.
- [ ] Na venda, selecionar SKU/apresentação/depósito; caixa 12 mais 2 BASE em linhas distintas reserva 14 BASE. Despachar baixa uma única vez; cancelar libera conforme o contrato existente. O frontend desta etapa não contém essas telas.
- [ ] Inativar cadastros após compromisso confirmado e conferir conclusão de recebimento/despacho; novos compromissos continuam bloqueados. Alterações de preço/fator atuais não reescrevem snapshots das linhas.
- [ ] Conferir caixa de catálogo 54 e preço unitário 4 na tabela do cliente: venda resulta em 48 quando não há preço específico de caixa na tabela. Esse roteiro usa o resolvedor da base, sem uma tela nova de Tabelas de preços nesta etapa.

Ao executar futuramente, registrar evidência real por cenário. Todos os itens acima permanecem desmarcados; formatação e revisão de código não contam como execução funcional.

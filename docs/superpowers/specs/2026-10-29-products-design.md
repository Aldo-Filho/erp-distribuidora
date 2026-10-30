# Produtos, variações e apresentações comerciais

Data: 29/10/2026. Estado: desenho completo aprovado pelo usuário, incluindo as propostas complementares, após revisão de consistência. Aprovação autoriza elaborar o plano técnico em etapas encadeadas; não autoriza implementação neste chat.

## Objetivo e contexto

Preparar o catálogo operacional do ERP interno para uma instalação por empresa, com produtos simples, variações, venda avulsa e embalagens de compra e venda. A experiência visual deve seguir o JamsHub atual e o AGENTS.md vigente, sem refazer outras telas.

Autenticação, Clientes e Fornecedores são etapas concluídas e validadas pelo usuário. Não reabrir suas pendências antigas. Os vínculos de fornecedores com produtos, marcas e categorias foram adiados expressamente para esta etapa.

## Abordagens discutidas e escolhida

1. Entrega única de catálogo, variações, embalagens e adaptações dos fluxos: permite revisão integrada, mas concentra mudanças antes de uso pelo usuário.
2. Etapas encadeadas: preparar primeiro a estrutura e adaptar os fluxos existentes para frações e conversões; entregar depois o cadastro completo de Produtos e vínculos. As telas operacionais completas de Estoque, Compras e Vendas continuam nas etapas seguintes do roteiro.

O usuário escolheu etapas encadeadas. Nenhuma etapa parcial deve ser apresentada como conclusão do fluxo operacional completo. O plano técnico será elaborado somente após aprovação deste desenho; implementação em outro chat expressamente autorizado.

## Estado atual confirmado por leitura estática

- Product possui SKU automático PRD, marca obrigatória, categoria opcional, custo, preço, peso, cor, dimensões, tamanho, situação e datas de criação/atualização. Não há agrupamento de variações nem cadastro de apresentações comerciais.
- ProductService permite estoque inicial opcional na mesma transação; a busca de produtos retorna lista, sem paginação real.
- ProductSupplier oferece um vínculo por par produto/fornecedor, com custo, prazo, código no fornecedor, preferência e situação. A preferência atual não garante unicidade por produto.
- Marcas e categorias possuem consulta, cadastro, exclusão e unicidade de nome normalizado. Seus registros não possuem situação nem metadados de datas. Existem vínculos próprios fornecedor/marca e fornecedor/categoria; não são automaticamente derivados do vínculo produto/fornecedor.
- StockItem é único por produto/depósito. Saldos, reservas, mínimos, máximos, movimentações, itens de pedidos, itens de compras e recebimentos usam quantidades inteiras. Os services também usam cálculos, mapas, agregações e limites baseados em inteiros.
- Pedidos resolvem preço pela tabela aplicável ao cliente e, na ausência, pelo preço do produto. Preços manuais e descontos possuem permissões próprias.
- Compras possuem custo por item e recebimento parcial com requestId. Reservas e despacho de vendas já existem. As proteções atuais de transação, concorrência e repetição devem ser preservadas; não são funcionalidades ausentes a recriar.
- As rotas atuais do frontend não oferecem Produtos; a pasta Products contém apenas products.css. Os componentes visuais compartilhados e o AGENTS.md foram alterados localmente por outro trabalho e devem ser preservados.
- A V1 contém campos fiscais e commercial_unit que não estão mapeados em Product. A presença dessas colunas não representa funcionalidade fiscal pronta.
- Esta leitura não comprova execução. Não foram executados testes, build, lint, serviços, SQL ou migrations.

## Decisões confirmadas na conversa

### Unidades e quantidades

- Unidades fixas do sistema: UN, KG, G, L, ML e M.
- UN aceita apenas quantidades inteiras; as demais aceitam até uma casa decimal.
- O estoque representa o total disponível na unidade base, separadamente por depósito e item comercializável. Não distinguir caixas lacradas de unidades soltas e não exigir operação de abertura de embalagem.
- Permitir comprar embalagens e vender avulso. Permitir várias apresentações de venda para o mesmo item, como unidade, pacote com 6 e caixa com 12.
- Fornecedores diferentes podem fornecer o mesmo item em embalagens diferentes, como caixas com 12 ou com 24.
- Cada apresentação informa sua equivalência na unidade base. Comprar duas caixas de 24 acrescenta 48 UN; vender um pacote de 6 desconta 6 UN.
- Recebimento parcial pode registrar parte do conteúdo das embalagens na unidade base, sem exigir caixas completas. Acrescentar somente a quantidade efetivamente recebida ao estoque e manter o restante pendente na compra. Exemplo aprovado: compra de duas caixas de 12 UN; recebimento de 18 UN; entrada de 18 UN e saldo pendente de 6 UN. Cada recebimento continua sujeito às proteções existentes contra excesso e repetição.

### Produtos e variações

- A lista principal mostra o produto agrupador, e suas variações são consultadas nos detalhes, evitando uma linha principal por cor/tamanho.
- Produtos sem variações são comercializados diretamente, sem exigir variação artificial na experiência do usuário.
- Produtos com variações exigem seleção da variação nas operações; o agrupador não tem saldo ou preço comercial próprios.
- Cada variação tem SKU, estoque, preços, apresentações e vínculos com fornecedores próprios.
- Características fixas disponíveis: Cor, Tamanho, Voltagem, Potência, Capacidade e Modelo. Valores são informados no cadastro.
- Cada produto escolhe as características aplicáveis; cadastrar apenas combinações efetivamente comercializadas, sem gerar todas automaticamente.
- Marca, categoria e unidade de medida pertencem ao produto principal e são compartilhadas pelas variações.
- Código de barras fica fora da primeira versão. Nome, SKU e características identificam os itens nas consultas.

### Preços e custos

- Preço próprio de apresentação é opcional. Sem ele, calcular preço unitário multiplicado pela equivalência; com ele, usar o preço definido para a apresentação. A regra vale para pacotes e caixas, sem modificar a equivalência de estoque.
- Custo de referência é manual, por produto simples ou variação, separado dos custos de fornecedores. Alterar cotação/vínculo não o atualiza automaticamente.
- Atualização de custo por recebimentos e custo histórico para margem não estão decididos por esta regra; devem ser tratados no desenho de Compras/Financeiro, sem usar retroativamente o custo atual como custo histórico comprovado.
- Prioridade aprovada para o preço de venda: usar preço próprio da apresentação na tabela aplicável ao cliente; se não houver, multiplicar o preço base do item nessa tabela pela equivalência; se a tabela não oferecer nenhum desses preços, usar o preço próprio da apresentação no catálogo ou o preço base do catálogo multiplicado pela equivalência. Ter uma tabela atribuída sem preço para o item permite fallback ao catálogo. Exemplo aprovado: caixa com 12 custa R$ 54 no catálogo, mas a tabela do cliente informa R$ 4 por unidade e nenhum preço próprio para a caixa; a caixa custa R$ 48 para esse cliente.

### Fornecedores

- Cadastro de produto não exige fornecedor. Vínculos podem ser adicionados posteriormente.
- Preservar custo, prazo específico, código no fornecedor, preferência e situação, acrescentando a representação de embalagem de compra.
- No máximo um fornecedor preferencial por item comercializável, sem obrigatoriedade de escolher um. Marcar outro retira a preferência anterior de forma atômica. A preferência indica uma escolha sugerida, sem obrigar compras nesse fornecedor.
- Um mesmo fornecedor pode oferecer várias opções de compra do mesmo produto simples ou variação, como pacote com 6 e caixa com 24, cada uma com equivalência, custo e código no fornecedor próprios. Esta possibilidade foi aprovada para a primeira versão e substitui a proposta inicial de uma única apresentação por vínculo.

### Situação

- Oferecer ativação/inativação na tela, preservando vínculos, histórico e saldos; não oferecer exclusão física de produtos/variações.
- Inativar o principal torna todas as suas variações indisponíveis para novas compras e vendas. Permitir inativar uma variação isoladamente.
- A confirmação deve avisar explicitamente: "Ao inativar o produto principal, todas as suas variações também ficarão inativas e indisponíveis para novas compras e vendas. Os saldos e o histórico serão preservados."
- Reativar o principal não reativa variações que já estavam inativas antes da inativação do principal.
- A consulta histórica e o acesso aos saldos continuam disponíveis conforme permissões.
- Ao inativar produto ou variação, bloquear novos itens de compras/vendas e novas confirmações que criem compromisso. Permitir concluir ou cancelar operações já confirmadas, incluindo despacho de vendas com reserva e recebimento de compras já contratadas. Pedidos ainda não confirmados precisam retirar/substituir o item inativo ou aguardar sua reativação; rascunhos de compra seguem a mesma proteção antes da confirmação. Não cancelar operações, liberar reservas ou apagar itens automaticamente por causa da inativação. Regra aprovada pelo usuário após apresentação do rascunho.

## Detalhamento complementar aprovado com o desenho

Estas escolhas foram propostas pelo agente e aprovadas em conjunto com o desenho completo. Não foram decisões individuais anteriores do usuário. As escolhas técnicas adicionais do plano serão identificadas como propostas técnicas sujeitas à revisão do plano.

### Precisão, conversões e preservação histórica

- Usar aritmética decimal exata no backend e armazenamento decimal nas quantidades. Não usar ponto flutuante nem conversões para int/long que descartem frações. O plano deve definir precisão total e limites após inventariar todas as colunas e cálculos afetados.
- Rejeitar precisão maior que a permitida em vez de arredondar silenciosamente; zeros finais não contam como precisão extra significativa. Aplicar a regra também ao resultado da conversão.
- Venda/compra na unidade base aceita a precisão dessa unidade. Quantidade de embalagens compostas é inteira e positiva; para frações do conteúdo, selecionar a unidade base. Equivalência de embalagem é positiva, respeita a unidade base e tem até uma casa decimal.
- Exibir unidade, quantidade comercial e equivalente na base nas operações que usam embalagem, evitando ambiguidade entre duas caixas e duas unidades.
- Registrar no item da operação a apresentação, equivalência, unidade e valor usados na contratação. Mudanças futuras no cadastro não recalculam pedidos, compras, reservas ou recebimentos históricos.
- Estoque, reservas e movimentações usam sempre a unidade base. No recebimento parcial aprovado, informar quantidade base recebida e mostrar a equivalência comercial contratada, o total recebido e o saldo pendente.
- Manter regras de saldo não negativo, reservas não superiores ao saldo, mínimo/máximo coerentes e histórico protegido. Preservar bloqueios, ordem de aquisição de locks e idempotência. Não enfraquecer a rejeição de frações para UN ao ampliar as outras unidades.
- Uma unidade base já usada em estoque ou operações não pode ser trocada por edição comum. Equivalência já usada não pode ser reinterpretada: inativar a apresentação antiga e cadastrar uma nova quando necessário.
- Não transformar automaticamente produtos existentes em variantes nem inferir unidade pelo nome, peso ou tamanho. Elaborar migração incremental que preserve IDs, SKUs, valores e relações; registros legados precisam de unidade confirmada antes de operar no novo modelo. O plano deve prever saneamento assistido, sem assumir que todos os legados representam UN.
- Conversão de produto simples com histórico em agrupador e reagrupamento de variantes ficam fora da primeira entrega; não transferir saldos ou histórico silenciosamente.

### Preço e custo na operação

- Custo de referência e preço base são valores por unidade base. Cada opção de compra do fornecedor informa explicitamente o custo da sua apresentação. Migrar vínculos antigos para uma opção inicial sem perder custo, prazo, código, preferência, situação, identidade e referências existentes; manter fator 1 somente quando confirmado que esse é o significado do vínculo legado.
- Manter um vínculo por par item comercializável/fornecedor e várias opções de compra dentro dele. A preferência pertence ao fornecedor naquele item, não a cada embalagem. Selecionar explicitamente a opção na compra quando houver mais de uma disponível; não escolher automaticamente a de menor custo ou presumir uma embalagem pelo fornecedor preferencial.
- Proposta de organização das opções: cada uma tem equivalência, custo, código, prazo específico opcional e situação próprios. O vínculo mantém sua situação e a preferência. Um vínculo inativo torna todas as suas opções indisponíveis para novas compras, preservando o histórico. Não permitir opções ativas indistinguíveis para o mesmo fornecedor/item: o plano deve definir identidade de apresentação e normalização de nomes sem confundir embalagens de quantidades diferentes.
- Apresentações de venda são independentes das opções de compra: não criar, alterar ou remover preços de venda ao cadastrar uma embalagem do fornecedor. Compras podem ter o mesmo item em opções diferentes em linhas separadas; adaptar a verificação atual de duplicidade, mantendo recebimentos e pendências por linha contratada.
- Aplicar a precedência de preços aprovada na seção Decisões confirmadas, preservando a seleção atual de tabela aplicável ao cliente por situação e vigência. Não escolher outra tabela automaticamente por ter preço menor.
- Preparar essa resolução nos contratos de Vendas e preços na etapa estrutural; a interface completa de Tabelas de preços será entregue na sua etapa própria.
- Manter valores monetários com duas casas e as permissões atuais de preço manual/desconto. Proposta: arredondar o valor bruto de cada linha a centavos, aplicar o desconto conforme sua modalidade e somar os valores líquidos das linhas. O plano deve especificar modo de arredondamento e compatibilidade entre cálculo Java e consulta agregada; a consulta atual só arredonda o desconto percentual, o que é insuficiente ao admitir quantidades fracionadas. Não arredondar quantidades para compensar totais e não recalcular documentos históricos na migração.
- Permitir que uma venda contenha o mesmo item em apresentações diferentes, como uma caixa e duas unidades avulsas, em linhas distintas com suas condições comerciais. O plano deve revisar a proteção atual de duplicidade por produto e agregar as necessidades de reserva na unidade base, mantendo rastreabilidade das linhas.

### Catálogo e experiência

- Reutilizar paginação real 25/50/100, total da API, ordenação estável, tratamento de carregamento/erro/vazio e proteção contra respostas antigas, conforme Clientes/Fornecedores. Paginar produtos principais; a contagem representa produtos principais encontrados, sem contar cada variação como produto da lista.
- Busca por nome, SKU e valores de características, com filtros de marca, categoria e situação. Quando uma variação corresponde à busca, mostrar o principal e identificar a correspondência nos detalhes.
- Detalhes oferecem dados comuns, variações, apresentações e fornecedores, conforme permissões. Não expor custo a públicos adicionais por efeito colateral de um novo endpoint; o plano deve conferir a política atual.
- Cadastro começa pela escolha simples/com variações. Exigir nome e marca; categoria permanece opcional. Exigir ao menos uma variação válida para concluir cadastro com variações. Não permitir combinação duplicada, comparando valores sem caixa ou espaços nas extremidades e preservando acentos/espaços internos.
- Características e unidade são fixas do sistema; valores das características e nomes das apresentações são preenchidos pelo usuário. Não criar CRUD de tipos de características ou de unidades.
- Variações usam todas as características escolhidas no principal. Alterar o conjunto de características após criar variações requer decisão específica; na primeira entrega, bloquear essa mudança e permitir corrigir os valores de uma variação, preservando unicidade e histórico da identificação nas operações.
- Custo e preço base obrigatórios e não negativos, mantendo a aceitação atual de zero. Zero é preço informado, não ausência. Peso, dimensões e outros opcionais existentes devem ser preservados; o plano deve explicitar a convivência de color/size legados com as novas características, sem descartá-los ou duplicá-los silenciosamente.
- Reutilizar PageHeading, FilterSelect, RowActions, Modal e ConfirmationProvider/useConfirm, conforme AGENTS.md atual. Sem confirmações nativas, sem subtítulos redundantes e sem reestilizar outras telas.
- Detalhes exibem metadados reais conforme record-metadata-design. Não inventar datas para marcas/categorias ou apresentar datas do principal como histórico dos filhos.
- Marcas e categorias: consultar, cadastrar e excluir quando as relações permitirem, conforme APIs atuais. Não acrescentar renomeação ou situação sem decisão própria. Gerenciar vínculos fornecedor/marca e fornecedor/categoria no contexto do fornecedor; preservar sua independência, sem criá-los ou removê-los automaticamente ao mudar um produto.
- Prazo específico da opção de compra prevalece sobre prazo geral do fornecedor quando informado, inclusive zero; ausente usa prazo geral, e ambos ausentes significam não informado. O prazo legado do vínculo deve ser preservado na opção inicial. Prever remoção explícita de prazo específico, preservando null/omissão do PATCH como manutenção. Nenhum prazo gera data garantida de entrega automaticamente.
- Inativação do principal pode ser representada por situação efetiva derivada da situação própria do principal e da variação, preservando a situação individual. Não permitir ativação efetiva de variação enquanto o principal estiver inativo. Explicar a indisponibilidade na interface.
- Não remover DELETEs existentes automaticamente; novos endpoints não devem permitir exclusão física de itens com histórico nem transformar um agrupador em item movimentável.

## Aprovação

As perguntas individuais sobre operações em andamento, preços da tabela do cliente, recebimento parcial e várias opções do mesmo fornecedor foram respondidas. O usuário aprovou o desenho completo, incluindo o detalhamento complementar, com "aprovado!". Posteriormente, aprovou o conjunto de planos técnicos em 29/10/2026 com "positivo". Implementação não iniciada, somente em outro chat expressamente autorizado.

## Etapas encadeadas e critérios de saída

### Etapa 1 — Estrutura e contratos integrados

Preparar produto simples/agrupador/variação, unidades, apresentações, fornecedor preferencial e snapshots comerciais. Adaptar todos os caminhos atuais de quantidades: cadastro inicial, estoque, limites, movimentos, reservas/liberação, itens e totais de vendas, confirmação/despacho, itens e totais de compras, recebimento parcial e repetição por requestId. Preparar resolução de preços por apresentação, sem antecipar as telas completas de Vendas/Compras.

Saída verificável: contratos e migração cobrem todos os caminhos de quantidade e mantêm os registros anteriores; UN rejeita fração, outras unidades aceitam uma casa; agrupador nunca entra no estoque; conversão ocorre uma única vez; operação histórica mantém fatores e valores; preferência e situação respeitam as regras. A comprovação em execução exige autorização específica e não é substituída por revisão estática.

### Etapa 2 — Catálogo e vínculos operacionais

Entregar consulta, cadastro, edição, detalhes, situação e busca por variação; apresentações de venda e fornecedores; gestão de marcas/categorias e vínculos fornecedor/marca/categoria adiados da etapa anterior. Seguir componentes e permissões existentes. Cadastro não exige saldo inicial nem fornecedor; inclusão opcional de estoque inicial mantém autorização stock.configure e atomicidade.

Saída verificável: lista não repete variações; seleção operacional identifica SKU correto; confirmações explicam efeito do principal; reativação preserva inativas anteriores; fornecedores/embalagens/preços independentes funcionam conforme desenho; feedback e formulário preservam dados em erro; paginação ocorre no backend. Preparar roteiro manual sem executá-lo automaticamente.

### Dependências seguintes do roteiro

Após a base de Produtos, planejar Depósitos/Estoque e Tabelas de preços, depois os fluxos completos de Vendas e Compras, ajustando a ordem pelas dependências. Financeiro, Dashboard/relatórios e preparação de produção seguem depois. Primeira versão utilizável depende de integração e validação desses fluxos, não apenas do catálogo.

## Limites e orientação para o plano técnico

- Apenas planejamento neste chat: sem código de produto, dependências, SQL, aplicação/alteração de migrations, testes/build/lint, serviços, commits, push, deploy ou worktree. Única escrita desta fase: documentação do desenho/plano e seu progresso.
- Preservar alterações locais e arquivos ignorados, inclusive ferramentas de auditoria. Não expor application.yaml nem atualizar memórias. Não enviar mensagens a outros chats.
- Separar leitura estática, validação informada pelo usuário e execução autorizada futura.
- O plano deve inventariar consumidores, endpoints, cálculos e schema, propor contratos e migrations incrementais sem aplicá-las, e explicitar a compatibilidade das entidades retornadas. Não reutilizar regras de Cliente indiscriminadamente.
- Separar colunas usadas por módulos executáveis de tabelas de roadmap, como sugestões de compra e documentos fiscais. Inventariar estas dependências sem implementar módulos futuros por existirem colunas de quantidade no schema.
- Conferir o mapeamento atual de products.*, suppliers.*, stock.*, prices.* e permissões de Vendas/Compras em SecurityConfig antes de especificar novas rotas. Não ampliar acesso por colocar vínculos de fornecedor numa tela de Produto.
- Não considerar o atual INVOICED prova de autorização de NF-e. Integração fiscal, boletos, WhatsApp, portal de comprador, multi-tenant e decisão de hospedagem definitiva estão fora desta etapa.
- Nada está implementado por este documento. O plano técnico em etapas encadeadas foi elaborado, revisado e aprovado. Próximo passo: execução em outro chat quando expressamente autorizada pelo usuário.

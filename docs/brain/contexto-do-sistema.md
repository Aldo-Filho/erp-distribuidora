# Contexto do sistema

## Como consultar

| Necessidade | Documento |
| --- | --- |
| Regras e limites funcionais | `regras-de-negocio.md` |
| Situação das frentes e prioridades | `roadmap.md` |
| Próximas alterações e critérios de aceite | `plano-atual.md` |
| Arquitetura, contratos e limites de validação | Este arquivo; ler somente a seção relevante |

## Arquitetura e módulos

- Backend: Java 21, Spring Boot, Spring Security, Spring Data JPA, PostgreSQL e Flyway; organização por funcionalidade. Cadastros usam JPA; acesso, aparência e perfil empresarial também usam JDBC.
- Frontend: React, Vite, Tailwind CSS e estilos em `frontend/src/index.css` e nos módulos.
- Autenticação por sessão no servidor, cookie `ERP_SESSION`, CSRF, permissões por cargo e auditoria. Não há autenticação stateless.
- Módulos: produtos/catálogos/variantes/apresentações, clientes, fornecedores, preços, depósitos, estoque, compras, vendas, financeiro, sugestões de compra, empresa, aparência, dashboard e administração de acesso.
- Banco e e-mail são configurados diretamente em `backend/src/main/resources/application.yaml`. Não há perfil local adicional ativo para externalização. Não registrar credenciais nos documentos.
- `target`, `dist` e `node_modules` são saídas/dependências locais, não fontes do sistema. Preservar manifests, lockfiles, Maven Wrapper e assets.
- `ProductCatalog` guarda nome, marca, categoria e unidade; `Product` representa o SKU e deriva esses dados. Não criar `products.name` ou `products.brand_id` para duplicar o catálogo.
- Dinheiro usa `NUMERIC(12,2)`; fatores/quantidades operacionais usam `NUMERIC(14,1)`. UN exige inteiros. Preços por cliente/apresentação e snapshots são calculados no backend.
- Venda: `PENDING`, `CONFIRMED`, `INVOICED`, `CANCELED`; `INVOICED` é exibido como Despachado. Compra: `DRAFT`, `SENT`, `CONFIRMED`, `PARTIALLY_RECEIVED`, `RECEIVED`, `CANCELED`.
- Histórico fiscal legado permanece separado de `demonstrative_invoices`; não há emissão fiscal real.

## Inicialização e migrations

- V1 consolida o schema. V2 contém somente segurança, cargos e permissões, sem usuários ou dados comerciais demonstrativos; o primeiro administrador usa o bootstrap existente. V9 permanece na sequência como migration sem operações.
- V3: unicidade do documento normalizado de clientes. V4: aparência/tema. V5: financeiro. V6: códigos de barras. V7: sugestões de compra. V8: dashboard/preferências. V10: demonstrativo. V11: perfil empresarial. Categorias financeiras e aparência padrão continuam essenciais.
- Alterações já feitas em V2/V9 modificam checksums: inicialização limpa requer banco novo. Não executar repair nem apagar/limpar o banco existente para ajustar essa diferença.
- V3 não corrige nem funde documentos históricos: duplicatas normalizadas impedem o índice e exigem tratamento explícito. Documentos novos/alterados passam por normalização e validação local, sem consulta à Receita.

## Navegação atual

- Início, Operações, Cadastros e Financeiro concentram as tarefas diárias. Configurações reúne empresa/aparência, cadastros financeiros, equipe e segurança em seções recolhíveis.
- Minha conta e Sair ficam no rodapé. O menu móvel possui botão Menu, nomes completos, fechamento por destino/Escape e gerenciamento de foco.
- A navegação usa `routes/navigation.js` e `components/layout/AppLayout.jsx`; visibilidade do menu não substitui autorização nas rotas/API.
- Organização do menu e ajustes recentes do dashboard estão aplicados no código; aceite visual das versões atuais ainda pendente.

## Contratos financeiros

- `POST /orders/{id}/confirmations` recebe corpo opcional `{paymentMethod, paymentDueDate}`; usa os valores do pedido quando omitidos. Cadastro/PATCH também aceitam `paymentDueDate` enquanto pendente. Métodos imediatos usam a data da confirmação. Uma repetição com método/vencimento diferente é rejeitada; a mesma confirmação não cria outra conta nem reserva. A confirmação na interface coleta vencimento futuro somente para crédito/boleto; sem ele a operação é recusada.
- Confirmar venda/compra cria o título na mesma transação comercial. A V5 garante unicidade de `order_id`/`purchase_id` e configura categorias padrão próprias de receita/despesa. A categoria marcada `defaultForDocuments` deve estar ativa; trocá-la afeta somente novos títulos. Tipo de categoria é imutável.
- Coleções abaixo usam `Page<T>`, limites comuns e ordem por ID decrescente; caixa usa data decrescente e ID. Os filtros de títulos são `status`, `categoryId`, `partyId` (cliente/fornecedor), `documentId` (venda/compra), `paymentMethod`, `dueFrom`, `dueTo`. `GET /receivables/summary` e `/payables/summary` retornam `{count,totalAmount}` do conjunto filtrado inteiro (valor nominal dos títulos, não saldo restante).
- `POST /receivables/{id}/settlements` e `/payables/{id}/settlements` recebem `{bankAccountId,amount,paidAt,paymentMethod,notes?}`. Baixa integral, data entre emissão e hoje, conta ativa e método da venda obrigatório no recebimento. Compra registra o método na baixa. Retorno 200 na criação e na repetição idêntica, com chave natural título/operação; outros dados são recusados.
- `POST /{receivables|payables}/{id}/settlements/{paymentId}/reversals` recebe `{reversedAt,reason}`; retorno 200 na criação/repetição idêntica. Data entre baixa e hoje, motivo obrigatório e operador da sessão. O estorno usa a conta original, mesmo inativa. Sem PATCH/DELETE de títulos, baixas ou estornos.
- `GET /{receivables|payables}/{id}`, `GET /{receivables|payables}/{id}/settlements` e `GET /{receivables|payables}/{id}/settlements/{paymentId}/reversals` permitem ler título e histórico. Os dois históricos são paginados e verificam pertencimento ao pai.
- `GET/POST /financial-categories`, `GET/PATCH /financial-categories/{id}`: cadastro `{name,type,defaultForDocuments?}`; PATCH `{name?,active?,defaultForDocuments?}`; filtros `type`, `active`. `GET/POST /bank-accounts`, `GET/PATCH /bank-accounts/{id}`: cadastro `{name,type,initialBalance}`; PATCH dos mesmos campos opcionais e `active`. Saldo inicial/tipo bancário não mudam após qualquer movimentação; não há exclusão. Alterações registram operador/data.
- `GET /cash-flow` retorna eventos `ACTUAL` (pagamentos/estornos) e `PROJECTED` (saldo dos títulos abertos, incluindo saldo parcial histórico). Filtros: `from`, `to`, `bankAccountId`, `categoryId`, `kind`, `direction` (`RECEIVABLE`/`PAYABLE`). Entradas têm valor positivo e saídas negativo; `direction` identifica a origem, inclusive em estornos. Projeções não têm conta bancária definida, portanto não entram em filtros por banco. `/cash-flow/summary` separa totais realizados/projetados do filtro; `/cash-flow/balance?bankAccountId=&through=` soma saldo inicial e movimentos realizados até a data (hoje quando omitida), incluindo contas inativas.
- `finance.read` protege títulos, históricos, caixa e totais; `finance.configure` protege configuração e permite leitura de categorias/contas; `finance.settle` e `finance.reverse` autorizam somente suas respectivas escritas. Administradores de acesso existentes recebem as quatro permissões na V5; outros cargos exigem concessão explícita. A tela de cargos reconhece os rótulos. `financialRoutePermissions` é usado no menu financeiro; rotas React protegidas por `finance.read` para consultas e `finance.configure` para configuração.
- Ordem de locks: documento comercial, título e conta bancária. Despacho/recebimento usam o mesmo lock comercial e recusam documento estornado. Cancelamento exige ausência de pagamentos sem estorno e cancela título/libera reservas atomicamente.
- V1/V2 e eventos históricos permanecem intactos. V5 identifica linhas antigas como `legacy`; não inventa operador/data de registro ausentes nem cria títulos retroativos. Título antigo aberto sem pagamento pode receber baixa integral; título pago com exatamente um pagamento integral pode ser estornado antes do despacho/recebimento. Históricos parciais/múltiplos exigem conciliação específica fora desta etapa. Duplicatas antigas de vínculo comercial impedem a constraint, em vez de serem fundidas silenciosamente.

## Interface financeira

- Páginas `/finance/receivables`, `/finance/payables`, `/finance/cash-flow`, `/finance/categories` e `/finance/bank-accounts`. Listas/históricos paginados no servidor; filtros e totais usam os contratos financeiros acima. Cliente/fornecedor pode ser filtrado por código sem exigir leitura do cadastro. Links comerciais usam `?documentId=` validado pelo retorno seguro de navegação.
- Detalhes de vendas e compras mostram a conta vinculada somente com `finance.read`. A confirmação da venda coleta método único/vencimento e exibe a conta após recarregar. Usuário sem consulta financeira não faz chamadas nem renderiza valores financeiros extras. Os valores comerciais já autorizados por vendas/compras permanecem no documento de origem.
- Baixa e estorno pedem confirmação, bloqueiam envios concorrentes e preservam o payload para repetir o mesmo evento após falha. Fechar o formulário recarrega o histórico para conferir resultados incertos. Cadastro de configuração com falha de rede/servidor exige conferir a lista antes de repetir, pois não é uma escrita idempotente.
- Histórico de baixa/estorno é somente leitura. Boleto demonstra valor, vencimento e referência `JAMSHUB-DEMO-{id}`; download HTML imprimível com “não processável”/“sem validade bancária” no conteúdo e nome do arquivo, sem linha digitável ou código bancário.
- Caixa separa realizado e previsto. O saldo exibido vem do servidor e considera conta/data final e todo o histórico anterior, sem filtros de categoria/origem/data inicial; a tela explicita essa diferença. Novos totais nunca são calculados somando apenas a página visível.

## Código de barras

- `product_barcodes` pertence ao SKU por FK `product_id` (V6). Campos: `code`, `type` (`EAN_GTIN`/`INTERNAL`), `mainCode`, `active`, auditoria de operador/data. Código é imutável e globalmente único, inclusive inativo; não pode ser transferido. Um principal ativo por SKU, protegido por índice parcial e lock do produto. Inativar o principal remove a marca; nenhum substituto é escolhido automaticamente.
- Normalização: remover espaços nas extremidades, converter letras para maiúsculas e preservar zeros iniciais. Aceita até 128 caracteres ASCII alfanuméricos, ponto, hífen, barra e sublinhado, sem espaços internos. EAN/GTIN exige 8/12/13/14 dígitos e dígito verificador. Código duplicado não é cadastrado novamente.
- `GET /product-barcodes?productId=&page=&size=` retorna `Page<Barcode>` por ID, limites comuns; `POST /product-barcodes` recebe `{productId,code,type,mainCode?}` (201); `PATCH /product-barcodes/{id}` recebe `{active?,mainCode?}` (200). Consulta exige `products.read`; escrita exige `products.update`. Cadastro/ativação na ficha do SKU, seção Dados comerciais; sem DELETE ou alteração de identidade.
- `GET /product-barcodes/lookup?code=` resolve um SKU ativo pelo código exato normalizado, com `products.read` ou `stock.read`. Inexistente retorna 404; código ou SKU/catálogo inativo retorna 400. O retorno usa a representação de produto já usada pelos fluxos operacionais.
- `useBarcodeScanner` e `BarcodeScanner` usam campo focado, leitor como teclado e Enter ou botão; bloqueiam consultas concorrentes e cancelam lookup ao desmontar. Não capturam digitação global. Busca por texto continua disponível.
- Venda seleciona o SKU no editor normal; compra mostra as opções do fornecedor para esse SKU ou seleciona-o no editor. Nenhuma leitura inclui produto automaticamente sem conferência. Estoque permite localizar SKU, configurar produto ou conferir o SKU do ajuste fixo; código de outro SKU é recusado no ajuste. A lista Produtos abre estoque do SKU exato (ou Dados comerciais sem `stock.read`).

## Sugestões de compra

- `POST /purchase-suggestions` recebe `{requestId: UUID}` e retorna snapshot da geração (200, inclusive retry). Exige `purchases.suggest`, `purchases.create` e `purchases.read`. GET da coleção, `/{id}` e `/{id}/items` exige `purchases.read`; coleção e itens retornam Page, padrões 0/25 e limites comuns, IDs descendentes na coleção e ascendentes nos itens. Campos de snapshot SQL usam snake_case. `GET /purchase-suggestions` e `/items` não alteram compras.
- V7 amplia quantidades legadas e acrescenta período exato, operador, requestId único, depósito, opção, prazo, estoque reservado/disponível, alvo, cobertura, custo/fator/comercial e vínculo de compra/item. Preserva histórico antigo sem inventar depósito. Dados antigos podem aparecer sem os campos novos.
- Geração usa transação e lock advisory PostgreSQL comum entre processos. Repetir o mesmo UUID devolve a mesma geração; outra geração recalcula os inputs. Compras geradas abertas são bloqueadas na ordem de ID antes de calcular cobertura, compatível com edições normais pelo lock da compra.
- Consumo soma movimentos OUTBOUND/SALE vinculados a vendas INVOICED nos 90 dias anteriores ao instante da geração, por SKU/depósito. Média = vendido/90; alvo = max(mínimo, média × (prazo + 7)); disponível = físico − reservado; falta residual = max(0, alvo − disponível − cobertura).
- Opção elegível exige SKU/catálogo, fornecedor, vínculo e opção ativos, conversão confirmada, custo não negativo e fator positivo. Preferencial ativo elegível primeiro; caso contrário menor custo por unidade BASE, com desempate por fornecedor e opção. Prazo da opção, depois fornecedor, depois zero. Quantidade comercial arredonda para cima (inteira para apresentações; BASE fracionável em décimos), sem sugerir menos que a falta. Serviços normais validam limites, unidade e total; qualquer erro reverte toda a geração.
- Criam-se novos rascunhos somente para falta residual por fornecedor/depósito, via PurchaseService/PurchaseItemService; usuário da sessão, opção, depósito e snapshots normais. Não altera rascunhos anteriores nem envia/confirma/recebe automaticamente. A FK purchase_suggestion_id identifica compras geradas. Cobertura considera itens atuais destas compras em DRAFT/SENT/CONFIRMED/PARTIALLY_RECEIVED e desconta received_quantity; edição/exclusão/cancelamento entra no próximo cálculo, sem usar quantidades antigas do snapshot. Recebimentos concluídos deixam a cobertura e já compõem estoque físico.
- Sem falta registra COVERED; sem opção elegível registra NO_SUPPLIER e não gera compra. `shortage_quantity` mantém a falta residual mesmo sem fornecedor; `suggested_quantity` é a quantidade BASE efetivamente incluída na compra. A tela mostra os dois valores. O alvo é calculado com vendido × (prazo + 7) / 90 antes de arredondar, evitando propagar arredondamento da média. Snapshot GENERATED guarda vínculos e parâmetros da geração, não substitui o estado editável atual. A página explica a fórmula e oferece link para edição/consulta normal das compras; detalhes da compra identificam a geração de origem. `purchases.suggest` aparece em cargos; concessão inicial limitada a administradores de acesso que já podem criar compras.

## NF-e demonstrativa

- V10 adiciona somente `demonstrative_invoices`: ID sequencial para número `DEMO-`, `order_id` único/FK, operador da sessão, instante com fuso e snapshot JSONB schemaVersion 1. Não altera nem preenche o domínio fiscal legado. Nenhuma emissão histórica é inventada para vendas já despachadas.
- `OrderDispatchService` mantém lock e contrato de retorno `Order`. Na mesma transação de consumo das reservas e mudança para INVOICED, chama `DemonstrativeInvoiceService.issue` (propagação MANDATORY), que exige `orders.dispatch`. Retry INVOICED retorna o pedido existente sem criar documento retroativo. Erro ao capturar/gravar o documento reverte todo o despacho.
- `InvoiceProvider` separa a captura; implementação `DemonstrativeInvoiceProvider` é local, sem rede. Futuro provedor real exige fluxo/contrato próprios e autorização explícita; não se deve inserir chamada externa numa transação de estoque como troca silenciosa.
- Snapshot guarda emissor via vínculo explícito `company_appearance.company_id`, destinatário/endereços, pagamento/vencimento, itens com SKU/nome/características, apresentação, unidade BASE, quantidades comercial/BASE, fator, preço, desconto informado/aplicado e totais. A fórmula replica o arredondamento de `OrderItemRepository.calculateTotalByOrderId` em BigDecimal; divergência do total bloqueia o despacho e reverte a transação. Quantidades não são convertidas a inteiro.
- Dados ausentes do emissor e endereços inexistentes/incompletos são sinalizados no snapshot, sem inventar empresa/documento ou criar novo bloqueio por falta de cadastro fiscal. Para itens legados sem descrição/SKU histórico, a captura no despacho usa o cadastro disponível com aviso explícito. Os dados são armazenados e nunca recalculados na consulta; o módulo não oferece UPDATE/DELETE/reescrita do snapshot.
- `GET /orders/{id}/demonstrative-invoice` exige `orders.read` antes da consulta e responde com `Cache-Control: no-store`; 404 quando não existe, inclusive legado. Retorno `{id,number,orderId,issuedAt,notice,snapshot}`. Não existe emissão manual pública. Valores são comerciais da venda, sujeitos à permissão da venda; não expõe saldos ou baixas financeiras.
- Detalhe da venda despachada oferece consulta com estados de carga/erro/ausência e impressão. Janela de impressão isolada, sem opener, scripts ou recursos externos; todos os valores do snapshot são escapados. Banner e marca/rodapé de impressão identificam “DEMONSTRATIVO — SEM VALIDADE FISCAL”. O documento não comprova pagamento, não apura tributos e não gera chave, protocolo, QR code, XML oficial ou comunicação SEFAZ.

## Dashboard

- `GET /dashboard?from=&to=&compare=&warehouseId=&unit=&rankingMetric=` exige sessão, usa `Cache-Control: no-store` e mantém `generatedAt`, `roleName`, `profiles`, `cards`. Adiciona `schemaVersion:1`, `selection`, `coverage`, `widgets` e catálogo autorizado. Datas juntas; até 366 dias, sem futuro; comparação contígua de igual duração. Timezone America/Sao_Paulo; séries diárias até 31 dias, semanais até 120, mensais acima, incluindo dias vazios e limites parciais. Série anterior alinhada pela posição no período atual.
- `DashboardCatalog` fecha IDs, permissões, tipos e dimensões. União de perfis FINANCE/SALES/PURCHASING/STOCK pelas permissões efetivas; nenhum nome/ID de cargo concede acesso. Serviços verificam acesso antes das consultas. Venda monetária exige `orders.read` + `finance.read`; dados de quantidade não selecionam nem retornam líquido/preço. Cobertura histórica exige `orders.read`.
- Faturamento soma `orders.total_amount` de CONFIRMED/INVOICED por `confirmed_at`, nunca criação. V8 deixa histórico nulo; confirmação nova grava uma vez na mesma transação da reserva/financeiro. Retry, despacho e cancelamento não reescrevem data. Cancelamento retira valor do período original; não é fechamento contábil imutável. `coverage.undatedEligibleSales` sinaliza vendas elegíveis sem data, fora das séries.
- Ranking top 10 por SKU/unidade BASE; quantidade/preço exigem unidade única (UN quando omitida), faturamento permite unidades diferentes. Desempate por SKU/ID/unidade. Fórmula líquida replica `OrderItemRepository.calculateTotalByOrderId` em decimal com os mesmos arredondamentos. Preço médio ponderado = líquido / quantidade BASE; itens sem conversão/unidade confiáveis são excluídos dessas duas métricas com aviso e preservados no faturamento em grupo incompleto.
- Caixa usa somente ACTUAL em `financial_cash_flow`: positivos, negativos em absoluto e soma assinada, incluindo estornos. Movimento líquido não é saldo bancário. Saldos/agenda/vencidos reutilizam PROJECTED da mesma view, que desconta baixas não estornadas e preserva saldo parcial legado. Excluem saldo zero; vencidos antes de hoje; agenda hoje até +6 dias. São snapshots, independentes do calendário global.
- Vendas: contagem/série por confirmação, situações atuais e fila CONFIRMED. Compras: situações atuais, fila CONFIRMED/PARTIALLY_RECEIVED, criação no período e gerações distintas por fornecedor/depósito sem multiplicar itens. Estoque: alertas por SKU/depósito (disponível < mínimo), sem disponível (<=0), físico/reservado/disponível e movimentos por tipo/unidade; não soma unidades incompatíveis. Depósito só filtra estoque e sugestões e exige `stock.read`; unidade só filtra blocos compatíveis.
- Widgets têm período/data-base/filtros efetivos, metric/series/rows/comparison e estados READY/EMPTY/UNAVAILABLE. EMPTY indica ausência de registros, preservando gráficos quando movimentos líquidos se compensam. Falha isolada não retorna SQL nem zero fictício. Conteúdo prévio é limitado e explicitado; detalhes paginados acessam a origem completa.
- `GET /dashboard/widgets/{id}/details` repete datas, unidade, depósito e métrica compatíveis; aceita `productId` no ranking, página >=0 e size 1–50. ID desconhecido 404, conhecido negado 403. Ranking retorna itens/venda e quantidades; líquido somente com acesso financeiro. Demais blocos retornam suas linhas de origem. Usa modal/tabela no dashboard; filtros de criação e `safeReturn` das telas antigas são preservados.
- `GET/PUT/DELETE /preferences/current/dashboard`: preferência da sessão, sem `userId`, tema ou aparência global. JSONB <=16 KiB em `dashboard_preferences` (V8); `{schemaVersion,revision,widgets:[{id,visible,x,y,w,h}],filter:{preset,referenceDate,from,to,compare}}`. Catálogo valida IDs/permissões, inteiros, grid 12 colunas, mínimos, h<=24 e colisão entre blocos visíveis; coordenadas ocultas não ocupam espaço. PUT substitui snapshot por revisão otimista; DELETE exige `?revision=N` e restaura logicamente mantendo revisão monotônica. Conflitos 409; JSON/IDs inválidos 400. GET remove revogados/aposentados e insere novos no fim, mantendo ocultações explícitas.
- `GET /preferences/current/dashboard?defaults=true` devolve o padrão autorizado com a revisão atual, sem gravação. A primeira abertura, a prévia em edição e o DELETE usam a mesma distribuição do servidor.
- React usa Recharts 3.10.1/React Grid Layout 2.3.0/react-is 19.2.8 com React 19 existente. Grade 12 colunas, célula de 40px + espaçamento; no celular a grade segue a ordem lógica, com até dois indicadores compactos por linha e os demais blocos em largura total. Os blocos medem a altura natural do conteúdo com ResizeObserver e ampliam o mínimo da grade, sem rolagem interna de indicadores, rankings ou agenda; gráficos têm área de 240px. No celular não há limite de altura dos blocos. A expansão visual também funciona com preferências antigas; o payload continua limitado a 24 linhas de altura escolhida, com eventual altura adicional derivada do conteúdo apenas na renderização. Edição por alças ou botões; salvar grava no servidor, cancelar restaura snapshot de entrada, restaurar em edição fica em rascunho. Unidade/depósito/métrica são filtros de consulta; calendário/comparação persistem. Erros de gravação e 409 são explícitos. AbortController e limpeza de efeitos descartam respostas antigas; usuário/permissões diferentes remontam todo o estado.
- Cards antigos permanecem no contrato público com suas métricas originais (criação + INVOICED no mês, nominal OPEN, caixa mensal, alertas/gerações globais), sem novos filtros. A interface não os consome. Rotas e retornos seguros existentes permanecem; segurança central autoriza as novas rotas e os serviços fazem a autorização granular.
- `/` autenticado redireciona para `/dashboard`; usuário sem blocos autorizados recebe estado explicativo.

## Ambiente de validação

- Toda classe Spring de testes ativa o perfil `test`. O `ContextCustomizerFactory` registrado em `src/test/resources/META-INF/spring.factories` exige destino explicitamente isolado e recusa `erp`, JNDI e URLs divergentes de Flyway/Hikari/JPA antes da criação dos beans. Destinos aceitos: `127.0.0.1:55435/jamshub_test`, `127.0.0.1:55434/products_verify` ou `127.0.0.1:55433/auth_verify_<número>`. Não há criação automática de banco; fornecer URL/credenciais do banco isolado somente quando a execução for autorizada. O perfil usa SMTP local, sem autenticação nem bootstrap.
- A coleção `testeRequisicoesInsomnia.yaml` permanece uma fixture manual; não foi importada nem executada. Na validação de estoque inicial positivo usar operador com `stock.configure` e `stock.adjust`.

## Contrato HTTP orientado a recursos

- Coleções consultáveis retornam `Page<T>`: `content`, `number`, `size`, `totalElements`, `totalPages`, `numberOfElements`, `first`, `last` e `empty`. `page` começa em zero (máximo 100000), `size` padrão 25 (máximo 100). A interface pode solicitar 20/25/50/100 explicitamente. O servidor fixa ordenação estável por ID ou nome/SKU + ID. Histórico de estoque: `occurredAt DESC, id DESC`. Associações usam o ID do filho como desempate no escopo do pai.
- Exceção limitada: `GET /auth/sessions` mantém a lista das sessões próprias válidas, limitada pela política de acesso, e o desafio de login mantém seu payload. Não são buscas abertas do cadastro.
- `allPages` nos serviços frontend busca páginas limitadas para seletores, associações e documentos que precisam da lista completa. Listas principais continuam paginadas. O histórico visível de estoque navega páginas de 25, preserva filtros e apresenta `totalElements`. A recuperação de um movimento usa `stockItemId + requestId` e `size=1`, sem percorrer o histórico.
- `PATCH` edita dados parciais; `status` de pedidos/compras não é aceito nessa rota. Transições usam os recursos de eventos abaixo. Confirmação/cancelamento/despacho usam a identidade do documento + operação como chave natural: lock de escrita e guarda de estado impedem repetir reservas, liberação ou baixa. O resultado é o documento atual (200); os eventos financeiros possuem histórico próprio de baixas e estornos, sem exclusão. Os métodos de estoque/recebimento continuam exigindo `requestId` UUID, conferindo o mesmo payload e retornando 201 na criação e 200 na repetição. Chave reutilizada com payload diferente continua erro 400, conforme o contrato existente. Não há retry automático de escritas nos clientes.
- Os handlers mantêm o envelope `{status,error,message,timestamp}`: 400 para payload/parâmetros/regra inválida; 401 sem sessão; 403 sem permissão/CSRF; 404 para item inexistente (GET deixou de retornar 204); 405 para operações indisponíveis; 409 para conflitos de integridade/concorrência e desafio de substituição de sessão; 429 para limitação de acesso. Exclusão bem-sucedida retorna 204; reenvio de convite preserva 202 porque a entrega é assíncrona. Nenhum erro de binding inclui valores enviados.
- Permissões de módulo e permissões adicionais de saldo inicial, preço manual, desconto e transições continuam verificadas no servidor. Recibos são consultados pelo par compra/recibo. Rotas antigas foram removidas dos controllers e dos consumidores ativos; o proxy `/api` do Vite permanece inalterado. Não há HATEOAS nem autenticação stateless.
- A paginação de recibos primeiro seleciona IDs no banco e então carrega o grafo de cada recibo da página; evita paginar em memória uma consulta que faz fetch de coleção. Os demais métodos paginados preservam os grafos de leitura existentes, quando definidos.

### Matriz de rotas atuais

`Page<T>` é a coleção paginada; tipos sem `Page` são representações de item. Rotas com pai na URL são coleções/itens filhos. As entradas são os DTOs existentes (ou filtros/query/path indicados); campos comerciais e monetários não foram renomeados.

| Rota / método | Entrada | Resposta HTTP / forma | Permissão |
| --- | --- | --- | --- |
| `GET /company/appearance` | — | 200; `Appearance {palette, logoUrl, version}` | Autenticado |
| `PATCH /company/appearance` | `{palette, version}` | 200; `Appearance` | company.appearance.manage |
| `GET /company/appearance/logo` | query v opcional para atualizar a imagem no navegador | 200; PNG/JPEG; sem cache; 404 sem logo | Autenticado |
| `PUT /company/appearance/logo` | multipart `file`, `version` | 200; `Appearance`; 400 imagem inválida; 413 tamanho excedido | company.appearance.manage |
| `DELETE /company/appearance/logo` | query version | 200; `Appearance` | company.appearance.manage |
| `GET /preferences/current` | — | 200; `{theme}` | Próprio usuário autenticado |
| `PATCH /preferences/current` | `{theme: "LIGHT" ou "DARK" ou null}` | 200; `{theme}` | Próprio usuário autenticado |
| `GET /auth/csrf` | — | 200; `CsrfView` | Público; CSRF nas escritas |
| `POST /auth/invitation-acceptances` | RequestInvitationAcceptanceDTO | 201; `Void` | Público; CSRF nas escritas |
| `PATCH /auth/password` | PatchPasswordDTO | 204; `Void` | Sessão própria |
| `POST /auth/password-recoveries` | RequestPasswordRecoveryDTO | 204; `Void` | Público; CSRF nas escritas |
| `POST /auth/password-resets` | RequestPasswordResetDTO | 204; `Void` | Público; CSRF nas escritas |
| `POST /auth/session-activities` | — | 204; `Void` | Sessão própria |
| `POST /auth/session-replacements` | RequestSessionReplacementDTO | 200 / 409; `Object` | Público; CSRF nas escritas |
| `GET /auth/sessions` | — | 200; `List<UserSession>` | Sessão própria |
| `POST /auth/sessions` | RequestLoginDTO | 200 / 409; `Object` | Público; CSRF nas escritas |
| `DELETE /auth/sessions/current` | — | 204; `Void` | Público; CSRF nas escritas |
| `GET /auth/sessions/current` | — | 200; `SessionView` | Sessão própria |
| `DELETE /auth/sessions/{id}` | — | 204; `Void` | Sessão própria |
| `GET /brands` | —; BrandFilter; query page, size | 200; `Page<Brand>` | products.read |
| `POST /brands` | RequestBrandDTO | 201; `Brand` | products.create |
| `DELETE /brands/{id}` | — | 204; `Void` | products.delete |
| `GET /brands/{id}` | — | 200; `Brand` | products.read |
| `GET /categories` | —; CategoryFilter; query page, size | 200; `Page<Category>` | products.read |
| `POST /categories` | RequestCategoryDTO | 201; `Category` | products.create |
| `DELETE /categories/{id}` | — | 204; `Void` | products.delete |
| `GET /categories/{id}` | — | 200; `Category` | products.read |
| `GET /customer-addresses` | —; CustomerAddressFilter; query page, size | 200; `Page<CustomerAddress>` | customers.read |
| `POST /customer-addresses` | RequestCustomerAddressDTO | 201; `CustomerAddress` | customers.create |
| `DELETE /customer-addresses/{id}` | — | 204; `Void` | customers.delete |
| `GET /customer-addresses/{id}` | — | 200; `CustomerAddress` | customers.read |
| `PATCH /customer-addresses/{id}` | PatchCustomerAddressDTO | 200; `CustomerAddress` | customers.update |
| `GET /customers` | —; CustomerFilter; query page, size | 200; `Page<Customer>` | customers.read |
| `POST /customers` | RequestCustomerDTO | 201; `Customer` | customers.create |
| `DELETE /customers/{id}` | — | 204; `Void` | customers.delete |
| `GET /customers/{id}` | — | 200; `Customer` | customers.read |
| `PATCH /customers/{id}` | PatchCustomerDTO | 200; `Customer` | customers.update |
| `GET /order-items` | —; OrderItemFilter; query page, size | 200; `Page<OrderItem>` | orders.read |
| `POST /order-items` | RequestOrderItemDTO | 201; `OrderItem` | orders.create |
| `DELETE /order-items/{id}` | — | 204; `Void` | orders.delete |
| `GET /order-items/{id}` | — | 200; `OrderItem` | orders.read |
| `PATCH /order-items/{id}` | PatchOrderItemDTO | 200; `OrderItem` | orders.update |
| `GET /orders` | —; OrderFilter; query page, size | 200; `Page<Order>` | orders.read |
| `POST /orders` | RequestOrderDTO | 201; `Order` | orders.create |
| `DELETE /orders/{id}` | — | 204; `Void` | orders.delete |
| `GET /orders/{id}` | — | 200; `Order` | orders.read |
| `PATCH /orders/{id}` | PatchOrderDTO | 200; `Order` | orders.update |
| `POST /orders/{id}/cancellations` | — | 200; `Order` | orders.cancel |
| `POST /orders/{id}/confirmations` | Corpo opcional: `paymentMethod`, `paymentDueDate`; crédito/boleto exigem vencimento futuro | 200; `Order` | orders.confirm |
| `POST /orders/{id}/dispatches` | RequestOrderDispatchDTO | 200; `Order` | orders.dispatch |
| `GET /orders/{id}/price` | —; query productId, presentationId | 200; `BigDecimal` | orders.read |
| `GET /permissions` | —; PermissionFilter; query page, size | 200; `Page<Permission>` | access.roles.manage |
| `POST /permissions` | — | 405; `ResponseErrorDTO` | Autenticado; escrita indisponível |
| `DELETE /permissions/{id}` | — | 405; `ResponseErrorDTO` | Autenticado; escrita indisponível |
| `GET /permissions/{id}` | — | 200; `Permission` | access.roles.manage |
| `PATCH /permissions/{id}` | — | 405; `ResponseErrorDTO` | Autenticado; escrita indisponível |
| `GET /price-table-items` | —; PriceTableItemFilter; query page, size | 200; `Page<PriceTableItem>` | prices.read |
| `POST /price-table-items` | RequestPriceTableItemDTO | 201; `PriceTableItem` | prices.create |
| `DELETE /price-table-items/{id}` | — | 204; `Void` | prices.delete |
| `GET /price-table-items/{id}` | — | 200; `PriceTableItem` | prices.read |
| `PATCH /price-table-items/{id}` | PatchPriceTableItemDTO | 200; `PriceTableItem` | prices.update |
| `GET /price-tables` | —; PriceTableFilter; query page, size | 200; `Page<PriceTable>` | prices.read |
| `POST /price-tables` | RequestPriceTableDTO | 201; `PriceTable` | prices.create |
| `DELETE /price-tables/{id}` | — | 204; `Void` | prices.delete |
| `GET /price-tables/{id}` | — | 200; `PriceTable` | prices.read |
| `PATCH /price-tables/{id}` | PatchPriceTableDTO | 200; `PriceTable` | prices.update |
| `GET /product-catalogs` | —; ProductCatalogFilter; query page, size | 200; `Page<ProductCatalog>` | products.read |
| `POST /product-catalogs` | RequestProductCatalogDTO | 201; `ProductCatalog` | products.create |
| `GET /product-catalogs/{id}` | — | 200; `ProductCatalog` | products.read |
| `PATCH /product-catalogs/{id}` | PatchProductCatalogDTO | 200; `ProductCatalog` | products.update |
| `GET /product-catalogs/{id}/products` | —; query query, active, page, size | 200; `Page<Product>` | products.read |
| `POST /product-catalogs/{id}/products` | RequestCatalogItemDTO | 201; `Product` | products.create |
| `GET /product-presentations` | —; ProductPresentationFilter; query page, size | 200; `Page<ProductPresentation>` | products.read |
| `POST /product-presentations` | RequestProductPresentationDTO | 201; `ProductPresentation` | products.create |
| `GET /product-presentations/{id}` | — | 200; `ProductPresentation` | products.read |
| `PATCH /product-presentations/{id}` | PatchProductPresentationDTO | 200; `ProductPresentation` | products.update |
| `GET /product-supplier-options` | —; ProductSupplierOptionFilter; query page, size | 200; `Page<ProductSupplierOption>` | suppliers.read |
| `POST /product-supplier-options` | RequestProductSupplierOptionDTO | 201; `ProductSupplierOption` | suppliers.update |
| `GET /product-supplier-options/{id}` | — | 200; `ProductSupplierOption` | suppliers.read |
| `PATCH /product-supplier-options/{id}` | PatchProductSupplierOptionDTO | 200; `ProductSupplierOption` | suppliers.update |
| `GET /product-suppliers` | —; ProductSupplierFilter; query page, size | 200; `Page<ProductSupplier>` | suppliers.read |
| `POST /product-suppliers` | RequestProductSupplierDTO | 201; `ProductSupplier` | suppliers.update |
| `DELETE /product-suppliers/{id}` | — | 204; `Void` | suppliers.update |
| `GET /product-suppliers/{id}` | — | 200; `ProductSupplier` | suppliers.read |
| `PATCH /product-suppliers/{id}` | PatchProductSupplierDTO | 200; `ProductSupplier` | suppliers.update |
| `GET /products` | —; ProductFilter; query page, size | 200; `Page<Product>` | products.read |
| `POST /products` | RequestProductDTO | 201; `Product` | products.create |
| `DELETE /products/{id}` | — | 204; `Void` | products.delete |
| `GET /products/{id}` | — | 200; `Product` | products.read |
| `PATCH /products/{id}` | PatchProductDTO | 200; `Product` | products.update |
| `GET /purchase-items` | —; PurchaseItemFilter; query page, size | 200; `Page<PurchaseItem>` | purchases.read |
| `POST /purchase-items` | RequestPurchaseItemDTO | 201; `PurchaseItem` | purchases.create |
| `DELETE /purchase-items/{id}` | — | 204; `Void` | purchases.delete |
| `GET /purchase-items/{id}` | — | 200; `PurchaseItem` | purchases.read |
| `PATCH /purchase-items/{id}` | PatchPurchaseItemDTO | 200; `PurchaseItem` | purchases.update |
| `GET /purchases` | —; PurchaseFilter; query page, size | 200; `Page<Purchase>` | purchases.read |
| `POST /purchases` | RequestPurchaseDTO | 201; `Purchase` | purchases.create |
| `DELETE /purchases/{id}` | — | 204; `Void` | purchases.delete |
| `GET /purchases/{id}` | — | 200; `Purchase` | purchases.read |
| `PATCH /purchases/{id}` | PatchPurchaseDTO | 200; `Purchase` | purchases.update |
| `POST /purchases/{id}/cancellations` | — | 200; `Purchase` | purchases.cancel |
| `POST /purchases/{id}/confirmations` | — | 200; `Purchase` | purchases.confirm |
| `GET /purchases/{id}/receipts` | —; query page, size | 200; `Page<PurchaseReceipt>` | purchases.read |
| `POST /purchases/{id}/receipts` | RequestPurchaseReceiptDTO | 201 novo / 200 repetição; `PurchaseReceipt` | purchases.receive |
| `POST /purchases/{id}/submissions` | — | 200; `Purchase` | purchases.send |
| `GET /purchases/{purchaseId}/receipts/{id}` | — | 200; `PurchaseReceipt` | purchases.read |
| `GET /roles` | —; RoleFilter; query page, size | 200; `Page<Role>` | access.roles.manage ou access.users.manage |
| `POST /roles` | RequestRoleDTO | 201; `Role` | access.roles.manage |
| `DELETE /roles/{id}` | — | 204; `Void` | access.roles.manage |
| `GET /roles/{id}` | — | 200; `Role` | access.roles.manage ou access.users.manage |
| `PATCH /roles/{id}` | PatchRoleDTO | 200; `Role` | access.roles.manage |
| `GET /roles/{roleId}/permissions` | —; query page, size | 200; `Page<RolePermission>` | access.roles.manage |
| `POST /roles/{roleId}/permissions` | RequestRolePermissionDTO | 201; `RolePermission` | access.roles.manage |
| `DELETE /roles/{roleId}/permissions/{permissionId}` | — | 204; `Void` | access.roles.manage |
| `GET /security/audit-events` | —; query from, to, actorId, actorName, action, outcome, page, size | 200; `Page<EventView>` | security.audit.read |
| `GET /security/settings` | — | 200; `SecuritySettings` | security.settings.manage |
| `PATCH /security/settings` | PatchSecuritySettingsDTO | 200; `SecuritySettings` | security.settings.manage |
| `GET /stock-items` | —; StockItemFilter; query page, size | 200; `Page<StockItem>` | stock.read |
| `POST /stock-items` | RequestStockItemDTO | 201; `StockItem` | stock.configure |
| `DELETE /stock-items/{id}` | — | 204; `Void` | stock.configure |
| `PATCH /stock-items/{id}` | PatchStockItemDTO | 200; `StockItem` | stock.configure |
| `GET /stock-movements` | —; StockMovementFilter; query page, size | 200; `Page<StockMovement>` | stock.read |
| `POST /stock-movements` | RequestStockMovementDTO | 201 novo / 200 repetição; `StockMovement` | stock.adjust |
| `GET /stock-movements/{id}` | — | 200; `StockMovement` | stock.read |
| `GET /supplier-addresses` | —; SupplierAddressFilter; query page, size | 200; `Page<SupplierAddress>` | suppliers.read |
| `POST /supplier-addresses` | RequestSupplierAddressDTO | 201; `SupplierAddress` | suppliers.create |
| `DELETE /supplier-addresses/{id}` | — | 204; `Void` | suppliers.delete |
| `GET /supplier-addresses/{id}` | — | 200; `SupplierAddress` | suppliers.read |
| `PATCH /supplier-addresses/{id}` | PatchSupplierAddressDTO | 200; `SupplierAddress` | suppliers.update |
| `GET /suppliers` | —; SupplierFilter; query page, size | 200; `Page<Supplier>` | suppliers.read |
| `POST /suppliers` | RequestSupplierDTO | 201; `Supplier` | suppliers.create |
| `DELETE /suppliers/{id}` | — | 204; `Void` | suppliers.delete |
| `GET /suppliers/{id}` | — | 200; `Supplier` | suppliers.read |
| `PATCH /suppliers/{id}` | PatchSupplierDTO | 200; `Supplier` | suppliers.update |
| `GET /suppliers/{supplierId}/brands` | —; query page, size | 200; `Page<SupplierBrand>` | suppliers.read |
| `POST /suppliers/{supplierId}/brands` | RequestSupplierBrandDTO | 201; `SupplierBrand` | suppliers.update |
| `DELETE /suppliers/{supplierId}/brands/{brandId}` | — | 204; `Void` | suppliers.update |
| `GET /suppliers/{supplierId}/categories` | —; query page, size | 200; `Page<SupplierCategory>` | suppliers.read |
| `POST /suppliers/{supplierId}/categories` | RequestSupplierCategoryDTO | 201; `SupplierCategory` | suppliers.update |
| `DELETE /suppliers/{supplierId}/categories/{categoryId}` | — | 204; `Void` | suppliers.update |
| `GET /user-invitations` | —; query email, page, size | 200; `Page<UserInvitation>` | access.users.manage |
| `POST /user-invitations` | RequestUserInvitationDTO | 201; `UserInvitation` | access.users.manage |
| `DELETE /user-invitations/{id}` | — | 204; `Void` | access.users.manage |
| `POST /user-invitations/{id}/deliveries` | — | 202; `Void` | access.users.manage |
| `GET /users` | —; UserFilter; query page, size | 200; `Page<User>` | access.users.manage ou security.sessions.manage |
| `POST /users` | — | 405; `ResponseErrorDTO` | Autenticado; escrita indisponível |
| `DELETE /users/{id}` | — | 204; `Void` | access.users.manage |
| `GET /users/{id}` | — | 200; `User` | access.users.manage ou security.sessions.manage |
| `PATCH /users/{id}` | PatchUserDTO | 200; `User` | access.users.manage |
| `POST /users/{id}/password-recoveries` | — | 204; `Void` | access.users.manage |
| `GET /users/{id}/sessions` | —; query page, size | 200; `Page<UserSession>` | security.sessions.manage |
| `DELETE /users/{userId}/sessions/{id}` | — | 204; `Void` | security.sessions.manage |
| `GET /warehouse-addresses` | —; WarehouseAddressFilter; query page, size | 200; `Page<WarehouseAddress>` | warehouses.read |
| `POST /warehouse-addresses` | RequestWarehouseAddressDTO | 201; `WarehouseAddress` | warehouses.create |
| `DELETE /warehouse-addresses/{id}` | — | 204; `Void` | warehouses.delete |
| `GET /warehouse-addresses/{id}` | — | 200; `WarehouseAddress` | warehouses.read |
| `PATCH /warehouse-addresses/{id}` | PatchWarehouseAddressDTO | 200; `WarehouseAddress` | warehouses.update |
| `GET /warehouses` | —; WarehouseFilter; query page, size | 200; `Page<Warehouse>` | warehouses.read |
| `POST /warehouses` | RequestWarehouseDTO | 201; `Warehouse` | warehouses.create |
| `DELETE /warehouses/{id}` | — | 204; `Void` | warehouses.delete |
| `GET /warehouses/{id}` | — | 200; `Warehouse` | warehouses.read |
| `PATCH /warehouses/{id}` | PatchWarehouseDTO | 200; `Warehouse` | warehouses.update |

## Operação contínua de compras e vendas

- `/orders/:id` e `/purchases/:id` apresentam revisão e próximas operações na mesma página; os caminhos `/edit` continuam aceitos para retomar a montagem. A interface diferencia montar, revisar/reservar e despachar, ou montar, registrar envio, confirmar com fornecedor e receber. A disponibilidade é indicativa e agrupada por SKU/depósito, somando apresentações; a reserva definitiva continua atômica no backend. `Adicionar` grava diretamente com os dados definidos; `Configurar` abre o formulário sem gravação automática.
- `GET /orders` e `GET /purchases` retornam páginas ordenadas por ID decrescente e aceitam `number` (ID exato), `query` (nome/nome fantasia do cliente/fornecedor) e `createdFrom`/`createdTo` (ISO datetime, limites inclusivos). Permanecem os filtros de situação, pagamento nas vendas, IDs de cliente/fornecedor e usuário. As listas preservam filtros, página e posição ao retornar e destacam o documento consultado.
- Em `POST /purchases/{id}/receipts`, cada item aceita **exatamente um** entre `quantity` (BASE, contrato anterior preservado) e `commercialQuantity` (número inteiro de apresentações contratadas). A conversão usa `baseFactorSnapshot` e `unitSnapshot` do item da compra, nunca o custo/fator atual do fornecedor; sem arredondamento implícito. A repetição mantém UUID, operador, observação e quantidades equivalentes em BASE. O navegador mantém a tentativa exata e o bloqueio entre abas.
- O encerramento do saldo não entregue após recebimento parcial não está implementado; recibos e movimentos existentes são preservados.

## Identidade visual e tema

- `company_appearance` é um singleton (`id=1`) com paleta, referência local do logo, versão e responsável pela alteração. Referencia a empresa existente de menor ID na migração, quando houver; sem empresa, a referência fica nula. Não há empresa fictícia nem nova tabela de identidade. A exclusão da empresa apenas desvincula a aparência.
- Paletas: `jade` (padrão), `ocean`, `violet`, `terracotta`, `amber`, `graphite`. As cores globais e dos módulos usam tokens semânticos de `index.css`, com conjuntos claro/escuro; estados de erro, sucesso e aviso preservam seu significado.
- Administração em `/settings/appearance`, protegida por `company.appearance.manage`; dados visuais e imagem são lidos por qualquer usuário autenticado. V4 concede a permissão aos cargos que já têm os três poderes administrativos de acesso. Demais concessões usam a tela de cargos.
- Logos: PNG/JPEG até 2 MiB, até 4096 pixels por lado e 16 milhões de pixels; conteúdo decodificado e regravado sem metadados. SVG não é aceito. Arquivo com UUID gerado pelo servidor em `${user.home}/.jamshub/appearance`, independente da pasta de execução; diretório pode ser substituído pela propriedade `erp.appearance.logo-directory`. Nenhum caminho ou nome enviado pelo cliente é usado para armazenamento. A configuração de banco/e-mail não mudou.
- Alterações globais exigem a versão atual; conflito retorna 409. O registro é travado durante substituição/leitura da imagem, com limpeza do arquivo novo após rollback e do anterior após commit. Paleta, substituição e remoção geram eventos de auditoria.
- `users.theme_preference` aceita `LIGHT`, `DARK` ou NULL (padrão claro). `/preferences/current` deriva o usuário somente da sessão e não altera senha, permissões ou versões de credencial/sessão. A escolha entre Claro e Escuro fica em Minha conta; a ausência de preferência usa Claro, independentemente do dispositivo. Preferências pessoais em cache são separadas por ID de usuário; a API é a fonte definitiva.
- Paleta em cache e tema claro são aplicados antes de montar React; após autenticar, a página espera a consulta de aparência/preferência e respeita a escolha salva, inclusive Escuro. `data-palette` e `data-theme` ficam na raiz. Falha na consulta permite continuar com aparência local e aviso; outras abas são atualizadas por BroadcastChannel, e outros navegadores ao abrir/refocar a aplicação.

## Cadastro da empresa

- `GET /company/profile` exige `company.read`; retorna `CompanyProfile {id, legalName, tradeName, taxId, stateReg, municipalReg, taxRegime, address, version}` com `Cache-Control: no-store`. Sem vínculo, `id` e dados cadastrais são nulos e a versão continua disponível.
- `PUT /company/profile` exige `company.read` e `company.manage`. Corpo completo sem ID: os mesmos campos cadastrais, endereço obrigatório e `version >= 0`. Retorna o perfil salvo com nova versão; 400 para dados inválidos/CNPJ já usado por outra empresa; 409 para versão obsoleta ou conflito de banco.
- Endereço: `{country, state, city, street, neighborhood, number, complement, zipCode}`. Razão social, CNPJ, regime e endereço completo são obrigatórios; nome fantasia, inscrições e complemento são opcionais. CNPJ usa `CustomerTaxId` (PJ); CEP aceita 8 dígitos com hífen opcional, persiste formatado; UF normalizada e validada. Campos textuais são aparados e limitados ao schema.
- V11 adiciona `company_appearance.profile_version`, independente da versão de aparência, e permissões `company.read`/`company.manage`. Concede ambas aos cargos com os três poderes administrativos já existentes. Demais concessões pela tela de cargos.
- `CompanyProfileRepository` usa JDBC no padrão de aparência. Escrita transacional trava o singleton antes de comparar versão; cria e vincula uma empresa apenas quando não há vínculo. Atualiza somente identidade/regime e o endereço de menor ID, preservando endereços adicionais, ambiente/série/numeração/certificado legados e aparência. Não escolhe outro emissor pelo CNPJ nem oferece exclusão/troca de empresa.
- Consulta usa uma instrução SQL para perfil/endereço/versão consistentes. Captura do demonstrativo trava o singleton em modo compartilhado antes de ler emissor/endereço para impedir mistura durante edição. Snapshots emitidos permanecem imutáveis.
- Frontend em `/settings/company`, em Configurações, com consulta de CEP existente, carga/erro/recarregamento, confirmação para descartar edição, salvamento e tratamento de conflito. `company.read` permite somente consulta; `company.manage` habilita edição. Auditoria `COMPANY_PROFILE_UPDATED` registra empresa/operador, sem CNPJ, endereço ou segredos no evento.
- Certificado, senha, token de provedor e emissão fiscal real continuam fora deste cadastro.


## Validação conhecida e pendências

- Exercitados no navegador contra o banco principal, com autorização: login/logout e retomada de rota; cadastros básicos; edição/busca de produto; apresentações e opções compostas; configuração de estoque; compra, confirmação, recebimento integral e em duas entregas; venda, reserva, despacho e consulta do demonstrativo; pagamento, recebimento, estorno de recebível antes do despacho e cancelamento com liberação da reserva.
- Empresa: cadastro inicial em validação anterior, edição/persistência, CNPJ inválido, consulta de CEP e descarte de alterações exercitados. Nome fantasia temporário restaurado. Snapshots emitidos conservaram dados anteriores do emissor. Isso não valida o bootstrap nem uma instalação nova completa.
- Estoque e painel refletiram as operações fictícias observadas. Testes manuais em 390 px e por teclado cobriram partes dos fluxos; não constituem aceite completo de acessibilidade/mobile. Os registros TESTE SQL 1711 e TESTE UX 1711 permanecem no banco e influenciam indicadores.
- Scripts V1–V11 executados em ordem em PostgreSQL 18.4 standalone isolado; assertions de schema/inicialização limpa passaram. Não equivale a aplicação e validação pelo Flyway. Compilação backend, testes/build frontend e testes específicos do dashboard passaram em validações anteriores; não comprovam a versão atual após alterações locais recentes.
- Pendentes: suíte backend completa em banco isolado com rede funcional; Flyway/checksums/reinício e validação JPA do schema; bootstrap, CSRF inválido e acesso negado por cargo; concorrência e retries de rede; leitor real e geração/concorrência das sugestões; estorno de compra e demais casos financeiros não exercitados. Lint tinha ocorrências preexistentes, ainda sem resolução confirmada.
- Pendentes também: instalação nova com empresa vazia, upload/combinações de aparência, aceite integral por teclado/celular e impressão. O botão de impressão não abriu prévia no navegador integrado, e a tentativa no Edge foi interrompida; não há defeito de impressão confirmado.
- Os 11 achados de usabilidade continuam no plano ativo. A implementação da Etapa 1 foi autorizada em outro chat, mas sua conclusão ainda não está confirmada nestes documentos. Menu móvel já recebeu implementação separada; validar antes de repetir esse trabalho.

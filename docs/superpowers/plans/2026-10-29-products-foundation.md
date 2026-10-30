# Produtos — base integrada do backend

> **Para execução por agente:** usar superpowers:executing-plans sequencialmente em outro chat autorizado. Seguir [plano coordenador](2026-10-29-products.md), AGENTS.md e as restrições do usuário; sem testes, build, lint, SQL, aplicação de migrations, serviços, commits ou delegação automática.

**Objetivo:** adaptar integralmente os contratos existentes para catálogo/variações, apresentações e quantidades decimais, sem perder IDs, histórico ou proteções operacionais.

**Arquitetura:** ProductCatalog contém os dados comuns; Product continua sendo o SKU operacional. Apresentações de venda e opções de compra são independentes. Linhas comerciais guardam quantidade contratada e equivalente base, e estoque trabalha exclusivamente na base.

**Tecnologias:** stack atual, sem dependências novas. **Desenho:** [Produtos aprovado](../specs/2026-10-29-products-design.md). Estado: plano aprovado pelo usuário em 29/10/2026; nenhuma implementação executada. Execução somente em outro chat expressamente autorizado.

## Restrições globais e revisão

Aplicam-se integralmente as restrições, decisões e cinco pontos de revisão do plano coordenador. Ler o estado do Git e os equivalentes de customer antes de modificar. A aplicação só fica estruturalmente consistente ao completar todas as tarefas abaixo. Os cenários são critérios de inspeção e roteiro futuro; não são comandos autorizados.

## Estrutura de arquivos e contratos compartilhados

Prefixo Java abaixo: backend/src/main/java/com/pi/erp/. Enumerar arquivos concretos em cada tarefa; nomes apresentados aqui são contratos entre tarefas.

| Pacote | Criar | Responsabilidade |
| --- | --- | --- |
| product/catalog | ProductCatalog, ProductCatalogType, ProductCatalogController, ProductCatalogService, ProductCatalogRepository, ProductCatalogFilter, RequestProductCatalogDTO, PatchProductCatalogDTO, RequestCatalogItemDTO | Agregado principal, SIMPLE/VARIANT, consulta agrupada e cadastro atômico |
| product | ProductUnit, ProductCharacteristic, ProductQuantity | Enum fixo de unidades/características e validação/conversão decimal do domínio |
| product/presentation | ProductPresentation, ProductPresentationController, ProductPresentationService, ProductPresentationRepository, ProductPresentationFilter, RequestProductPresentationDTO, PatchProductPresentationDTO | Embalagens e preço próprio de venda |
| supplier/product/option | ProductSupplierOption, ProductSupplierOptionController, ProductSupplierOptionService, ProductSupplierOptionRepository, ProductSupplierOptionFilter, RequestProductSupplierOptionDTO, PatchProductSupplierOptionDTO | Opções comerciais de compra do mesmo vínculo |
| product/price | ProductPriceService | Resolução do preço por cliente, SKU e apresentação |

Não criar pastas globais de controllers/services nem DTOs de resposta. Novas entidades usam mapeamentos e callbacks correspondentes aos campos realmente adicionados; enums e valores simples não recebem datas fictícias.

### Identidade, campos e respostas

- ProductCatalog: id, name, brand, category opcional, unit opcional somente em legado pendente, type SIMPLE/VARIANT, characteristics (Set<ProductCharacteristic>), active, createdAt e updatedAt. Relação items não serializada como coleção automática.
- Product: manter id, sku, cost, price, peso/dimensões e campos opcionais existentes, active próprio e datas; acrescentar catalog, attributes (Map<ProductCharacteristic,String>) e variantKey. Dados comuns name/brand/category/unit passam a ser acessores delegados ao catálogo. Serializar catalogId, atributos, active efetivo e ownActive; ocultar retorno catalog inteiro para evitar ciclo/coleções indiretas.
- SIMPLE concentra a situação no catálogo e mantém ownActive=true no único item interno. Na migração, copiar a situação antiga para o catálogo e ajustar a flag interna para true, preservando a disponibilidade efetiva original. PATCH /product/{id} de active em SIMPLE altera o catálogo; em VARIANT altera somente o próprio item. Isso evita que um simples legado inativo permaneça indevidamente inativo após reativar o principal.
- ProductUnit: UN, KG, G, L, ML, M. ProductCharacteristic: COLOR, SIZE, VOLTAGE, POWER, CAPACITY, MODEL. ProductCatalogType: SIMPLE, VARIANT. Rótulos em português pertencem ao frontend.
- ProductPresentation (product_presentations): id, product, name, baseFactor, basePresentation, price opcional, active. Uma apresentação base por SKU, fator 1, nome correspondente à unidade; as demais são embalagens. Fator 1 não transforma automaticamente uma embalagem nomeada em apresentação base.
- ProductSupplierOption (product_supplier_options): id, productSupplier, name, baseFactor, basePresentation, cost, avgDeliveryDays opcional, supplierProductCode opcional, active e conversionConfirmed. ProductSupplier preserva id/product/supplier/preferred/active e referência opcional legacyOption. Dados antigos cost/prazo/código são expostos por essa opção identificada, nunca por posição da coleção.
- Linhas de venda/compra: acrescentar apresentação/opção, baseQuantity, presentationNameSnapshot, baseFactorSnapshot, unitSnapshot, productNameSnapshot, skuSnapshot e attributesSnapshot; nomes de atributos do snapshot seguem os enums. quantity é comercial e unitPrice/unitCost é preço/custo por apresentação. Campos de snapshot só de saída, preenchidos pelo servidor.
- Não inventar SKU no catálogo; ProductRepository.nextSkuNumber e SkuGenerator continuam atribuindo cada SKU concreto. Novos IDs de catálogo não substituem product_id nas FKs operacionais.

### Interfaces entre tarefas

- ProductQuantity.requireBase(BigDecimal value, ProductUnit unit, boolean allowZero): BigDecimal; ProductQuantity.requireSignedBase(BigDecimal value, ProductUnit unit): BigDecimal; ProductQuantity.toBase(BigDecimal quantity, BigDecimal factor, ProductUnit unit, boolean basePresentation): BigDecimal. Rejeitar nulo, faixa ou precisão indevida; requireSignedBase admite ajuste negativo não zero. Retorno pode padronizar escala após validar, sem arredondar.
- ProductCatalogService.search(ProductCatalogFilter filter, Pageable pageable): Page<ProductCatalog>; register(RequestProductCatalogDTO data): ProductCatalog; update(Long id, PatchProductCatalogDTO data): ProductCatalog; registerItem(Long id, RequestCatalogItemDTO data): Product. Operações de cadastro compostas são transacionais.
- ProductCatalogRepository.findByIdForUpdate(Long id): Optional<ProductCatalog>; ProductRepository.findByIdForUpdate(Long id): Optional<Product>, com locks explícitos. ProductService mantém register/update/delete e delega cadastro de agregado/novo item ao catálogo sem criar dependência circular.
- ProductPriceService.resolve(Long customerId, Product product, ProductPresentation presentation, LocalDate date): BigDecimal. Não aplica preço manual nem desconto; essas autorizações continuam no service de pedidos.
- ProductPresentationService.findForProduct(Long productId, Long presentationId): ProductPresentation; ProductSupplierOptionService.findForPurchase(Long supplierId, Long productId, Long optionId): ProductSupplierOption. ID omitido seleciona apresentação base de venda; na compra só selecionar implicitamente se existir exatamente uma opção ativa com conversão confirmada.
- Incluir demais assinaturas CRUD no padrão search/filter, register/data, update/id/data e delete/id apenas onde houver exclusão aprovada. Coleções de características e atributos não têm CRUD de tipos.

### Corpos dos novos cadastros

RequestProductCatalogDTO: name, brandId, categoryId, unit, type, characteristics, active, @Valid items. Nome/marca/unidade/tipo obrigatórios; items contém exatamente um item SIMPLE e ao menos um VARIANT. RequestCatalogItemDTO: cost, price, weightKg, color, dimensionX/Y/Z, size, active, attributes e @Valid stock no formato atual com quantidades BigDecimal. Não exigir custo/preço no agrupador; exigir em cada item. O servidor gera SKU.

PatchProductCatalogDTO: name, brandId, categoryId, unit, characteristics, active, removeCategory Boolean. Tipo não editável; conjunto de características imutável após os primeiros itens. Unit só pode ser definida em legado pendente ou corrigida sem qualquer vínculo operacional/estoque/apresentação não base; se usada, rejeitar troca.

Request/PatchProductDTO atuais: preservar campos existentes, acrescentar catalogId/unit/attributes e flags de remoção para opcionais numéricos quando necessários à limpeza. Cadastro novo sem catalogId é simples e exige nome/marca/unit no service; com catalogId exige catálogo VARIANT, atributos completos e não aceita mudar dados comuns no mesmo corpo. PATCH de item de catálogo SIMPLE pode encaminhar campos comuns ao agregado; em VARIANT rejeitar essa tentativa com mensagem específica, usando o endpoint principal para dados comuns. Remover somente as constraints incondicionais de nome/marca incompatíveis com o cadastro condicional; manter os demais limites de negócio.

## Tarefa 1 — Schema incremental e decimal de domínio

**Arquivos:** criar ProductUnit.java, ProductCharacteristic.java e ProductQuantity.java em product; propor backend/src/main/resources/db/migration/V7__product-catalog-and-presentations.sql e V8__commercial-quantity-snapshots.sql. Conferir numeração novamente na execução; V1–V6 permanecem intactas.

**Produz:** schema completo abaixo e interfaces ProductQuantity; **consome:** desenho e inventário atual, não o banco em execução.

- [ ] Criar product_catalogs e tabelas product_catalog_characteristics e product_variant_values. Acrescentar products.catalog_id e variant_key. Para cada produto antigo, criar catálogo SIMPLE próprio preservando nome/marca/categoria/situação/datas registradas; não agrupar automaticamente por semelhança.
- [ ] Mover nome/marca/categoria para o cadastro principal, atualizar FKs/índices dependentes e retirar as colunas redundantes somente depois de copiar/conferir no próprio script. Não mudar product_id, SKU nem referências. Manter color/size legados em Product simples; novos atributos de variações usam a tabela de valores e getters compatíveis para COLOR/SIZE, sem duas fontes editáveis concorrentes.
- [ ] Legado sem unidade: manter unit nula e bloquear operações novas até confirmação pelo cadastro; não extrair unidade de commercial_unit fiscal nem deduzir por nome. Exigir que a confirmação reconheça a unidade já representada pelos números anteriores, sem converter saldos. Campo definido não modifica snapshots históricos ausentes. Se o saldo antigo estiver em caixas e o desejado for unidades avulsas, manter a pendência: isso exige conversão de dados com desenho/autorização próprios, não se resolve trocando o rótulo da unidade.
- [ ] Criar apresentações de venda e opções de fornecedor. Para vínculo antigo, copiar custo/prazo/código/situação para legacyOption; conservar a identidade do vínculo. Remover cost/avg_delivery_days/supplier_product_code de product_suppliers somente após a cópia e substituição por acessores delegados na entidade, evitando que o antigo cost NOT NULL impeça criar vínculos novos. Nova opção exige fator/unidade conhecidos; opção legada fica conversionConfirmed=false até conferência humana do significado do custo e da embalagem. Não aplicar fator 1 automaticamente como equivalência confirmada.
- [ ] Criar unicidade do par catálogo/variantKey e de apresentação base por SKU; chave normalizada de variação é JSON canônico das características ordenadas pelo nome do enum com valores trim e minúsculas Locale.ROOT, preservando acentos/espaços internos. SIMPLE usa chave vazia. Características são fixas, valores não vazios e exatamente iguais ao conjunto escolhido.
- [ ] Preferência: índice único parcial product_suppliers(product_id) WHERE preferred. Antes de criá-lo, detectar conflito legado e interromper a migration com erro específico, sem apagar preferências nem escolher vencedor. Documentar que resolver conflitos exige escolha humana antes da aplicação autorizada, inclusive usando PATCHs existentes se aplicável.
- [ ] Converter para NUMERIC(14,1): stock_items quantity/reserved_quantity/min_quantity/max_quantity; stock_reservations.quantity; stock_movements.quantity/previous_balance/new_balance; order_items.quantity; purchase_items.quantity/received_quantity; purchase_receipt_items.quantity. Preservar valores, nulabilidade de máximo e constraints de positividade existentes.
- [ ] Acrescentar base_quantity às linhas comerciais e snapshots definidos acima. Legados começam com base_quantity igual ao quantity histórico, fator numérico 1 representando a semântica antiga da aplicação, sem declarar uma embalagem desconhecida. unit/name/características históricas não recuperáveis permanecem sem informação histórica; não preencher por valores atuais fingindo contratação original. Preços, descontos e totais antigos não são recalculados.
- [ ] Quantity comercial de compra e received_quantity deixam de ter mesma dimensão: received_quantity permanece BASE; comparações de pendência usam base_quantity. Recebimento e reserva mantêm FK da linha, e não apenas produto.
- [ ] Implementar validação Java antes da conversão/persistência: stripTrailingZeros().scale() <= 0 para UN, <= 1 para demais; limite absoluto 9999999999999.9 inclusive agregados. Quantidade de embalagem composta inteira; fator e resultado obedecem precisão/unidade base. Rejeitar antes de o PostgreSQL arredondar para a escala da coluna.

**Revisão:** conferir cópia/backfill e constraints textualmente; não escrever/aplicar migrations neste chat. Na implementação futura, apenas preparar scripts, sem aplicá-los automaticamente. Tabelas de sugestões/fiscal sem módulos ficam inventariadas como dependências futuras, sem implementar funcionalidades ou alterar suas quantidades por arrasto.

## Tarefa 2 — Catálogo, identidade e situação efetiva

**Arquivos:** criar os arquivos product/catalog da matriz; alterar product/Product.java, ProductService.java, ProductRepository.java, ProductController.java, ProductFilter.java, RequestProductDTO.java e PatchProductDTO.java; alterar product/brand/Brand.java e product/category/Category.java, supplier/product/ProductSupplierService.java e repositories com caminhos JPA antigos.

**Produz:** agregado e APIs de catálogo/itens; **consome:** Tarefa 1. O catálogo usa productRepository e stockItemService diretamente para o cadastro composto; ProductService pode delegar cadastro ao catálogo, sem catálogo depender de ProductService.

- [ ] Implementar entidade, DTOs e serviços conforme contratos. Para criar catálogo e itens, resolver marca/categoria e validar todos os valores/combinações antes de salvar. Persistir pai, itens/SKUs e eventual estoque inicial na mesma transação. Exigir stock.configure antes de qualquer estoque inicial. SIMPLE usa active do principal; item interno aceita active omitido/true, rejeitando false isolado para não criar situação escondida. O adaptador do POST /product encaminha seu active original ao principal.
- [ ] Preservar getters JSON name/brand/category em Product delegando ao catálogo; corrigir mappedBy de Brand/Category para catálogos e filtros JPA para product.catalog.brand/category/name. Revisar ProductSupplierFilter e filtros de fornecedores por produto sem alterar o significado independente de supplierBrands/supplierCategories.
- [ ] Implementar situação própria e efetiva: Product.isActive() retorna catalog.active && active próprio; ownActive informa a flag própria, sempre true no item interno SIMPLE. Atualizar setters apenas na identidade apropriada; inativação do principal muda só o catálogo. O SKU não muda por edição de dados ou atributos.
- [ ] GET /productCatalog/search -> Page<ProductCatalog>, page 0..100000, size 1..100 padrão25, ordem name/id. ProductCatalogFilter(name, sku, brandId, categoryId, active, query) combina campos específicos em AND; query busca nome OU SKU OU valores de atributos por EXISTS, sem fetch de coleção na paginação.
- [ ] GET /productCatalog/{id} -> entidade ou 404; POST ->201; PATCH ->200. GET /productCatalog/{id}/items/search -> Page<Product>, mesmas faixas, ordem sku/id, filtros query/active. POST /productCatalog/{id}/items ->201 com RequestCatalogItemDTO. /product/search mantém List<Product>, filtros anteriores mais catalogId, com situação efetiva; não usar essa lista para renderizar o catálogo principal.
- [ ] SIMPLE possui exatamente um item; VARIANT exige atributos completos e combinação única. Não oferecer mudança de tipo, agrupamento de legados ou reparenting de item. Duplicidade concorrente é barrada pelo índice e tratamento centralizado.
- [ ] Preservar DELETE /product/{id} para consumidores anteriores, sem botão na tela. Bloquear item com referências; impedir deixar catálogo VARIANT vazio. Se excluir item SIMPLE sem relações, remover seu catálogo na mesma transação, sem cascata que apague histórico/estoque. Não criar DELETE de agrupador na interface nova.
- [ ] Bloqueio de edição/cadastro: catálogo primeiro, itens por ID crescente; dados comuns e unidade são lidos sob esse bloqueio. Operações de pedidos/compras tomam o lock do documento antes dos catálogos/itens, e estoque depois; edições de catálogo não tomam locks de documentos. Manter essa ordem em novos caminhos, sem inverter locks existentes.

**Revisão:** grupos jamais são aceitos por FKs de estoque; busca por variação retorna um principal; página/contagem sem repetição; SIMPLE não exige variante na UX; metadata do pai não é apresentado como atualização dos filhos.

## Tarefa 3 — Apresentações, fornecedores e resolução de preços

**Arquivos:** criar os arquivos product/presentation, supplier/product/option e product/price da matriz; alterar supplier/product/ProductSupplier.java, ProductSupplierService.java, ProductSupplierRepository.java, RequestProductSupplierDTO.java, PatchProductSupplierDTO.java e ProductSupplierFilter.java; alterar price/item/PriceTableItem.java, PriceTableItemService.java, PriceTableItemRepository.java, RequestPriceTableItemDTO.java, PatchPriceTableItemDTO.java e PriceTableItemFilter.java. Ajustar schema da Tarefa 1 para price_table_items.presentation_id.

**Produz:** findForProduct/findForPurchase/resolve e CRUD; **consome:** catálogo/SKU/unidade e precisão.

- [ ] POST/PATCH apresentação: productId no cadastro, name, baseFactor, price opcional, active; basePresentation é criada pelo servidor. A apresentação base tem fator1, preço próprio nulo e situação controlada pelo SKU, sem duplicar o campo Product.price. Exigir nome/fator positivo, inteiros em UN e fator imutável após uso; permitir limpar price de embalagem por removePrice Boolean. Nomes normalizados únicos por SKU + fator, sem proibir embalagens de quantidades diferentes. Não mover apresentação para outro SKU no PATCH.
- [ ] POST/PATCH opção de fornecedor: productSupplierId no cadastro, name, baseFactor, basePresentation, cost, avgDeliveryDays, supplierProductCode, active e confirmConversion Boolean. Custo obrigatório não negativo, prazo inteiro não negativo, zero preservado, removeAvgDeliveryDays Boolean para limpar. Para confirmar opção legada pendente, exigir confirmConversion=true e valores explícitos de fator/tipo/custo; persistir conversionConfirmed no servidor somente após validar o conjunto. Situação e preferência no vínculo não se confundem com situação da opção.
- [ ] GET /productPresentation/search e /productSupplierOption/search retornam páginas por productId/productSupplierId, query/active, ordem name/id; limites iguais ao catálogo. Novos controllers seguem search/findOne/register/update, sem exclusão física: inativar opções/apresentações usadas. Retornar entidades sem serializar todas as opções por acidente no vínculo.
- [ ] Proteger preferência sob lock do catálogo e Product concreto, depois vínculos; ao escolher um, limpar preferência dos outros e salvar/flush antes de atribuir a nova para respeitar o índice parcial. Sem preferencial também é válido. Inativação não escolhe substituto automaticamente; a preferência não supera a situação efetiva.
- [ ] Preservar /productSupplier e par único. Acrescentar primeira opção ao cadastro quando informada; campos antigos cost/prazo/código são adaptadores de legacyOption, preservando consumidores anteriores. Se não houver legacyOption, rejeitar escrita nesses campos com mensagem pedindo optionId, sem usar a primeira opção arbitrariamente. Busca antiga por custo/código/prazo passa a usar EXISTS de opções e não duplica vínculos.
- [ ] Preço de tabela: presentation_id nulo significa preço base do SKU, preservando linhas antigas. Substituir uk_price_table_item por unicidade parcial de (price_table_id,product_id) para linhas base e de (price_table_id,presentation_id) para específicas; validar que a apresentação pertence ao produto e não duplicar preço da apresentação base com linha nula. Modificar os existsBy.../AndIdNot para a identidade nova.
- [ ] RequestPriceTableItemDTO/PriceTableItemFilter acrescentam presentationId; PatchPriceTableItemDTO acrescenta presentationId e removePresentation Boolean. Omitir/null mantém, remover explicitamente volta ao preço base e remover junto com novo ID é contraditório. Preservar os campos e permissões anteriores e validar unicidade usando o estado final.
- [ ] Implementar resolve: tabela atribuída ao cliente ativa/vigente -> preço específico da apresentação, senão base dessa tabela × fator; sem preço aplicável -> preço próprio da apresentação, senão preço base do SKU × fator. Zero é preço definido. Arredondar preço derivado para centavos HALF_UP antes de registrar, validar limite monetário atual e não selecionar tabela mais barata automaticamente.
- [ ] Não atualizar custo de referência por cotação ou compra; custo informado na compra refere-se à opção, não a uma unidade base presumida. Prazo da opção prevalece, inclusive zero; ausente usa prazo geral; nenhum gera data de entrega garantida.

**Revisão:** mesmo fornecedor oferece pacote6/caixa24 com custos/códigos próprios; preços de venda independentes; caixa54 no catálogo e tabela unitária4 resulta em48; manter a leitura autenticada por domínio na Tarefa7.

## Tarefa 4 — Estoque, movimentações e reservas decimais

**Arquivos:** alterar stock/StockItem.java, StockItemService.java, StockItemRepository.java, RequestStockItemDTO.java, PatchStockItemDTO.java; stock/movement/StockMovement.java, StockMovementService.java, RequestStockMovementDTO.java; stock/reservation/StockReservation.java, StockReservationService.java, StockReservationRepository.java; product/RequestProductDTO.java (InitialStockDTO). Atualizar filtros/repositories desses pacotes somente onde houver comparação/assinatura de quantidade.

**Produz:** saldos/reservas/movimentos BigDecimal em unidade base; **consome:** ProductQuantity, Product.unit e baseQuantity das linhas.

- [ ] Substituir Integer de quantidade/saldo/limites por BigDecimal, inclusive construtores, mapas de agregação e assinaturas privadas. Não alterar IDs, prazo, operadores ou enumerações por busca/substituição global.
- [ ] register/update de estoque recebem valores BASE. Preservar máximo opcional e removeMaxQuantity Boolean, validação combinado mínimo/máximo, unicidade depósito/SKU e restrição de mudar produto/depósito com histórico/reservas. Operações administrativas para saldo de item inativo continuam disponíveis conforme permissão.
- [ ] Movimentos usam soma/subtração exata e comparação por signum/compareTo, preservando INBOUND/OUTBOUND positivos e ADJUSTMENT assinado não zero, saldo não negativo nem abaixo de reservas. Preservar registros compensatórios e ausência de PATCH/DELETE de histórico.
- [ ] StockReservationService.reserve agrega orderItem.baseQuantity em Map<Long,BigDecimal> por stockItemId, bloqueia estoque por ID crescente e cria reserva por linha com sua baseQuantity. Dois pacotes do mesmo SKU não são duas reservas para a quantidade errada.
- [ ] release/prepareDispatch comparam reserva e baseQuantity por compareTo ==0, preservam estados e condições originais. Trocar todas as igualdades BigDecimal.equals que fariam 1 e1.0 divergir; não revalidar disponibilidade ativa do catálogo ao concluir compromisso confirmado.
- [ ] registerSale desconta reserva BASE; registerPurchase acrescenta receiptItem.quantity BASE. Nenhuma dessas duas funções multiplica novamente pelo fator. Preservar vínculos e unicidade de movimento por origem para repetição segura.

**Revisão:** 120 unidades − pacote6 =114; 1,5KG funciona e1,25KG é rejeitado; saldo/reserva/min/máximo têm mesma unidade; agregação nunca passa por longValue/intValue e respeita faixa decimal.

## Tarefa 5 — Vendas e preços comerciais preservados

**Arquivos:** alterar order/item/OrderItem.java, RequestOrderItemDTO.java, PatchOrderItemDTO.java, OrderItemService.java, OrderItemRepository.java; order/OrderService.java; order/dispatch/OrderDispatchService.java somente onde novas quantidades/validações exigirem. Não criar telas de Vendas.

**Produz:** linha comercial com snapshots/baseQuantity e totais coerentes; **consome:** apresentações/preço e reservas BASE.

- [ ] DTOs mantêm orderId/productId/warehouseId; quantity BigDecimal; acrescentar presentationId e removePresentation Boolean no PATCH para voltar explicitamente à base. Omitir/null preserva apresentação atual, salvo troca de produto que deve escolher apresentação válida do novo SKU, nunca reaproveitar a antiga.
- [ ] Ao incluir/mudar produto ou apresentação, validar item e catálogo ativos, unidade confirmada, apresentação ativa pertencente ao SKU e existência de estoque no depósito. Resolver preço via ProductPriceService; preço manual/desconto continuam exigindo as permissões atuais. Mudança apenas de quantidade não atualiza preço/fator contratado.
- [ ] Gravar quantidade comercial, fator, unidade e baseQuantity; preencher snapshots do item no servidor. Alterações futuras no cadastro não mudam rascunho já contratado automaticamente. Mudança explícita de apresentação/produto no pedido pendente recria os snapshots dessa linha.
- [ ] Permitir mesmo productId em apresentações diferentes. Substituir a duplicidade por (orderId,productId,warehouseId,presentationId); linha idêntica já existente gera conflito em vez de somar silenciosamente. Atualizar tanto ItemService quanto validateConfirmation, não apenas repository.
- [ ] Recalcular totais e query calculateTotalByOrderId com o mesmo arredondamento por linha definido no coordenador. Confirmar validação de desconto AMOUNT contra bruto arredondado e PERCENTAGE até100, limite monetário e total; não converter preço de caixa para preço base arredondado para depois multiplicar.
- [ ] Confirmação revalida disponibilidade efetiva e snapshot, bloqueia catálogo/item e usa baseQuantity na reserva. Não resolver novamente preço/fator contra o cadastro no momento de confirmar. Após CONFIRMED, despacho/cancelamento usa reserva/contrato preservados e não é bloqueado apenas pela inativação posterior.

**Revisão:** uma caixa12 mais2avulsas reserva14; caixa54 não se torna60; tabela do cliente prevalece; inativação impede confirmar PENDING mas não impede despacho do CONFIRMED; valores antigos não são recalculados pela migração.

## Tarefa 6 — Compras, opções e recebimento parcial

**Arquivos:** alterar purchase/item/PurchaseItem.java, RequestPurchaseItemDTO.java, PatchPurchaseItemDTO.java, PurchaseItemService.java, PurchaseItemRepository.java; purchase/PurchaseService.java; purchase/receipt/PurchaseReceiptItem.java, RequestPurchaseReceiptDTO.java, PurchaseReceiptService.java e PurchaseReceiptItemRepository.java; stock/movement/StockMovementService.java conforme Tarefa4. Não criar telas de Compras.

**Produz:** compra por opção, pendência BASE e repetição exata; **consome:** ProductSupplierOption, snapshots e estoque decimal.

- [ ] DTO de item acrescenta supplierOptionId e quantity BigDecimal; unitCost continua opcional de entrada conforme contrato atual e refere-se à apresentação. Na omissão de opção, só aceitar única opção ativa/conversão confirmada do par; mais de uma exige escolha. PATCH omitido/null mantém opção, troca de produto exige opção válida para o novo par.
- [ ] Validar fornecedor/vínculo/opção/item/catálogo ativos e unidade/conversão confirmadas ao adicionar/alterar compromisso DRAFT e ao sair de DRAFT para SENT. Confirmar também a disponibilidade nas transições que criam compromisso novo, sem reabrir compras SENT já contratadas por mera inativação posterior. Documentar SENT como fronteira contratada do fluxo existente; CONFIRMED/PARTIALLY_RECEIVED seguem o contrato já assumido.
- [ ] Armazenar quantidade/custo comerciais e baseQuantity/snapshots. Permitir linhas do mesmo SKU em opções diferentes; identidade de duplicidade (purchaseId,productId,warehouseId,supplierOptionId). Atualizar validação do documento e repository, preservando pendência/recebimento por linha.
- [ ] receivedQuantity passa a BigDecimal BASE e começa emZERO. Comparar recebido e pendente com baseQuantity; não comparar quantidade base recebida a número de caixas. existsByPurchaseIdAndReceivedQuantityGreaterThan recebe BigDecimal.ZERO.
- [ ] Ajustar totais Java e calculateTotalByPurchaseId para ROUND(quantity * unit_cost,2) por linha. Alterar cadastro/opção depois não muda preço/fator de compra já registrada. Custo de referência do SKU permanece manual.
- [ ] RequestPurchaseReceiptDTO.ItemDTO.quantity BigDecimal é recebido BASE, validado contra unidade snapshot e saldo pendente; linhas com histórico de unidade ausente exigem primeiro conferência assistida da unidade legada, sem reescrever quantidades históricas. A resposta identifica baseQuantity/receivedQuantity/unidade de forma não ambígua.
- [ ] Mapas de solicitação/persistência usam BigDecimal e igualdade numérica. Mesmo requestId com mapa equivalente, inclusive18 versus18.0, retorna o recibo anterior sem entrada nova; com conteúdo diferente, rejeita. Preservar lock da compra, itens/estoque em ordem e o grafo de serialização corrigido no módulo atual.
- [ ] Excesso/erro em qualquer linha reverte recibo, saldo e histórico juntos. Compra2caixas12/recebimento18 deixa6BASE pendentes e PARTIALLY_RECEIVED; completar6 muda para RECEIVED. Inativação posterior do produto não impede avançar uma compra já SENT para CONFIRMED nem receber compra CONFIRMED/PARTIALLY_RECEIVED. SENT continua sem recebimento direto, preservando a máquina de estados atual; disponibilidade do operador e demais autorizações permanecem exigidas.

**Revisão:** pacote6 ecaixa24 do mesmo fornecedor preservam custo/código/fator por linha; duas linhas do mesmo SKU/depósito somam entradas sem sobrescrita; soma decimal e repetição não duplicam saldo.

## Tarefa 7 — Permissões, serialização, compatibilidade e entrega da base

**Arquivos:** auth/config/SecurityConfig.java; repositories/controllers alterados nas tarefas anteriores; docs/superpowers/plans/2026-10-29-products-progress.md e roteiro manual da base. Revisar frontend/src/services/prices.js e demais consumidores sem implementar catálogo nesta fase.

- [ ] Mapear productCatalog/productPresentation para products.read/create/update; base DELETE existente products.delete. Rotas de itens por catálogo usam products.create no POST e products.read no GET. Não adicionar permissões novas sem necessidade.
- [ ] productSupplierOption GET exige suppliers.read e escrita suppliers.update, como productSupplier atual. Vínculos supplier/{id}/brands e /categories continuam suppliers.read/update. Preços de tabela específicos continuam prices.read/create/update; resolver preço de pedido não concede leitura genérica de fornecedores/tabelas.
- [ ] Produtos já retornam cost a products.read; preservar essa política, sem inventar novo papel de sigilo. Não embutir opções/custos de fornecedores no Product/ProductCatalog, pois isso ampliaria suppliers.read. Consulta de estoque continua stock.read, e estoque inicial stock.configure.
- [ ] Atualizar graphs de Product/StockItem/OrderItem/PurchaseItem/recibos para todas as associações efetivamente serializadas, inclusive catalog.brand/category e atributos necessários. Páginas não fazem fetch de coleção; usar consultas separadas ou carregamento em duas etapas, preservando count e limites.
- [ ] Revisar referência a nome/marca/categoria em ProductService/ProductSupplierService, relações Brand/Category e qualquer consulta nativa/JPQL afetada. Não substituir filtros de supplierBrands/supplierCategories por categorias inferidas dos produtos.
- [ ] Listar contratos alterados e consumidores: /product, /productSupplier, /orderItem, /purchaseItem, /purchase/{id}/receive, /stockItem, /stockMovement e priceTableItem. Ferramentas ignoradas, Insomnia e testes não são adaptados nem executados sem autorização específica.
- [ ] Preparar roteiro manual com os cinco pontos do coordenador, repetição concorrente de preferência/recebimento, rollback, gráfico serializado e permissões negativas. Registrar apenas revisão estática executada, sem marcar cenários como aprovados por leitura.
- [ ] Auto-revisar entidades/DTOs/services contra customer, migrations contra schema anterior e diff próprio. Não iniciar backend, aplicar Flyway ou afirmar compatibilidade em execução. Registrar a necessidade de autorização e ambiente isolado para validação futura.

**Saída:** contratos completos disponíveis para a etapa de Catálogo, nenhuma quantidade operacional ainda dependente de Integer e nenhuma tarefa que precise supor um SKU de agrupador. Divergência funcional real volta ao usuário; detalhes de implementação seguem este plano.

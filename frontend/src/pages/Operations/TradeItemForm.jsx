import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import Pagination from "../../components/feedback/Pagination";
import { products } from "../../services/products";
import { productSuppliers } from "../../services/productSuppliers";
import { productStock } from "../../services/productStock";
import { orders, purchases } from "../../services/operations";
import { useProductResource } from "../Products/useProductResource";
import { ProductLoad } from "../Products/ProductControls";
import useProductForm from "../Products/useProductForm";
import { decimal, quantity, money, textNumber } from "../Products/productFormData";
import { SalesProductPicker, SkuPicker, WarehouseSelect, skuName } from "./OperationSelectors";
import { available } from "./StockPage";
import { salesError } from "./salesErrors";

export default function TradeItemForm({
  purchase = false,
  document,
  item,
  existingItems = [],
  onClose,
  onSaved,
}) {
  const { has } = useAuth();
  const service = purchase ? purchases : orders;
  const permission = `${purchase ? "purchases" : "orders"}.${item ? "update" : "create"}`;
  const form = useProductForm(
    `school:${purchase ? "purchase" : "order"}:${document.id}:item:${item?.id || "new"}`,
    {
      productId: String(item?.product.id || ""),
      warehouseId: String(item?.warehouse.id || ""),
      quantity: textNumber(item?.quantity ?? 1),
      choiceId: String((purchase ? item?.supplierOption?.id : item?.presentation?.id) || ""),
      price: "",
      priceMode: "automatic",
      discount: textNumber(item?.discount ?? 0),
      discountType: item?.discountType || "AMOUNT",
    },
    onClose,
    onSaved,
  );
  const [picker, setPicker] = useState(false);
  const quantityInput = useRef(null);
  const [advanced, setAdvanced] = useState(
    () => form.value.priceMode === "manual" || Number(form.value.discount.replace(",", ".")) > 0,
  );
  const [page, setPage] = useState(0);
  const [checkingCurrentPrice, setCheckingCurrentPrice] = useState(false);
  const [choice, setChoice] = useState(purchase ? item?.supplierOption : item?.presentation);
  const canChangeSaleIdentity = !purchase && has("products.read") && has("warehouses.read");
  const showSalePicker = !purchase && (!item || canChangeSaleIdentity) &&
    (picker || !form.value.productId);
  const canReadStock = has("stock.read");
  const canReadWarehouses = has("warehouses.read");
  const selectProduct = (next) => {
    if (String(next.id) !== form.value.productId) {
      form.setValue((old) => ({
        ...old,
        productId: String(next.id),
        choiceId: "",
        ...(!purchase ? { price: "", priceMode: "automatic", warehouseId: "" } : {}),
      }));
      setChoice(null);
      setCheckingCurrentPrice(false);
      setPage(0);
    }
    setPicker(false);
  };
  const productRequest = useProductResource(
    (options) => products.findItem(form.value.productId, options),
    [form.value.productId],
    Boolean(form.value.productId) && has("products.read"),
  );
  const product =
    String(productRequest.data?.id) === form.value.productId
      ? productRequest.data
      : !purchase &&
          item &&
          !has("products.read") &&
          String(item.product.id) === form.value.productId
        ? item.product
        : undefined;
  const productState = {
    ...productRequest,
    data: product,
    loading: Boolean(
      has("products.read") && form.value.productId && !productRequest.error && !product,
    ),
  };
  const stockRequest = useProductResource(
    async (options) => ({
      scope: form.value.productId,
      records: await productStock.search(form.value.productId, options),
    }),
    [form.value.productId],
    Boolean(form.value.productId) && has("stock.read"),
  );
  const stock = {
    ...stockRequest,
    data: stockRequest.data?.scope === form.value.productId ? stockRequest.data.records : undefined,
    loading: Boolean(
      has("stock.read") &&
      form.value.productId &&
      !stockRequest.error &&
      (stockRequest.loading || stockRequest.data?.scope !== form.value.productId),
    ),
  };
  const choiceScope = `${form.value.productId}:${page}`;
  const choicesRequest = useProductResource(
    async (options) => {
      if (!purchase)
        return {
          ...(await products.presentations(
            form.value.productId,
            { active: true },
            page,
            25,
            options,
          )),
          scope: choiceScope,
        };
      const links = await productSuppliers.search(
        { productId: form.value.productId, supplierId: document.supplier.id, active: true },
        options,
      );
      if (!links.length) return { content: [], totalPages: 0, scope: choiceScope };
      return {
        ...(await productSuppliers.options(links[0].id, { active: true }, page, 25, options)),
        scope: choiceScope,
      };
    },
    [form.value.productId, document.supplier?.id, page, purchase],
    Boolean(product) &&
      (!item || canChangeSaleIdentity) &&
      has(purchase ? "suppliers.read" : "products.read"),
  );
  const choices = {
    ...choicesRequest,
    data: choicesRequest.data?.scope === choiceScope ? choicesRequest.data : undefined,
    loading: Boolean(
      product &&
      (!item || canChangeSaleIdentity) &&
      has(purchase ? "suppliers.read" : "products.read") &&
      !choicesRequest.error &&
      (choicesRequest.loading || choicesRequest.data?.scope !== choiceScope),
    ),
  };
  const selectedChoice =
    choices.data?.content.find((row) => String(row.id) === form.value.choiceId) ||
    (String(choice?.id) === form.value.choiceId &&
    (purchase || String(choice?.productId) === form.value.productId)
      ? choice
      : null);
  const duplicateItem =
    !purchase &&
    existingItems.find(
      (row) =>
        row.id !== item?.id &&
        String(row.product.id) === form.value.productId &&
        String(row.warehouse.id) === form.value.warehouseId &&
        String(row.presentation?.id || "") === form.value.choiceId,
    );
  const resolveSalePrice =
    checkingCurrentPrice ||
    !item ||
    form.value.productId !== String(item.product.id) ||
    form.value.choiceId !== String(item.presentation?.id || "");
  const manualPrice =
    !purchase &&
    (form.value.priceMode === "manual" ||
      (!form.value.priceMode && Boolean(form.value.price.trim())));
  const overridesSalePrice = manualPrice && Boolean(form.value.price.trim());
  const priceScope = `${document.id}:${document.customer?.id}:${form.value.productId}:${selectedChoice?.id}`;
  const priceRequest = useProductResource(
    async (options) => ({
      scope: priceScope,
      amount: await orders.price(document.id, product.id, selectedChoice.id, options),
    }),
    [priceScope],
    !purchase && resolveSalePrice && Boolean(product && selectedChoice),
  );
  const sameSaleIdentity =
    !purchase && item &&
    form.value.productId === String(item.product.id) &&
    form.value.choiceId === String(item.presentation?.id || "");
  const currentQuotedPrice = priceRequest.data?.scope === priceScope
    ? priceRequest.data.amount : null;
  const automaticAmount = sameSaleIdentity ? item.unitPrice : currentQuotedPrice;
  const priceLoading = Boolean(
    !purchase &&
    resolveSalePrice &&
    product &&
    selectedChoice &&
    !priceRequest.error &&
    (priceRequest.loading || priceRequest.data?.scope !== priceScope),
  );
  const displayedPrice = purchase || manualPrice ? form.value.price : textNumber(automaticAmount);
  const currentPriceReady =
    !purchase &&
    item &&
    checkingCurrentPrice &&
    priceRequest.data?.scope === priceScope &&
    currentQuotedPrice != null &&
    !priceRequest.error;
  // Presentation-only calculation: the backend remains authoritative for amounts.
  const preview = (() => {
    if (purchase) return null;
    const normalized = (input) => String(input ?? "").trim().replace(",", ".");
    const count = Number(normalized(form.value.quantity));
    const unit = Number(normalized(displayedPrice));
    const discountInput = has("orders.discount") ? form.value.discount : (item?.discount ?? 0);
    const reduction = Number(normalized(discountInput));
    const discountType = has("orders.discount")
      ? form.value.discountType
      : (item?.discountType || "AMOUNT");
    if (
      !normalized(form.value.quantity) || !normalized(displayedPrice) ||
      !/^\d+(?:\.\d+)?$/.test(normalized(form.value.quantity)) ||
      !/^\d+(?:\.\d+)?$/.test(normalized(displayedPrice)) ||
      !/^\d+(?:\.\d+)?$/.test(normalized(discountInput)) ||
      ![count, unit, reduction].every(Number.isFinite) || count <= 0 ||
      (discountType === "PERCENTAGE" && reduction > 100)
    ) return null;
    const gross = Math.round((count * unit + Number.EPSILON) * 100) / 100;
    const discountValue = discountType === "PERCENTAGE"
      ? Math.round((gross * reduction / 100 + Number.EPSILON) * 100) / 100
      : reduction;
    if (discountValue > gross) return null;
    return { gross, discountValue, net: Math.round((gross - discountValue) * 100) / 100 };
  })();
  useEffect(() => {
    if (
      purchase ||
      page !== 0 ||
      form.value.choiceId ||
      (item && form.value.productId === String(item.product.id))
    )
      return;
    const base = choices.data?.content.find(
      (row) => row.basePresentation && row.active && String(row.productId) === form.value.productId,
    ) || (choices.data?.totalElements === 1 && choices.data.content[0]?.active
      ? choices.data.content[0] : null);
    if (base) {
      setChoice(base);
      form.change("choiceId", String(base.id));
    }
  }, [choices.data, form.value.productId, form.value.choiceId, page, purchase, item?.id]);
  useEffect(() => {
    if (
      purchase || !canReadWarehouses || !canReadStock || stock.loading || stock.error ||
      form.value.warehouseId || (item && form.value.productId === String(item.product.id))
    ) {
      return;
    }
    if (stock.data?.length === 1) {
      form.change("warehouseId", String(stock.data[0].warehouse.id));
    }
  }, [stock.data, stock.loading, stock.error, form.value.productId, form.value.warehouseId,
    purchase, item?.id, canReadStock, canReadWarehouses]);
  useEffect(() => {
    if (!purchase && product && !showSalePicker) {
      quantityInput.current?.focus({ preventScroll: true });
    }
  }, [product?.id, showSalePicker, purchase]);
  const submit = (event) =>
    form.submit(event, async (value) => {
      if (!has(permission)) throw new Error("Sem permissão para salvar este item.");
      if (!product?.unit || String(product.id) !== value.productId)
        throw new Error("Selecione um produto com unidade BASE confirmada.");
      if (!selectedChoice || (purchase && !selectedChoice.conversionConfirmed))
        throw new Error("Selecione uma apresentação/opção com conversão confirmada.");
      if (!value.warehouseId) throw new Error("Selecione o depósito.");
      if (!purchase && overridesSalePrice && !has("orders.manualPrice"))
        throw new Error(
          "Você não tem permissão para preço manual. Use o preço automático para continuar.",
        );
      if (
        !purchase &&
        !overridesSalePrice &&
        resolveSalePrice &&
        (priceLoading || priceRequest.error || automaticAmount == null)
      )
        throw new Error("Aguarde a consulta do preço automático ou tente consultá-lo novamente.");
      if (!purchase && duplicateItem)
        throw new Error("Esta combinação já está no pedido. Edite a linha existente.");
      if (
        !purchase &&
        has("stock.read") &&
        (stock.loading ||
          stock.error ||
          !stock.data?.some((row) => String(row.warehouse.id) === value.warehouseId))
      )
        throw new Error("Confira o estoque e selecione um depósito configurado para este produto.");
      const quantityValue = decimal(value.quantity, "Quantidade comercial", {
        positive: true,
        places: selectedChoice.basePresentation && product.unit !== "UN" ? 1 : 0,
        max: 9999999999999.9,
      });
      const body = {};
      if (!item || purchase || quantityValue !== Number(item.quantity))
        body.quantity = quantityValue;
      if (!item)
        Object.assign(body, {
          [purchase ? "purchaseId" : "orderId"]: document.id,
          productId: product.id,
          warehouseId: Number(value.warehouseId),
          [purchase ? "supplierOptionId" : "presentationId"]: selectedChoice.id,
        });
      if (item && !purchase) {
        if (product.id !== item.product.id) body.productId = product.id;
        if (Number(value.warehouseId) !== item.warehouse.id)
          body.warehouseId = Number(value.warehouseId);
        if (selectedChoice.id !== item.presentation?.id) body.presentationId = selectedChoice.id;
      }
      if ((purchase && value.price.trim()) || overridesSalePrice)
        body[purchase ? "unitCost" : "unitPrice"] = decimal(
          value.price,
          purchase ? "Custo comercial" : "Preço comercial",
        );
      if (
        !purchase &&
        has("orders.discount") &&
        (!item ||
          Number(value.discount.replace(",", ".")) !== Number(item.discount) ||
          value.discountType !== item.discountType)
      ) {
        body.discount = decimal(value.discount, "Desconto");
        body.discountType = value.discountType;
      }
      if (item && !purchase && !Object.keys(body).length) return undefined;
      if (!purchase && has("products.read")) {
        let currentPresentation;
        try {
          currentPresentation = await products.findPresentation(selectedChoice.id);
        } catch (issue) {
          if (issue.status === 404) {
            choices.reload();
            form.change("choiceId", "");
            setChoice(null);
            throw new Error(
              "A apresentação não está mais disponível. Selecione novamente uma opção atualizada.",
            );
          }
          throw issue;
        }
        if (
          String(currentPresentation.productId) !== value.productId ||
          !currentPresentation.active
        ) {
          choices.reload();
          form.change("choiceId", "");
          setChoice(null);
          throw new Error(
            String(currentPresentation.productId) !== value.productId
              ? "A apresentação pertence a outro SKU. As opções foram recarregadas; selecione novamente."
              : "Esta apresentação foi inativada. As opções foram recarregadas; escolha uma apresentação ativa.",
          );
        }
        if (
          Number(currentPresentation.baseFactor) !== Number(selectedChoice.baseFactor) ||
          currentPresentation.basePresentation !== selectedChoice.basePresentation
        ) {
          choices.reload();
          form.change("choiceId", "");
          setChoice(null);
          throw new Error(
            "A conversão da apresentação foi alterada. Selecione novamente e confira a quantidade antes de salvar.",
          );
        }
      }
      try {
        return item ? await service.updateItem(item.id, body) : await service.addItem(body);
      } catch (issue) {
        if (!purchase && /Presentation|presentation/.test(issue.message)) {
          choices.reload();
          form.change("choiceId", "");
          setChoice(null);
        }
        throw issue;
      }
    });
  const selectedStock = stock.data?.find(
    (row) => String(row.warehouse.id) === form.value.warehouseId,
  );
  return (
    <Modal
      title={`${item ? "Editar" : "Adicionar"} item da ${purchase ? "compra" : "venda"}`}
      onClose={form.close}
      busy={form.saving}
      wide
      className={purchase ? "purchase-dialog" : "order-dialog"}
    >
      <form
        className={"customer-form " + (purchase ? "purchase-item-form" : "order-item-form")}
        onSubmit={(event) => {
          if (showSalePicker) {
            event.preventDefault();
            return;
          }
          submit(event);
        }}
      >
        <ErrorAlert message={purchase ? form.error : salesError(form.error)} />
        <fieldset disabled={form.saving || !has(permission)} className="customer-form-fields">
          {showSalePicker ? (
            <>
              {form.value.productId && (
                <button type="button" className="text-button" onClick={() => setPicker(false)}>
                  Manter produto selecionado
                </button>
              )}
              <SalesProductPicker orderId={document.id} onSelect={selectProduct} />
            </>
          ) : purchase ? (
            <label>
              Produto / SKU
              <div className={purchase ? "purchase-supplier-control" : "order-customer-control"}>
                <input
                  readOnly
                  value={product ? skuName(product) : item ? skuName(item.product) : ""}
                  placeholder="Selecione um produto / SKU"
                />
                {(!item || canChangeSaleIdentity) && (
                  <button
                    type="button"
                    className="secondary"
                    disabled={!has("products.read")}
                    onClick={() => setPicker(true)}
                  >
                    {form.value.productId ? "Trocar produto" : "Selecionar produto"}
                  </button>
                )}
              </div>
            </label>
          ) : (
            <div className="order-selected-product">
              <div>
                <small>Produto selecionado</small>
                <strong>{product?.name || item?.product.name || "Carregando produto…"}</strong>
                <small>{product ? skuName(product) : item ? skuName(item.product) : ""}</small>
              </div>
              {(!item || canChangeSaleIdentity) && (
                <button type="button" className="secondary" onClick={() => setPicker(true)}>
                  Trocar produto
                </button>
              )}
            </div>
          )}
          {form.value.productId && <ProductLoad state={productState}>{null}</ProductLoad>}
          {(purchase || (product && !showSalePicker)) && (
            <>
              {item && (purchase || !canChangeSaleIdentity) ? (
                <p>
                  {item.presentationNameSnapshot || "Conversão legada pendente"} · {item.warehouse.name}{" "}
                  · fator {quantity(item.baseFactorSnapshot)} {item.unitSnapshot}
                  <br />
                  {purchase
                    ? "Para trocar produto, apresentação ou depósito, remova o item e adicione outro enquanto o documento estiver aberto."
                    : "Para trocar produto, apresentação ou depósito, é necessária permissão de consulta de produtos e depósitos. Os demais campos deste item podem ser editados."}
                </p>
              ) : (
                <>
                  <ProductLoad
                    state={choices}
                    empty={
                      purchase
                        ? "Nenhuma opção disponível. Configure o vínculo e a embalagem em Produtos → Fornecedores."
                        : "Nenhuma apresentação disponível."
                    }
                  >
                    <label>
                      {purchase ? "Opção de compra" : "Apresentação de venda"}
                      <FilterSelect
                        required
                        value={form.value.choiceId}
                        onChange={(event) => {
                          const next =
                            choices.data.content.find((row) => String(row.id) === event.target.value) ||
                            (String(selectedChoice?.id) === event.target.value ? selectedChoice : null);
                          setChoice(next);
                          form.change("choiceId", event.target.value);
                          if (!purchase && event.target.value !== form.value.choiceId) {
                            form.change("price", "");
                            form.change("priceMode", "automatic");
                          }
                        }}
                      >
                        <option value="">Selecionar</option>
                        {!purchase &&
                          item &&
                          selectedChoice &&
                          !choices.data?.content.some((row) => row.id === selectedChoice.id) && (
                            <option value={selectedChoice.id}>
                              {item.presentationNameSnapshot || selectedChoice.name} · Apresentação
                              atual
                            </option>
                          )}
                        {choices.data?.content.map((row) => (
                          <option
                            key={row.id}
                            value={row.id}
                            disabled={
                              purchase
                                ? !row.conversionConfirmed
                                : String(row.productId) !== form.value.productId || !row.active
                            }
                          >
                            {row.name} · {quantity(row.baseFactor)} {product?.unit}{" "}
                            {purchase ? `· ${money(row.cost)}` : ""}
                            {purchase && !row.conversionConfirmed ? " · Conversão pendente" : ""}
                          </option>
                        ))}
                      </FilterSelect>
                    </label>
                  </ProductLoad>
                  <Pagination
                    page={page}
                    totalPages={choices.data?.totalPages}
                    change={(next) => {
                      setChoice(null);
                      form.change("choiceId", "");
                      setPage(next);
                    }}
                  />
                  <WarehouseSelect
                    value={form.value.warehouseId}
                    disabled={!purchase && has("stock.read") && (stock.loading || Boolean(stock.error))}
                    allowedIds={
                      !purchase && has("stock.read")
                        ? stock.data?.map((row) => row.warehouse.id) || []
                        : undefined
                    }
                    onChange={(e) => form.change("warehouseId", e.target.value)}
                  />
                </>
              )}
              {!purchase && (
                <ErrorAlert
                  message={resolveSalePrice ? salesError(priceRequest.error) : ""}
                  retry={priceRequest.reload}
                />
              )}
              {purchase && !item && (
                <p>
                  Custo em branco usa o custo da opção de compra. Este custo não altera o preço de
                  venda.
                </p>
              )}
              <div className="form-grid">
                <label>
                  {purchase ? "Quantidade comercial · " : "Quantidade · "}
                  {selectedChoice?.name || item?.presentationNameSnapshot || "selecione a apresentação"}
                  <input
                    ref={quantityInput}
                    required
                    inputMode="decimal"
                    value={form.value.quantity}
                    onChange={(e) => form.change("quantity", e.target.value)}
                  />
                  {!purchase && selectedChoice && (
                    <small className="field-hint">
                      Cada {selectedChoice.name}: {quantity(selectedChoice.baseFactor)} {product.unit}
                    </small>
                  )}
                </label>
                {
                  <label>
                    {purchase ? "Custo por apresentação" : "Preço por apresentação"}{" "}
                    {purchase && item
                      ? `(atual ${money(purchase ? item.unitCost : item.unitPrice)})`
                      : purchase
                        ? "(opcional)"
                        : ""}
                    <input
                      inputMode="decimal"
                      readOnly={!purchase}
                      placeholder={
                        purchase
                          ? "Automático"
                          : priceLoading
                            ? "Consultando preço…"
                            : "Selecione a apresentação"
                      }
                      value={displayedPrice}
                      onChange={(e) =>
                        purchase
                          ? form.change("price", e.target.value)
                          : form.setValue((old) => ({
                              ...old,
                              price: e.target.value,
                              priceMode: "manual",
                            }))
                      }
                    />
                    {!purchase && (
                      <small className="field-hint">
                        {manualPrice ? "Preço manual" : sameSaleIdentity ? "Preço salvo na venda" : "Preço automático do cliente"}
                      </small>
                    )}
                    {!purchase && manualPrice && (
                      <button
                        type="button"
                        className="text-button"
                        onClick={() =>
                          form.setValue((old) => ({ ...old, price: "", priceMode: "automatic" }))
                        }
                      >
                        {resolveSalePrice ? "Usar preço automático" : "Restaurar preço do item"}
                      </button>
                    )}
                  </label>
                }
              </div>
              {!purchase && (
                <div className="order-item-total" aria-live="polite">
                  <strong>Total do item</strong>
                  {preview ? (
                    <dl className="customer-details">
                      <div><dt>Total bruto</dt><dd>{money(preview.gross)}</dd></div>
                      <div><dt>Desconto</dt><dd>{money(preview.discountValue)}</dd></div>
                      <div><dt>Total líquido</dt><dd>{money(preview.net)}</dd></div>
                    </dl>
                  ) : (
                    <p className="field-hint">Informe valores válidos de quantidade, preço e desconto para visualizar o total.</p>
                  )}
                  <p className="field-hint">Valor conferido ao salvar.</p>
                </div>
              )}
              {!purchase && (
                <details
                  className="order-item-options"
                  open={advanced}
                  onToggle={(event) => setAdvanced(event.currentTarget.open)}
                >
                  <summary>Preço e desconto</summary>
                  <div className="order-item-options-content">
                    <p className="field-hint">
                      {manualPrice
                        ? "Preço informado explicitamente para esta apresentação."
                        : !item || form.value.productId !== String(item.product.id) ||
                            form.value.choiceId !== String(item.presentation?.id || "")
                          ? "Preço automático conforme a tabela do cliente e a apresentação selecionada."
                          : "Preço salvo no pedido (não é uma consulta à tabela vigente). Alterar só a quantidade mantém este valor."}
                    </p>
                    {has("orders.manualPrice") && (
                      <label>
                        Informar outro preço
                        <input
                          inputMode="decimal"
                          value={form.value.price}
                          placeholder={displayedPrice || "Preço por apresentação"}
                          onChange={(event) => form.setValue((old) => ({
                            ...old,
                            price: event.target.value,
                            priceMode: event.target.value.trim() ? "manual" : "automatic",
                          }))}
                        />
                      </label>
                    )}
                    {item && !purchase &&
                      form.value.productId === String(item.product.id) &&
                      form.value.choiceId === String(item.presentation?.id || "") && (
                        <div className="span-all">
                          <button
                            type="button"
                            className="text-button"
                            disabled={priceLoading}
                            onClick={() => {
                              setCheckingCurrentPrice(true);
                              if (checkingCurrentPrice) priceRequest.reload();
                            }}
                          >
                            {priceLoading ? "Consultando preço…" : "Consultar preço vigente"}
                          </button>
                          {currentPriceReady && (
                            <p className="field-hint">
                              Preço automático vigente: {money(currentQuotedPrice)} por apresentação.
                              {has("orders.manualPrice")
                                ? " Para atualizar o item, aplique o valor explicitamente."
                                : " Somente usuários com permissão de preço manual podem aplicar esse valor ao item existente."}
                            </p>
                          )}
                          {currentPriceReady && has("orders.manualPrice") && (
                            <button
                              type="button"
                              className="text-button"
                              onClick={() => form.setValue((old) => ({
                                ...old,
                                price: textNumber(currentQuotedPrice),
                                priceMode: "manual",
                              }))}
                            >
                              Aplicar preço vigente
                            </button>
                          )}
                        </div>
                      )}
                    {has("orders.discount") && (
                      <div className="form-grid">
                        <label>
                          Desconto
                          <input inputMode="decimal" required value={form.value.discount}
                            onInvalid={() => setAdvanced(true)}
                            onChange={(event) => form.change("discount", event.target.value)} />
                        </label>
                        <label>
                          Tipo de desconto
                          <FilterSelect value={form.value.discountType}
                            onChange={(event) => form.change("discountType", event.target.value)}>
                            <option value="AMOUNT">Valor do item (R$)</option>
                            <option value="PERCENTAGE">Percentual (%)</option>
                          </FilterSelect>
                        </label>
                      </div>
                    )}
                  </div>
                </details>
              )}
              {purchase && <p>Informe a quantidade na apresentação selecionada. O estoque é controlado em BASE.</p>}
              {!purchase && selectedChoice?.active === false && (
                <p className="product-warning">
                  A apresentação atual está inativa. Escolha uma apresentação ativa antes de confirmar o
                  pedido.
                </p>
              )}
              {duplicateItem && (
                <p className="product-warning">
                  Esta combinação de produto, apresentação e depósito já está no pedido. Edite o item #
                  {duplicateItem.id} para alterar a quantidade.
                </p>
              )}
              {has("stock.read") && (
                <>
                  <ProductLoad state={stock} empty="Produto sem estoque configurado.">
                    {selectedStock ? (
                      purchase ? (
                        <p>
                          Saldo no depósito: físico {quantity(selectedStock.quantity)} · reservado{" "}
                          {quantity(selectedStock.reservedQuantity)} · disponível{" "}
                          {available(selectedStock)} {product?.unit}
                        </p>
                      ) : (
                        <div className="order-stock-summary">
                          <span>Disponível: <strong>{available(selectedStock)} {product.unit}</strong></span>
                          <details>
                            <summary>Detalhes do saldo</summary>
                            <p>Físico: {quantity(selectedStock.quantity)} · Reservado: {quantity(selectedStock.reservedQuantity)}</p>
                          </details>
                        </div>
                      )
                    ) : (
                      <p>Escolha um depósito com estoque configurado.</p>
                    )}
                  </ProductLoad>
                </>
              )}
            </>
          )}
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" onClick={form.close} disabled={form.saving}>
            {purchase ? "Cancelar" : "Voltar ao pedido"}
          </button>
          <button
            disabled={
              form.saving ||
              showSalePicker ||
              !has(permission) ||
              !product ||
              !selectedChoice ||
              (!purchase &&
                !overridesSalePrice &&
                resolveSalePrice &&
                (priceLoading || Boolean(priceRequest.error) || automaticAmount == null)) ||
              (!purchase &&
                (Boolean(duplicateItem) ||
                  (has("stock.read") &&
                    (stock.loading || Boolean(stock.error) || !selectedStock)))) ||
              (!purchase && !item && !form.value.warehouseId) ||
              (!purchase && item && !form.dirty)
            }
          >
            {form.saving
              ? "Salvando…"
              : purchase
                ? "Salvar item"
                : item
                  ? "Salvar alterações"
                  : "Adicionar à venda"}
          </button>
        </div>
      </form>
      {purchase && picker && (
        <SkuPicker
          orderId={purchase ? undefined : document.id}
          onClose={() => setPicker(false)}
          onSelect={selectProduct}
        />
      )}
    </Modal>
  );
}

import { useCallback, useEffect, useMemo, useState } from "react";
import { Plus, Search, X, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { Modal } from "../../components/Modal";
import { Dropdown } from "../../components/Dropdown";
import { api } from "../../services/api";
import { ProductForm } from "./ProductForm";
import { ProductStockInfo } from "./ProductStockInfo";
import { ClassificationModal } from "./ClassificationModal";
import { filterProducts } from "./productFilters";
import { buildStockPayload, createProductWithStock } from "./productStock";
import "./products.css";

// Estado inicial do formulário. Mantê-lo fora do componente evita recriá-lo a cada renderização.
const EMPTY_PRODUCT = {
  name: "",
  sku: "",
  brandId: "",
  categoryId: "",
  cost: "",
  price: "",
  weightKg: "",
  color: "",
  dimensionX: "",
  dimensionY: "",
  dimensionZ: "",
  size: "",
  warehouseId: "",
  quantity: "0",
  reservedQuantity: "0",
  minQuantity: "0",
  maxQuantity: "0",
};
// Campos que a API recebe como números. Inputs HTML sempre retornam texto.
const numberFields = [
  "brandId",
  "categoryId",
  "cost",
  "price",
  "weightKg",
  "dimensionX",
  "dimensionY",
  "dimensionZ",
];

export function ProductsPage() {
  // Coleções carregadas do backend. Categorias e marcas também alimentam os dropdowns e formulário.
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [stockItems, setStockItems] = useState([]);
  const [stockError, setStockError] = useState("");
  const [warehouses, setWarehouses] = useState([]);
  const [warehouseError, setWarehouseError] = useState("");
  const [formError, setFormError] = useState("");
  const [createdProductId, setCreatedProductId] = useState(null);
  // Todos os filtros ficam agrupados para que um único método possa atualizá-los.
  const [filters, setFilters] = useState({
    search: "",
    categoryId: "",
    brandId: "",
    active: "",
    searchBy: "name",
  });
  // Estados de interface: carregamento, mensagens, menus e modais.
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [openedMenuId, setOpenedMenuId] = useState(null);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [classificationModal, setClassificationModal] = useState(null);
  const [productForm, setProductForm] = useState(EMPTY_PRODUCT);
  const [saving, setSaving] = useState(false);

  // Carrega os dados usados pela tabela e pelos campos de seleção.
  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");
    setStockError("");
    try {
      const [productData, categoryData, brandData, stockData] =
        await Promise.all([
          api("/product/search"),
          api("/category"),
          api("/brand"),
          api("/stockItem/search").catch((requestError) => {
            setStockError(
              `Não foi possível carregar o estoque. ${requestError.message}`,
            );
            return [];
          }),
        ]);
      setProducts(productData);
      setCategories(categoryData);
      setBrands(brandData);
      setStockItems(stockData);
      setError("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- Carregamento inicial da API; os dados chegam de forma assíncrona.
    loadData();
  }, [loadData]);

  const loadWarehouses = useCallback(async () => {
    setWarehouseError("");
    try {
      setWarehouses(await api("/warehouse/search"));
      setWarehouseError("");
    } catch (requestError) {
      setWarehouseError(requestError.message);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- Sincroniza as opções do formulário com a API de armazéns.
    loadWarehouses();
  }, [loadWarehouses]);

  // Filtragem local: mantém a resposta da interface imediata.
  const filteredProducts = useMemo(
    () => filterProducts(products, filters),
    [products, filters],
  );

  const stockByProduct = useMemo(() => {
    const grouped = new Map();
    for (const item of stockItems) {
      const productId = String(item.product?.id);
      if (!grouped.has(productId)) grouped.set(productId, []);
      grouped.get(productId).push(item);
    }
    return grouped;
  }, [stockItems]);

  function updateFilter(name, value) {
    // Preserva os outros filtros e altera somente o campo solicitado.
    setFilters((current) => ({ ...current, [name]: value }));
  }

  function openProductModal(product = null) {
    setCreatedProductId(null);
    setFormError("");
    if (!product) loadWarehouses();
    // "new" identifica cadastro. Um objeto identifica edição do respectivo produto.
    setOpenedMenuId(null);
    setSelectedProduct(product || "new");
    setProductForm(
      product
        ? {
            name: product.name || "",
            sku: product.sku || "",
            brandId: String(product.brand?.id || ""),
            categoryId: String(product.category?.id || ""),
            cost: product.cost ?? "",
            price: product.price ?? "",
            weightKg: product.weightKg ?? "",
            color: product.color || "",
            dimensionX: product.dimensionX ?? "",
            dimensionY: product.dimensionY ?? "",
            dimensionZ: product.dimensionZ ?? "",
            size: product.size || "",
          }
        : EMPTY_PRODUCT,
    );
  }

  async function saveProduct(event) {
    event.preventDefault();
    if (saving) return;
    let stock;
    try {
      stock = selectedProduct === "new" ? buildStockPayload(productForm) : null;
    } catch (validationError) {
      setFormError(validationError.message);
      return;
    }
    setSaving(true);
    setFormError("");
    // Não enviamos o estado diretamente: primeiro convertemos os campos numéricos.
    const payload = { ...productForm };
    [
      "warehouseId",
      "quantity",
      "reservedQuantity",
      "minQuantity",
      "maxQuantity",
    ].forEach((field) => delete payload[field]);
    numberFields.forEach((field) => {
      payload[field] = payload[field] === "" ? null : Number(payload[field]);
    });
    if (selectedProduct !== "new") delete payload.sku;

    try {
      if (selectedProduct === "new") {
        await createProductWithStock({
          request: api,
          payload,
          stock,
          productId: createdProductId,
          onCreated: setCreatedProductId,
        });
      } else {
        await api(`/product/${selectedProduct.id}`, {
          method: "PATCH",
          body: JSON.stringify(payload),
        });
      }
      setSelectedProduct(null);
      await loadData();
    } catch (requestError) {
      setFormError(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  function closeProductModal() {
    if (saving) return;
    setSelectedProduct(null);
    if (createdProductId) loadData();
  }

  function removeClassification(type, id) {
    const updateItems = type === "brand" ? setBrands : setCategories;
    const field = type === "brand" ? "brandId" : "categoryId";
    updateItems((current) => current.filter((item) => item.id !== id));
    setFilters((current) =>
      String(current[field]) === String(id)
        ? { ...current, [field]: "" }
        : current,
    );
    setProductForm((current) =>
      String(current[field]) === String(id)
        ? { ...current, [field]: "" }
        : current,
    );
  }

  async function deleteProduct(product) {
    setOpenedMenuId(null);
    if (!window.confirm(`Excluir “${product.name}”?`)) return;
    try {
      await api(`/product/${product.id}`, { method: "DELETE" });
      await loadData();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  // Formatação é isolada para que toda moeda siga o padrão brasileiro.
  const formatMoney = (value) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value || 0);

  return (
    <>
      <header className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Produtos
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {products.length}{" "}
            {products.length === 1
              ? "produto cadastrado"
              : "produtos cadastrados"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50"
            onClick={() => setClassificationModal("brand")}
          >
            <Plus size={17} strokeWidth={1.8} aria-hidden="true" />
            Marca
          </button>
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm hover:bg-slate-50"
            onClick={() => setClassificationModal("category")}
          >
            <Plus size={17} strokeWidth={1.8} aria-hidden="true" />
            Categoria
          </button>
          <button
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white shadow-sm hover:bg-blue-700"
            onClick={() => openProductModal()}
          >
            <Plus size={17} strokeWidth={1.8} aria-hidden="true" />
            Novo Produto
          </button>
        </div>
      </header>

      <section className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-[145px_minmax(220px,1fr)_160px_160px_160px]">
        <Dropdown
          value={filters.searchBy}
          onChange={(value) => updateFilter("searchBy", value)}
          ariaLabel="Campo da pesquisa"
          options={[
            { value: "name", label: "Nome" },
            { value: "sku", label: "SKU" },
            { value: "price", label: "Valor" },
          ]}
        />
        <label className="flex h-10 items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 text-slate-400 shadow-sm sm:col-span-2 xl:col-span-1">
          <Search size={18} strokeWidth={1.8} aria-hidden="true" />
          <input
            className="w-full bg-transparent text-sm text-slate-800 outline-none placeholder:text-slate-400"
            value={filters.search}
            onChange={(event) => updateFilter("search", event.target.value)}
            placeholder={
              filters.searchBy === "price"
                ? "Buscar por valor (ex.: 49,90)"
                : filters.searchBy === "sku"
                  ? "Buscar por SKU..."
                  : "Buscar por nome..."
            }
            aria-label={
              filters.searchBy === "price"
                ? "Buscar por valor"
                : filters.searchBy === "sku"
                  ? "Buscar por SKU"
                  : "Buscar por nome"
            }
            inputMode={filters.searchBy === "price" ? "decimal" : "text"}
          />
        </label>
        <Dropdown
          value={filters.categoryId}
          onChange={(value) => updateFilter("categoryId", value)}
          ariaLabel="Filtrar por categoria"
          options={[
            { value: "", label: "Categorias" },
            ...categories.map((item) => ({ value: item.id, label: item.name })),
          ]}
        />
        <Dropdown
          value={filters.brandId}
          onChange={(value) => updateFilter("brandId", value)}
          ariaLabel="Filtrar por marca"
          options={[
            { value: "", label: "Marcas" },
            ...brands.map((item) => ({ value: item.id, label: item.name })),
          ]}
        />
        <Dropdown
          value={filters.active}
          onChange={(value) => updateFilter("active", value)}
          ariaLabel="Filtrar por status"
          options={[
            { value: "", label: "Todos status" },
            { value: "true", label: "Ativos" },
            { value: "false", label: "Inativos" },
          ]}
        />
      </section>

      {error && (
        <div
          className="mb-4 flex items-center justify-between rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700"
          role="alert"
        >
          {error}
          <button onClick={() => setError("")}>
            <X size={16} strokeWidth={1.8} aria-hidden="true" />
          </button>
        </div>
      )}
      {stockError && (
        <div
          className="mb-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800"
          role="alert"
        >
          {stockError}
          <button className="font-semibold underline" onClick={loadData}>
            Tentar novamente
          </button>
        </div>
      )}
      <section className="overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="min-w-[1150px]">
          <div className="grid min-h-11 grid-cols-[130px_minmax(180px,1.4fr)_minmax(120px,1fr)_minmax(130px,1fr)_90px_120px_280px_62px] items-center border-b border-slate-200 px-4 text-xs font-medium text-slate-500">
            <span>SKU</span>
            <span>Nome</span>
            <span>Marca</span>
            <span>Categoria</span>
            <span>Status</span>
            <span>Valor</span>
            <span>Estoque</span>
            <span>Ações</span>
          </div>
          {loading ? (
            <EmptyState text="Carregando produtos..." />
          ) : filteredProducts.length === 0 ? (
            <EmptyState text="Nenhum produto encontrado." />
          ) : (
            filteredProducts.map((product) => (
              <div
                className="grid h-16 grid-cols-[130px_minmax(180px,1.4fr)_minmax(120px,1fr)_minmax(130px,1fr)_90px_120px_280px_62px] items-center border-b border-slate-200 px-4 text-sm text-slate-800 last:border-0"
                key={product.id}
              >
                <span
                  className="truncate pr-3 font-mono text-[11px] text-slate-600"
                  title={product.sku}
                >
                  {product.sku}
                </span>
                <strong
                  className="line-clamp-2 min-w-0 break-words pr-3 font-semibold"
                  title={product.name}
                >
                  {product.name}
                </strong>
                <span
                  className="line-clamp-2 min-w-0 break-words pr-3"
                  title={product.brand?.name}
                >
                  {product.brand?.name || "—"}
                </span>
                <span
                  className="line-clamp-2 min-w-0 break-words pr-3"
                  title={product.category?.name}
                >
                  {product.category?.name || "Sem categoria"}
                </span>
                <span>
                  <span
                    className={`rounded-full px-2 py-1 text-[11px] font-semibold ${product.active ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"}`}
                  >
                    {product.active ? "Ativo" : "Inativo"}
                  </span>
                </span>
                <span>{formatMoney(product.price)}</span>
                <ProductStockInfo
                  items={stockByProduct.get(String(product.id)) || []}
                  unavailable={Boolean(stockError)}
                />
                <span className="relative justify-center">
                  <button
                    className="grid h-8 w-8 place-items-center rounded-md text-slate-500 hover:bg-slate-100"
                    onClick={() =>
                      setOpenedMenuId(
                        openedMenuId === product.id ? null : product.id,
                      )
                    }
                    aria-label={`Ações de ${product.name}`}
                  >
                    <MoreHorizontal
                      size={18}
                      strokeWidth={1.8}
                      aria-hidden="true"
                    />
                  </button>
                  {openedMenuId === product.id && (
                    <span className="absolute right-0 top-9 z-10 w-32 rounded-lg border border-slate-200 bg-white p-1 shadow-xl">
                      <button
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-xs text-slate-700 hover:bg-slate-50"
                        onClick={() => openProductModal(product)}
                      >
                        <Pencil
                          size={15}
                          strokeWidth={1.8}
                          aria-hidden="true"
                        />
                        Editar
                      </button>
                      <button
                        className="flex w-full items-center gap-2 rounded-md px-2 py-2 text-left text-xs text-red-600 hover:bg-red-50"
                        onClick={() => deleteProduct(product)}
                      >
                        <Trash2
                          size={15}
                          strokeWidth={1.8}
                          aria-hidden="true"
                        />
                        Excluir
                      </button>
                    </span>
                  )}
                </span>
              </div>
            ))
          )}
        </div>
      </section>

      {selectedProduct && (
        <Modal
          title={selectedProduct === "new" ? "Novo produto" : "Editar produto"}
          onClose={closeProductModal}
        >
          <ProductForm
            product={selectedProduct}
            brands={brands}
            categories={categories}
            warehouses={warehouses}
            warehouseError={warehouseError}
            onRetryWarehouses={loadWarehouses}
            error={formError}
            productCreated={Boolean(createdProductId)}
            form={productForm}
            onChange={(event) =>
              setProductForm((current) => ({
                ...current,
                [event.target.name]: event.target.value,
              }))
            }
            onCancel={closeProductModal}
            onSubmit={saveProduct}
            saving={saving}
          />
        </Modal>
      )}
      {classificationModal && (
        <ClassificationModal
          key={classificationModal}
          type={classificationModal}
          items={classificationModal === "brand" ? brands : categories}
          loading={loading}
          onCreated={(item) => {
            const updateItems =
              classificationModal === "brand" ? setBrands : setCategories;
            updateItems((current) => [...current, item]);
          }}
          onDeleted={(id) => removeClassification(classificationModal, id)}
          onClose={() => setClassificationModal(null)}
        />
      )}
    </>
  );
}

function EmptyState({ text }) {
  return <div className="p-10 text-center text-sm text-slate-500">{text}</div>;
}

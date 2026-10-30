import useAuth from "../../auth/useAuth";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { products } from "../../services/products";
import { useProductResource } from "./useProductResource";
import { characteristics } from "./productFormData";
import ProductNumberInput from "./ProductNumberInput";

export default function ProductItemFields({ value, onChange, catalog, item, disabled }) {
  const { has } = useAuth();
  const canStock = !item && has("stock.configure") && has("warehouses.read");
  const warehouses = useProductResource(
    (options) => products.warehouses(options),
    [],
    canStock && value.withStock,
  );
  const change = (field, next) => onChange({ ...value, [field]: next });
  return (
    <div className="form-grid">
      {item && (
        <label>
          SKU
          <input value={item.sku} readOnly />
        </label>
      )}
      {catalog.type === "VARIANT" &&
        (catalog.characteristics || []).map((field) => (
          <label key={field}>
            {characteristics[field]}
            <input
              required
              value={value.attributes[field] || ""}
              onChange={(e) =>
                change("attributes", { ...value.attributes, [field]: e.target.value })
              }
            />
          </label>
        ))}
      {[
        ["cost", "Custo de referência por unidade base", true],
        ["price", "Preço por unidade base", true],
        ["weightKg", "Peso (kg)"],
        ["dimensionX", "Dimensão X"],
        ["dimensionY", "Dimensão Y"],
        ["dimensionZ", "Dimensão Z"],
      ].map(([field, label, required]) => (
        <label key={field}>
          {label}
          <ProductNumberInput
            label={label}
            places={field === "weightKg" ? 3 : 2}
            max={
              field === "weightKg"
                ? 9999999.999
                : field.startsWith("dimension")
                  ? 99999999.99
                  : 9999999999.99
            }
            required={required}
            inputMode="decimal"
            value={value[field]}
            onChange={(e) => change(field, e.target.value)}
          />
        </label>
      ))}
      {catalog.type === "SIMPLE" &&
        [
          ["color", "Cor (opcional)"],
          ["size", "Tamanho (opcional)"],
        ].map(([field, label]) => (
          <label key={field}>
            {label}
            <input value={value[field]} onChange={(e) => change(field, e.target.value)} />
          </label>
        ))}
      {!item && (canStock || value.withStock) && (
        <label className="check span-all">
          <input
            type="checkbox"
            checked={value.withStock}
            onChange={(e) => change("withStock", e.target.checked)}
          />
          Incluir estoque inicial (BASE {catalog.unit || "a confirmar"})
        </label>
      )}
      {!item && !canStock && value.withStock && (
        <p className="product-warning span-all">
          O rascunho inclui estoque inicial sem permissão atual. Desmarque a inclusão para cadastrar
          somente o produto.
        </p>
      )}
      {!item && has("stock.configure") && !has("warehouses.read") && (
        <p className="muted span-all">
          Estoque inicial requer também permissão para consultar depósitos.
        </p>
      )}
      {canStock && value.withStock && (
        <>
          <p className="muted span-all">
            Quantidades na unidade base. O reservado inicial deve ser zero; as reservas são geradas
            pelos pedidos.
          </p>
          <div className="span-all">
            <ErrorAlert message={warehouses.error} retry={warehouses.reload} />
          </div>
          <label>
            Depósito
            <FilterSelect
              required
              disabled={disabled || warehouses.loading || !warehouses.data}
              value={value.stock.warehouseId}
              onChange={(e) => change("stock", { ...value.stock, warehouseId: e.target.value })}
            >
              <option value="">Selecionar depósito</option>
              {(warehouses.data || []).map((warehouse) => (
                <option key={warehouse.id} value={warehouse.id}>
                  {warehouse.name}
                </option>
              ))}
            </FilterSelect>
          </label>
          {[
            ["quantity", "Saldo inicial"],
            ["reservedQuantity", "Reservado inicial"],
            ["minQuantity", "Mínimo"],
            ["maxQuantity", "Máximo (opcional)"],
          ].map(([field, label]) => (
            <label key={field}>
              {label}
              <ProductNumberInput
                label={label}
                places={catalog.unit === "UN" ? 0 : 1}
                max={9999999999999.9}
                inputMode="decimal"
                required={field !== "maxQuantity"}
                value={value.stock[field]}
                onChange={(e) => change("stock", { ...value.stock, [field]: e.target.value })}
              />
            </label>
          ))}
        </>
      )}
    </div>
  );
}

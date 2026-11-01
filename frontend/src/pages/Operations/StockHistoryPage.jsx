import { useCallback, useState } from "react";
import { useLocation, useSearchParams } from "react-router-dom";
import PageHeading from "../../components/layout/PageHeading";
import FilterSelect from "../../components/feedback/FilterSelect";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useListReturn from "../../hooks/useListReturn";
import { historyIds, historyDates, parseHistoryQuery } from "../../routes/stockHistoryQuery";
import StockHistoryContent, { types, reasons } from "./StockHistoryContent";

export default function StockHistoryPage() {
  const location = useLocation();
  return <History key={location.search} />;
}
function History() {
  const [params, setParams] = useSearchParams();
  const location = useLocation();
  const { filter, error } = parseHistoryQuery(params);
  const [values, setValues] = useState(() => Object.fromEntries(params));
  const [failure, setFailure] = useState("");
  const [count, setCount] = useState({ loading: !error });
  const reportCount = useCallback((value) => setCount(value), []);
  const { returnToList } = useListReturn({ modulePath: "/products" });
  const labels = {
    productId: "SKU (ID)",
    warehouseId: "Depósito (ID)",
    stockItemId: "Saldo (ID)",
    orderId: "Pedido (ID)",
    purchaseId: "Compra (ID)",
    userId: "Operador (ID)",
    occurredFrom: "A partir de",
    occurredTo: "Até",
  };
  const change = (key, value) => setValues((previous) => ({ ...previous, [key]: value }));
  return (
    <div className="operation-page stock-page">
      <header className="page-title">
        <PageHeading
          title="Histórico de estoque"
          total={count.total}
          loading={count.loading}
          singular="movimento encontrado"
          plural="movimentos encontrados"
        />
        <button className="secondary" onClick={returnToList}>
          Voltar aos produtos
        </button>
      </header>
      <form
        className="panel stock-history-filters"
        onSubmit={(e) => {
          e.preventDefault();
          const next = new URLSearchParams(Object.entries(values).filter(([, value]) => value));
          const parsed = parseHistoryQuery(next);
          if (parsed.error) setFailure(parsed.error);
          else {
            setFailure("");
            setParams(next, { state: location.state });
          }
        }}
      >
        <div className="form-grid">
          {historyIds.map((key) => (
            <label key={key}>
              {labels[key]}
              <input
                inputMode="numeric"
                value={values[key] || ""}
                onChange={(e) => change(key, e.target.value)}
              />
            </label>
          ))}
          <label>
            Tipo
            <FilterSelect
              value={values.type || ""}
              onChange={(e) => change("type", e.target.value)}
            >
              <option value="">Todos</option>
              {Object.entries(types).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </FilterSelect>
          </label>
          <label>
            Motivo
            <FilterSelect
              value={values.reason || ""}
              onChange={(e) => change("reason", e.target.value)}
            >
              <option value="">Todos</option>
              {Object.entries(reasons).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </FilterSelect>
          </label>
          {historyDates.map((key) => (
            <label key={key}>
              {labels[key]}
              <input
                type="datetime-local"
                value={values[key] || ""}
                onChange={(e) => change(key, e.target.value)}
              />
            </label>
          ))}
        </div>
        <div className="actions">
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setValues({});
              setFailure("");
              setParams({}, { state: location.state });
            }}
          >
            Limpar filtros
          </button>
          <button>Aplicar filtros</button>
        </div>
      </form>
      <ErrorAlert message={failure || error} />
      <p className="field-hint">
        Identificador de SKU é diferente do catálogo e do saldo. A consulta retorna todos os
        movimentos encontrados pelos filtros.
      </p>
      {!error && <StockHistoryContent filter={filter} onStateChange={reportCount} />}
    </div>
  );
}

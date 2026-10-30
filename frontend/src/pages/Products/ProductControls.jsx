import FilterSelect from "../../components/feedback/FilterSelect";
import Pagination from "../../components/feedback/Pagination";
import ErrorAlert from "../../components/feedback/ErrorAlert";

export function ProductPageFooter({ list }) {
  return (
    <div className="customer-list-footer">
      <span>
        {list.data
          ? `${list.data.totalElements.toLocaleString("pt-BR")} ${list.data.totalElements === 1 ? "registro" : "registros"}`
          : ""}
      </span>
      <label>
        Por página
        <FilterSelect value={list.size} onChange={(e) => list.changeSize(Number(e.target.value))}>
          {[25, 50, 100].map((size) => (
            <option key={size} value={size}>
              {size}
            </option>
          ))}
        </FilterSelect>
      </label>
      <Pagination page={list.page} totalPages={list.data?.totalPages} change={list.changePage} />
    </div>
  );
}
export function ProductLoad({ state, children, empty = "Nenhum registro encontrado." }) {
  return (
    <>
      <ErrorAlert message={state.error} retry={state.reload} />
      {state.loading ? (
        <p role="status">Carregando…</p>
      ) : state.error ? null : state.data == null ||
        (Array.isArray(state.data) && !state.data.length) ||
        state.data?.content?.length === 0 ? (
        <p className="empty">{empty}</p>
      ) : (
        children
      )}
    </>
  );
}
export function ProductFilters({ list, placeholder = "Pesquisar" }) {
  return (
    <div className="product-toolbar">
      <label>
        Busca
        <input
          type="search"
          value={list.filters.query}
          placeholder={placeholder}
          onChange={(e) => list.changeFilters({ ...list.filters, query: e.target.value })}
        />
      </label>
      <label>
        Situação
        <FilterSelect
          value={list.filters.active}
          onChange={(e) => list.changeFilters({ ...list.filters, active: e.target.value })}
        >
          <option value="">Todas</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </FilterSelect>
      </label>
    </div>
  );
}

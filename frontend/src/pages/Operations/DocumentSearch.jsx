import { useEffect, useState } from "react";

export const emptyDocumentSearch = { number: "", query: "", from: "", to: "" };

export function useDocumentSearch(initial, resetPage) {
  const [value, setValue] = useState(() => ({ ...emptyDocumentSearch, ...initial }));
  const [query, setQuery] = useState(value.query.trim());
  useEffect(() => {
    const timer = setTimeout(() => {
      if (query !== value.query.trim()) {
        setQuery(value.query.trim());
        resetPage(0);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [value.query, query, resetPage]);
  return {
    value,
    change: (next) => {
      resetPage(0);
      setValue(next);
    },
    key: JSON.stringify([value.number, query, value.from, value.to]),
    pending: query !== value.query.trim(),
    filter: {
      number: value.number,
      query,
      createdFrom: value.from ? `${value.from}T00:00:00` : "",
      createdTo: value.to ? `${value.to}T23:59:59.999999` : "",
    },
  };
}

export default function DocumentSearch({ search, purchase = false }) {
  const change = (key, value) => search.change({ ...search.value, [key]: value });
  return (
    <>
      <label>
        Número
        <input
          inputMode="numeric"
          value={search.value.number}
          placeholder="Número exato"
          onChange={(e) => {
            if (/^\d{0,15}$/.test(e.target.value)) change("number", e.target.value);
          }}
        />
      </label>
      <label>
        {purchase ? "Fornecedor" : "Cliente"}
        <input
          type="search"
          maxLength={120}
          placeholder="Nome ou nome fantasia"
          value={search.value.query}
          onChange={(e) => change("query", e.target.value)}
        />
      </label>
      <label>
        Registrado de
        <input
          type="date"
          max={search.value.to || undefined}
          value={search.value.from}
          onChange={(e) => change("from", e.target.value)}
        />
      </label>
      <label>
        Até
        <input
          type="date"
          min={search.value.from || undefined}
          value={search.value.to}
          onChange={(e) => change("to", e.target.value)}
        />
      </label>
    </>
  );
}

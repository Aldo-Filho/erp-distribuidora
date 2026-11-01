import { useEffect, useRef, useState } from "react";
import { customers } from "../../services/customers";

export const emptyFilters = { query: "", searchField: "NAME", personType: "", active: "" };

export default function useCustomers(origin) {
  const [filters, setFilters] = useState(() => {
    const restored = { ...emptyFilters };
    for (const key of Object.keys(restored))
      if (typeof origin?.filters?.[key] === "string") restored[key] = origin.filters[key];
    if (!["", "true", "false"].includes(restored.active)) restored.active = "";
    if (!["NAME", "DOCUMENT", "CONTACT"].includes(restored.searchField))
      restored.searchField = "NAME";
    if (!["", "PF", "PJ"].includes(restored.personType)) restored.personType = "";
    return restored;
  });
  const [query, setQuery] = useState(filters.query);
  const [page, setPage] = useState(() =>
    Number.isSafeInteger(origin?.page) && origin.page >= 0 ? origin.page : 0,
  );
  const [size, setSize] = useState(() => ([25, 50, 100].includes(origin?.size) ? origin.size : 25));
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState({ loading: true });
  const generation = useRef(0);
  const currentRequest = useRef(null);
  const invalidate = () => {
    generation.current += 1;
    currentRequest.current?.abort();
    setState({ loading: true });
  };
  useEffect(() => {
    if (filters.query === query) return;
    const timer = setTimeout(() => {
      setQuery(filters.query);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [filters.query]);
  useEffect(() => {
    // Wait for the latest text; never fetch an intermediate filter combination.
    if (filters.query !== query) return;
    const id = ++generation.current;
    const controller = new AbortController();
    currentRequest.current = controller;
    setState({ loading: true });
    customers.search({ ...filters, query }, page, size, { signal: controller.signal }).then(
      (data) => {
        if (id !== generation.current) return;
        const last = Math.max(0, data.totalPages - 1);
        if (page > last) {
          setPage(last);
          return;
        }
        setState({ data });
      },
      (error) => {
        if (id === generation.current && error.name !== "AbortError")
          setState({ error: error.message });
      },
    );
    return () => {
      generation.current += 1;
      controller.abort();
    };
  }, [filters, query, page, size, revision]);
  return {
    ...state,
    filters,
    page,
    size,
    changeFilters: (next) => {
      invalidate();
      setPage(0);
      setFilters(next);
    },
    changePage: (next) => {
      invalidate();
      setPage(next);
    },
    changeSize: (next) => {
      invalidate();
      setPage(0);
      setSize(next);
    },
    reload: () => {
      invalidate();
      setRevision((value) => value + 1);
    },
  };
}

import { useEffect, useRef, useState } from "react";
import { suppliers } from "../../services/suppliers";

export const emptyFilters = { query: "", searchField: "NAME", active: "" };

export default function useSuppliers() {
  const [filters, setFilters] = useState(emptyFilters);
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
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
    suppliers.search({ ...filters, query }, page, size, { signal: controller.signal }).then(
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

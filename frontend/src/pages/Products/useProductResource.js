import { useEffect, useRef, useState } from "react";

// Callers declare dependencies explicitly, as with useEffect.
export function useProductResource(read, dependencies, enabled = true) {
  const [state, setState] = useState({ loading: enabled });
  const [revision, setRevision] = useState(0);
  const generation = useRef(0);
  useEffect(() => {
    const id = ++generation.current;
    const controller = new AbortController();
    setState(enabled ? { loading: true } : {});
    if (enabled) {
      read({ signal: controller.signal }).then(
        (data) => {
          if (id === generation.current) setState({ data });
        },
        (error) => {
          if (id === generation.current && error.name !== "AbortError")
            setState({ error: error.message });
        },
      );
    }
    return () => {
      generation.current += 1;
      controller.abort();
    };
  }, [...dependencies, enabled, revision]);
  return {
    ...state,
    reload: () => {
      generation.current += 1;
      setState({ loading: true });
      setRevision((n) => n + 1);
    },
  };
}

export function useCatalogPage(read, initialFilters = { query: "", active: "" }) {
  const [filters, setFilters] = useState(initialFilters);
  const [query, setQuery] = useState(initialFilters.query || "");
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
  }, [filters.query, query]);
  useEffect(() => {
    if (filters.query !== query) return;
    const id = ++generation.current;
    const controller = new AbortController();
    currentRequest.current = controller;
    setState({ loading: true });
    read({ ...filters, query }, page, size, { signal: controller.signal }).then(
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
  }, [read, filters, query, page, size, revision]);
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
      if (next === page) return;
      invalidate();
      setPage(next);
    },
    changeSize: (next) => {
      if (next === size) return;
      invalidate();
      setPage(0);
      setSize(next);
    },
    reload: () => {
      invalidate();
      setRevision((n) => n + 1);
    },
  };
}

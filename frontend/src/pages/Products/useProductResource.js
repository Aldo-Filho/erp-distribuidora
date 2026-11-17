import { useEffect, useEffectEvent, useRef, useState } from "react";

// Callers declare a serializable resource identity; callbacks always see current values.
export function useProductResource(read, dependencies, enabled = true, onLoaded) {
  const [result, setResult] = useState(null);
  const [revision, setRevision] = useState(0);
  const generation = useRef(0);
  const signature = JSON.stringify([dependencies, enabled, revision]);
  const [request, setRequest] = useState(() => ({ signature }));
  if (request.signature !== signature) setRequest({ signature });
  const load = useEffectEvent((options) => read(options));
  const loaded = useEffectEvent((data) => onLoaded?.(data));
  const state = !enabled ? {} :
    result?.request === request
      ? result.state : { loading: true };
  useEffect(() => {
    const id = ++generation.current;
    const controller = new AbortController();
    if (enabled) {
      load({ signal: controller.signal }).then(
        (data) => {
          if (id === generation.current) {
            loaded(data);
            setResult({ request, state: { data } });
          }
        },
        (error) => {
          if (id === generation.current && error.name !== "AbortError")
            setResult({ request, state: { error: error.message } });
        },
      );
    }
    return () => {
      generation.current += 1;
      controller.abort();
    };
  }, [request, enabled]);
  return {
    ...state,
    reload: () => {
      generation.current += 1;
      setRevision((n) => n + 1);
    },
  };
}

export function useCatalogPage(read, initialFilters = { query: "", active: "" }, origin) {
  const [filters, setFilters] = useState(initialFilters);
  const [query, setQuery] = useState(initialFilters.query || "");
  const [page, setPage] = useState(() =>
    Number.isSafeInteger(origin?.page) && origin.page >= 0 ? origin.page : 0,
  );
  const [size, setSize] = useState(() => ([25, 50, 100].includes(origin?.size) ? origin.size : 25));
  const [revision, setRevision] = useState(0);
  const [result, setResult] = useState(null);
  const requestKey = JSON.stringify([filters, query, page, size, revision]);
  const state = result?.requestKey === requestKey ? result.state : { loading: true };
  const generation = useRef(0);
  const currentRequest = useRef(null);
  const invalidate = () => {
    generation.current += 1;
    currentRequest.current?.abort();
    setRevision((n) => n + 1);
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
    read({ ...filters, query }, page, size, { signal: controller.signal }).then(
      (data) => {
        if (id !== generation.current) return;
        const last = Math.max(0, data.totalPages - 1);
        if (page > last) {
          setPage(last);
          return;
        }
        setResult({ requestKey, state: { data } });
      },
      (error) => {
        if (id === generation.current && error.name !== "AbortError")
          setResult({ requestKey, state: { error: error.message } });
      },
    );
    return () => {
      generation.current += 1;
      controller.abort();
    };
  }, [read, filters, query, page, size, requestKey]);
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
    },
  };
}

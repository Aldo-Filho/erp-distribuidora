import { useCallback, useEffect, useRef, useState } from "react";

export default function useLoad(loader) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [loadedFor, setLoadedFor] = useState(null);
  const generation = useRef(0);
  const load = useCallback(async () => {
    const request = ++generation.current;
    try {
      const value = await loader();
      if (request === generation.current) {
        setData(value);
        setError("");
      }
    } catch (failure) {
      if (request === generation.current) setError(failure.message);
    } finally {
      if (request === generation.current) {
        setLoadedFor(() => loader);
        setLoading(false);
      }
    }
  }, [loader]);
  const reload = useCallback(() => {
    setLoading(true);
    setError("");
    return load();
  }, [load]);
  useEffect(() => {
    load();
    return () => { generation.current += 1; };
  }, [load]);
  return {
    data: loadedFor === loader ? data : null,
    loading: loading || loadedFor !== loader,
    error: loadedFor === loader ? error : "",
    reload,
  };
}

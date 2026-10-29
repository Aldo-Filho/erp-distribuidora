import { useCallback, useEffect, useState } from "react";

export default function useLoad(loader) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const reload = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await loader());
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoading(false);
    }
  }, [loader]);
  useEffect(() => {
    reload();
  }, [reload]);
  return { data, loading, error, reload };
}

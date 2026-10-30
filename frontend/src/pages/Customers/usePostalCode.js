import useConfirm from "../../components/feedback/useConfirm";
import { useEffect, useRef, useState } from "react";
import { lookupPostalCode } from "../../services/postalCode";

export default function usePostalCode(value, onChange, disabled = false) {
  const confirm = useConfirm();
  const request = useRef(null);
  const sequence = useRef(0);
  const latest = useRef(value);
  latest.current = value;
  const enabled = useRef(!disabled);
  enabled.current = !disabled;
  const assisted = useRef({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const cancel = () => {
    sequence.current += 1;
    request.current?.abort();
    request.current = null;
  };
  useEffect(() => () => cancel(), []);
  useEffect(() => {
    if (disabled) {
      cancel();
      setLoading(false);
    }
  }, [disabled]);
  const change = (next) => {
    cancel();
    setLoading(false);
    setError("");
    onChange(next);
  };
  const lookup = async () => {
    if (request.current || !enabled.current) return;
    const snapshot = value;
    const id = ++sequence.current;
    const controller = new AbortController();
    request.current = controller;
    setLoading(true);
    setError("");
    try {
      const found = await lookupPostalCode(snapshot.zipCode, { signal: controller.signal });
      if (sequence.current !== id || latest.current !== snapshot || !enabled.current) return;
      const replacements = Object.entries(found).some(
        ([field, text]) =>
          text &&
          snapshot[field] &&
          snapshot[field] !== text &&
          snapshot[field] !== assisted.current[field],
      );
      if (
        replacements &&
        !(await confirm("Substituir os campos de endereço preenchidos pelos dados deste CEP?", {
          title: "Atualizar endereço",
          confirmLabel: "Substituir",
        }))
      )
        return;
      if (sequence.current !== id || latest.current !== snapshot || !enabled.current) return;
      const next = { ...snapshot };
      for (const [field, text] of Object.entries(found)) {
        if (text) next[field] = text;
      }
      assisted.current = found;
      onChange(next);
    } catch (issue) {
      if (sequence.current === id && issue.name !== "AbortError") setError(issue.message);
    } finally {
      if (sequence.current === id) {
        request.current = null;
        setLoading(false);
      }
    }
  };
  return { change, lookup, loading, error };
}

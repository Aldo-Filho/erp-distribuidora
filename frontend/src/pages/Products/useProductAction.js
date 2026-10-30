import { useEffect, useRef, useState } from "react";
import useConfirm from "../../components/feedback/useConfirm";

export default function useProductAction() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const locked = useRef(false);
  const live = useRef(true);
  const confirm = useConfirm();
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const run = async (write, message, options = {}) => {
    if (locked.current) return;
    locked.current = true;
    setPending(true);
    try {
      if (message && !(await confirm(message, options))) return;
      if (!live.current) return;
      setError("");
      setNotice("");
      await write();
      if (live.current) setNotice("Alteração salva.");
    } catch (issue) {
      if (live.current) setError(issue.message);
    } finally {
      locked.current = false;
      if (live.current) setPending(false);
    }
  };
  return {
    run,
    pending,
    error,
    notice,
    succeed: (message = "Registro salvo.") => {
      setError("");
      setNotice(message);
    },
  };
}

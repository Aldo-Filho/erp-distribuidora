import { useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { removeDraft, useDraft } from "../../auth/drafts";
import useConfirm from "../../components/feedback/useConfirm";

export default function useProductForm(identity, initialValue, onClose, onSaved) {
  const { session } = useAuth();
  const key = `products:${session.userId}:${identity}`;
  const initial = useRef(initialValue).current;
  const [value, setValue] = useDraft(key, initial);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const lock = useRef(false);
  const live = useRef(true);
  const confirm = useConfirm();
  useEffect(() => {
    live.current = true;
    return () => {
      live.current = false;
    };
  }, []);
  const close = async () => {
    if (lock.current) return;
    if (
      JSON.stringify(value) !== JSON.stringify(initial) &&
      !(await confirm("Descartar as alterações deste formulário?", {
        title: "Descartar alterações",
        confirmLabel: "Descartar",
        destructive: true,
      }))
    )
      return;
    removeDraft(key);
    onClose();
  };
  const submit = async (event, write) => {
    event.preventDefault();
    if (lock.current) return;
    lock.current = true;
    setSaving(true);
    setError("");
    try {
      const saved = await write(value);
      removeDraft(key);
      if (live.current) {
        if (saved === undefined) onClose();
        else onSaved(saved);
      }
    } catch (issue) {
      if (live.current) setError(issue.message);
    } finally {
      lock.current = false;
      if (live.current) setSaving(false);
    }
  };
  return {
    value,
    dirty: JSON.stringify(value) !== JSON.stringify(initial),
    setValue,
    saving,
    error,
    close,
    submit,
    change: (field, next) => setValue((old) => ({ ...old, [field]: next })),
  };
}

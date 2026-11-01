import { useCallback, useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { removeDraft, saveDraft, useDraft } from "../../auth/drafts";
import { purchases } from "../../services/operations";

export const activePurchaseWrites = new Set();

export default function usePurchaseEntry(id) {
  const { has, session } = useAuth();
  const lock = useRef(false);
  const snapshot = useRef(null);
  const generation = useRef(0);
  const request = useRef(null);
  const [busy, setBusy] = useState(false);
  const [state, setState] = useState({ loading: Boolean(id), data: null, error: "" });
  const attemptKey = `purchase-write:${session.userId}:${id || "new"}`;
  const [uncertain, setUncertain] = useDraft(attemptKey, false);
  const reload = useCallback(async () => {
    if (!id) return null;
    const version = ++generation.current;
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    setState((previous) => ({ ...previous, loading: true, error: "" }));
    try {
      const purchase = await purchases.findOne(id, { signal: controller.signal });
      const data = purchase
        ? { purchase, items: await purchases.items(id, { signal: controller.signal }) }
        : null;
      if (version === generation.current) {
        snapshot.current = data;
        setState({ data, loading: false, error: "" });
      }
      return data;
    } catch (issue) {
      if (version === generation.current && issue.name !== "AbortError")
        setState((previous) => ({ ...previous, loading: false, error: issue.message }));
      throw issue;
    }
  }, [id]);
  useEffect(() => {
    reload().catch(() => {});
    return () => {
      generation.current += 1;
      request.current?.abort();
    };
  }, [reload]);
  const write = async (permission, work) => {
    if (!has(permission)) throw new Error("Sem permissão para esta operação.");
    if (
      lock.current ||
      activePurchaseWrites.has(attemptKey) ||
      uncertain ||
      state.loading ||
      state.error
    )
      throw new Error("Confira o estado atual e o resultado anterior antes de gravar novamente.");
    lock.current = true;
    activePurchaseWrites.add(attemptKey);
    setBusy(true);
    let submitted = false;
    let confirmed = false;
    try {
      if (id) {
        const current = await purchases.findOne(id);
        if (current?.status !== "DRAFT") {
          if (current && snapshot.current)
            setState((previous) => ({
              ...previous,
              data: { ...previous.data, purchase: current },
            }));
          await reload();
          throw new Error("A compra não está mais em rascunho. Consulte seu acompanhamento.");
        }
        if (current.supplier.id !== snapshot.current?.purchase.supplier.id) {
          await reload();
          throw new Error(
            "O fornecedor foi alterado em outra sessão. Confira os dados atuais antes de continuar.",
          );
        }
      }
      submitted = true;
      // Save outside React state so navigation/unmount cannot lose an in-flight attempt.
      saveDraft(attemptKey, true);
      const saved = await work();
      confirmed = true;
      removeDraft(attemptKey);
      // A confirmed write must never be reported as failed just because its refresh failed.
      await reload().catch(() => {});
      return saved;
    } catch (issue) {
      if (submitted && !confirmed) {
        if ([400, 401, 403, 404, 409, 422].includes(issue.status)) removeDraft(attemptKey);
        else setUncertain(true);
      }
      throw issue;
    } finally {
      lock.current = false;
      activePurchaseWrites.delete(attemptKey);
      setBusy(false);
    }
  };
  return {
    ...state,
    loading: state.loading && !state.data,
    refreshing: state.loading,
    busy,
    uncertain,
    reload: () => reload().catch(() => {}),
    acknowledge: () => {
      if (activePurchaseWrites.has(attemptKey)) {
        setState((previous) => ({
          ...previous,
          error: "Há uma gravação em andamento. Aguarde e consulte novamente.",
        }));
        return;
      }
      setUncertain(false);
      reload().catch(() => {});
    },
    saveHeader: (body) =>
      write(id ? "purchases.update" : "purchases.create", () =>
        id ? purchases.update(id, body) : purchases.register(body),
      ),
    addItem: (body) => write("purchases.create", () => purchases.addItem(body)),
    updateItem: (itemId, body) =>
      write("purchases.update", () => purchases.updateItem(itemId, body)),
    removeItem: (itemId) => write("purchases.delete", () => purchases.deleteItem(itemId)),
  };
}

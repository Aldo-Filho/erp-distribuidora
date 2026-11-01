import { useCallback, useEffect, useRef, useState } from "react";
import useAuth from "../../auth/useAuth";
import { useDraft } from "../../auth/drafts";
import { orders } from "../../services/operations";
import { salesError } from "./salesErrors";

const lineMatches = (line, body) =>
  Object.entries(body).every(([field, value]) => {
    if (field === "orderId") return true;
    const actual =
      field === "productId"
        ? line.product.id
        : field === "warehouseId"
          ? line.warehouse.id
          : field === "presentationId"
            ? line.presentation?.id
            : line[field];
    return String(actual) === String(value);
  });

export default function useOrderEntry(id) {
  const { session } = useAuth();
  const [data, setData] = useState({ order: null, items: [], loading: true, error: "" });
  const [writePending, setWritePending] = useState(false);
  const [attempt, setAttempt] = useDraft(`order-entry-attempt:${session.userId}:${id}`, null);
  const attemptRef = useRef(attempt);
  attemptRef.current = attempt;
  const lock = useRef(false);
  const live = useRef(true);
  const generation = useRef(0);
  const remember = (next) => {
    attemptRef.current = next;
    setAttempt(next);
  };
  const reload = useCallback(async () => {
    if (!id) return null;
    const request = ++generation.current;
    try {
      const [order, items] = await Promise.all([orders.findOne(id), orders.items(id)]);
      const snapshot = { order, items, loading: false, error: "" };
      if (live.current && request === generation.current) setData(snapshot);
      return snapshot;
    } catch (issue) {
      if (live.current && request === generation.current) {
        setData((previous) => ({ ...previous, loading: false, error: salesError(issue.message) }));
      }
      throw issue;
    }
  }, [id]);
  useEffect(() => {
    live.current = true;
    reload().catch(() => {});
    return () => {
      live.current = false;
      generation.current += 1;
    };
  }, [reload]);
  const confirmed = (snapshot, intent) => {
    if (intent.kind === "remove") return !snapshot.items.some((line) => line.id === intent.itemId);
    if (intent.kind === "header") {
      return Object.entries(intent.body).every(
        ([field, value]) =>
          String(field === "customerId" ? snapshot.order?.customer.id : snapshot.order?.[field]) ===
          String(value),
      );
    }
    return snapshot.items.find(
      (line) =>
        (intent.kind !== "update" || line.id === intent.itemId) && lineMatches(line, intent.body),
    );
  };
  const reconcile = async () => {
    if (lock.current) return false;
    lock.current = true;
    setWritePending(true);
    try {
      const snapshot = await reload();
      const intent = attemptRef.current;
      if (intent && !confirmed(snapshot, intent)) {
        throw new Error(
          "O resultado ainda não foi identificado. Consulte o pedido antes de repetir a operação.",
        );
      }
      remember(null);
      return true;
    } finally {
      lock.current = false;
      if (live.current) setWritePending(false);
    }
  };
  const run = async (intent, write) => {
    if (lock.current || attemptRef.current)
      throw new Error("Aguarde a operação ou confira seu resultado antes de continuar.");
    lock.current = true;
    setWritePending(true);
    remember(intent);
    let saved;
    try {
      try {
        saved = await write();
      } catch (issue) {
        if ([400, 401, 403, 404, 405, 409, 422, 429].includes(issue.status)) {
          remember(null);
          await reload().catch(() => {});
          throw issue;
        }
        const snapshot = await reload().catch(() => null);
        const result = snapshot && confirmed(snapshot, intent);
        if (!result)
          throw new Error(
            "Resultado não confirmado. Use Conferir resultado antes de registrar outra alteração.",
          );
        saved = intent.kind === "header" ? snapshot.order : result;
      }
      remember(null);
      await reload().catch(() => {});
      return saved;
    } finally {
      lock.current = false;
      if (live.current) setWritePending(false);
    }
  };
  return {
    ...data,
    writePending,
    uncertain: Boolean(attempt),
    reload,
    reconcile,
    saveHeader: (body) => run({ kind: "header", body }, () => orders.update(id, body)),
    addItem: (body) => run({ kind: "add", body }, () => orders.addItem(body)),
    updateItem: (itemId, body) =>
      run({ kind: "update", itemId, body }, () => orders.updateItem(itemId, body)),
    removeItem: (itemId) => run({ kind: "remove", itemId }, () => orders.deleteItem(itemId)),
    confirmOrder: () =>
      run({ kind: "header", body: { status: "CONFIRMED" } }, () =>
        orders.update(id, { status: "CONFIRMED" }),
      ),
    cancelOrder: () =>
      run({ kind: "header", body: { status: "CANCELED" } }, () =>
        orders.update(id, { status: "CANCELED" }),
      ),
  };
}

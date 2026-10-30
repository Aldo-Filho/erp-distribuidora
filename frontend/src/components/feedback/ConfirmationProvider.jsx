import { createContext, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import Modal from "./Modal";

export const ConfirmationContext = createContext(null);

export default function ConfirmationProvider({ children }) {
  const [confirmation, setConfirmation] = useState(null);
  const pending = useRef(null);
  const cancelButton = useRef(null);
  const finish = useCallback((accepted) => {
    const current = pending.current;
    pending.current = null;
    setConfirmation(null);
    current?.resolve(accepted);
  }, []);
  const request = useCallback((message, options = {}, owner) => {
    if (pending.current) return Promise.resolve(false);
    return new Promise((resolve) => {
      pending.current = { resolve, owner };
      setConfirmation({ message, ...options });
    });
  }, []);
  const cancel = useCallback(
    (owner) => {
      if (pending.current?.owner === owner) finish(false);
    },
    [finish],
  );
  useEffect(
    () => () => {
      pending.current?.resolve(false);
      pending.current = null;
    },
    [],
  );
  const context = useMemo(() => ({ request, cancel }), [request, cancel]);

  return (
    <ConfirmationContext.Provider value={context}>
      {children}
      {confirmation &&
        createPortal(
          <Modal
            title={confirmation.title || "Confirmar ação"}
            onClose={() => finish(false)}
            initialFocusRef={cancelButton}
            className="confirmation-dialog"
          >
            <p>{confirmation.message}</p>
            <div className="actions">
              <button
                ref={cancelButton}
                type="button"
                className="secondary"
                onClick={() => finish(false)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={confirmation.destructive ? "confirmation-danger" : undefined}
                onClick={() => finish(true)}
              >
                {confirmation.confirmLabel || "Confirmar"}
              </button>
            </div>
          </Modal>,
          document.body,
        )}
    </ConfirmationContext.Provider>
  );
}

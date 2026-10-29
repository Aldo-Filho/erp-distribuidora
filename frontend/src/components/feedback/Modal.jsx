import { useEffect, useId, useRef } from "react";
import { X } from "lucide-react";

export default function Modal({ title, onClose, busy = false, wide = false, children }) {
  const dialog = useRef(null);
  const current = useRef({ onClose, busy });
  current.current = { onClose, busy };
  const titleId = useId();

  useEffect(() => {
    const previous = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const topDialog = () =>
      [...document.querySelectorAll('[role="dialog"][aria-modal="true"]')]
        .filter((element) => element.getClientRects().length)
        .at(-1);
    const focusable = () =>
      [
        ...dialog.current.querySelectorAll(
          'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]',
        ),
      ].filter((element) => element.getClientRects().length);
    if (topDialog() === dialog.current) {
      (focusable().find((element) => element.tagName === "INPUT") || dialog.current).focus();
    }
    const keydown = (event) => {
      if (topDialog() !== dialog.current) return;
      if (event.key === "Escape") {
        event.preventDefault();
        event.stopPropagation();
        if (!current.current.busy) current.current.onClose();
      }
      if (event.key === "Tab") {
        const elements = focusable();
        const first = elements[0];
        const last = elements.at(-1);
        if (!first) {
          event.preventDefault();
          dialog.current.focus();
        } else if (
          event.shiftKey &&
          (document.activeElement === first || document.activeElement === dialog.current)
        ) {
          event.preventDefault();
          last.focus();
        } else if (
          !event.shiftKey &&
          (document.activeElement === last || document.activeElement === dialog.current)
        ) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    const contain = (event) => {
      if (topDialog() === dialog.current && !dialog.current.contains(event.target)) {
        dialog.current.focus();
      }
    };
    document.addEventListener("keydown", keydown);
    document.addEventListener("focusin", contain);
    return () => {
      document.removeEventListener("keydown", keydown);
      document.removeEventListener("focusin", contain);
      document.body.style.overflow = overflow;
      const remaining = topDialog();
      if (previous?.isConnected && (!remaining || remaining.contains(previous))) previous.focus();
    };
  }, []);

  return (
    <div className="modal-backdrop">
      <section
        ref={dialog}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-busy={busy}
        className={"dialog record-dialog" + (wide ? " dialog-wide" : "")}
      >
        <div className="dialog-heading">
          <h2 id={titleId}>{title}</h2>
          <button
            type="button"
            className="icon-button"
            aria-label="Fechar janela"
            disabled={busy}
            onClick={onClose}
          >
            <X size={20} />
          </button>
        </div>
        {children}
      </section>
    </div>
  );
}

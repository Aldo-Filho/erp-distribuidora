import { Children, isValidElement, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { MoreHorizontal } from "lucide-react";

export default function RowActions({ children, className = "", label = "Ações do registro" }) {
  const actions = Children.toArray(children).filter(isValidElement);
  const [position, setPosition] = useState(null);
  const trigger = useRef(null);
  const menu = useRef(null);
  const firstFocus = useRef("first");
  const id = useId();

  const show = (focus = "first") => {
    const rect = trigger.current.getBoundingClientRect();
    const width = Math.min(220, window.innerWidth - 24);
    const height = Math.min(actions.length * 40 + 12, window.innerHeight - 24);
    firstFocus.current = focus;
    const top =
      window.innerHeight - rect.bottom >= height + 8
        ? rect.bottom + 6
        : Math.max(12, rect.top - height - 6);
    setPosition({
      top,
      left: Math.max(12, Math.min(rect.right - width, window.innerWidth - width - 12)),
      width,
    });
  };
  const close = (restore = false) => {
    setPosition(null);
    if (restore) trigger.current?.focus();
  };

  useEffect(() => {
    if (!position) return;
    const items = [...menu.current.querySelectorAll("button:not(:disabled)")];
    (firstFocus.current === "last" ? items.at(-1) : items[0])?.focus({ preventScroll: true });
    const anchor = trigger.current.getBoundingClientRect();
    const outside = (event) => {
      if (!menu.current?.contains(event.target) && !trigger.current?.contains(event.target))
        setPosition(null);
    };
    const scroll = (event) => {
      if (menu.current?.contains(event.target)) return;
      const current = trigger.current?.getBoundingClientRect();
      if (
        !current ||
        Math.abs(current.top - anchor.top) > 1 ||
        Math.abs(current.left - anchor.left) > 1
      ) {
        setPosition(null);
      }
    };
    const resize = () => setPosition(null);
    document.addEventListener("pointerdown", outside);
    window.addEventListener("scroll", scroll, true);
    window.addEventListener("resize", resize);
    return () => {
      document.removeEventListener("pointerdown", outside);
      window.removeEventListener("scroll", scroll, true);
      window.removeEventListener("resize", resize);
    };
  }, [position]);

  const keydown = (event) => {
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();
      close(true);
    } else if (event.key === "Tab") {
      close(true);
    } else if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
      event.preventDefault();
      const items = [...menu.current.querySelectorAll("button:not(:disabled)")];
      const index = items.indexOf(document.activeElement);
      if (event.key === "Home") items[0]?.focus();
      else if (event.key === "End") items.at(-1)?.focus();
      else
        items[
          (index + (event.key === "ArrowDown" ? 1 : -1) + items.length) % items.length
        ]?.focus();
    }
  };

  return (
    <div className="responsive-row-actions">
      <div className={"row-actions row-actions-inline " + className}>{children}</div>
      <button
        ref={trigger}
        type="button"
        className="row-actions-more"
        aria-label={label}
        title="Ações"
        aria-haspopup="menu"
        aria-expanded={!!position}
        aria-controls={position ? id : undefined}
        disabled={actions.every((action) => action.props.disabled)}
        onClick={() => (position ? close() : show())}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            show(event.key === "ArrowUp" ? "last" : "first");
          }
        }}
      >
        <MoreHorizontal size={20} aria-hidden="true" />
      </button>
      {position &&
        createPortal(
          <div
            id={id}
            ref={menu}
            role="menu"
            aria-label={label}
            className="row-actions-menu"
            style={position}
            onKeyDown={keydown}
          >
            {actions.map((action, index) => {
              const text = action.props.title;
              return (
                <button
                  key={action.key || index}
                  type="button"
                  role="menuitem"
                  tabIndex={-1}
                  disabled={action.props.disabled}
                  className={
                    "row-actions-item" +
                    (action.props.className?.includes("danger-link") ? " is-danger" : "")
                  }
                  onClick={(event) => {
                    close(true);
                    action.props.onClick?.(event);
                  }}
                >
                  {action.props.children}
                  {text && <span>{text}</span>}
                </button>
              );
            })}
          </div>,
          trigger.current?.closest('[role="dialog"]') || document.body,
        )}
    </div>
  );
}

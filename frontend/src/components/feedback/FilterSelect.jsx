import { Children, useEffect, useId, useRef, useState } from "react";
import { Check, ChevronDown } from "lucide-react";

export default function FilterSelect({
  value,
  onChange,
  children,
  disabled = false,
  required = false,
  ...props
}) {
  const options = Children.toArray(children).filter((child) => child?.type === "option");
  const selected = options.findIndex((option) => String(option.props.value) === String(value));
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [above, setAbove] = useState(false);
  const [listHeight, setListHeight] = useState(240);
  const [validationRequested, setValidationRequested] = useState(false);
  const invalid = validationRequested && required && (value == null || String(value) === "");
  const root = useRef(null);
  const trigger = useRef(null);
  const list = useRef(null);
  const search = useRef({ text: "", at: 0 });
  const id = useId();

  useEffect(() => {
    if (!open) return;
    const outside = (event) => {
      if (!root.current.contains(event.target)) setOpen(false);
    };
    const resize = () => setOpen(false);
    const scroll = (event) => {
      if (list.current?.contains(event.target)) return;
      setOpen(false);
    };
    document.addEventListener("pointerdown", outside);
    window.addEventListener("resize", resize);
    window.addEventListener("scroll", scroll, true);
    return () => {
      document.removeEventListener("pointerdown", outside);
      window.removeEventListener("resize", resize);
      window.removeEventListener("scroll", scroll, true);
    };
  }, [open]);

  useEffect(() => {
    if (!open || !list.current) return;
    const option = document.getElementById(`${id}-${active}`);
    if (!option) return;
    const bounds = list.current.getBoundingClientRect();
    const optionBounds = option.getBoundingClientRect();
    const top = bounds.top + list.current.clientTop;
    const bottom = top + list.current.clientHeight;
    // Scroll only the options; scrollIntoView can move the surrounding modal as well.
    if (optionBounds.top < top) list.current.scrollTop += optionBounds.top - top;
    else if (optionBounds.bottom > bottom) list.current.scrollTop += optionBounds.bottom - bottom;
  }, [active, open, id]);

  const show = () => {
    const rect = trigger.current.getBoundingClientRect();
    let top = 8;
    let bottom = window.innerHeight - 8;
    // An absolute list is clipped by every scroll container, including record dialogs.
    for (
      let ancestor = trigger.current.parentElement;
      ancestor && ancestor !== document.body && ancestor !== document.documentElement;
      ancestor = ancestor.parentElement
    ) {
      if (!/auto|scroll|hidden|clip/.test(window.getComputedStyle(ancestor).overflowY)) continue;
      const bounds = ancestor.getBoundingClientRect();
      top = Math.max(top, bounds.top + ancestor.clientTop + 4);
      bottom = Math.min(bottom, bounds.top + ancestor.clientTop + ancestor.clientHeight - 4);
    }
    const spaceAbove = Math.max(0, rect.top - top - 6);
    const spaceBelow = Math.max(0, bottom - rect.bottom - 6);
    const expectedHeight = Math.min(240, options.length * 40 + 10);
    const opensAbove = spaceBelow < expectedHeight && spaceAbove > spaceBelow;
    setAbove(opensAbove);
    setListHeight(Math.min(240, opensAbove ? spaceAbove : spaceBelow));
    setActive(Math.max(selected, 0));
    setOpen(true);
  };
  const choose = (index) => {
    const option = options[index];
    if (!option || option.props.disabled) return;
    onChange({ target: { value: String(option.props.value) } });
    setOpen(false);
    trigger.current.focus();
  };
  const keydown = (event) => {
    if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
      event.preventDefault();
      if (!open) show();
      else {
        const enabled = options
          .map((option, index) => (option.props.disabled ? -1 : index))
          .filter((index) => index >= 0);
        const position = enabled.indexOf(active);
        if (event.key === "Home") setActive(enabled[0] ?? 0);
        else if (event.key === "End") setActive(enabled.at(-1) ?? 0);
        else
          setActive(
            enabled[
              (position + (event.key === "ArrowDown" ? 1 : -1) + enabled.length) % enabled.length
            ] ?? 0,
          );
      }
    } else if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      if (open) choose(active);
      else show();
    } else if (event.key === "Escape") {
      if (open) {
        event.preventDefault();
        event.stopPropagation();
        setOpen(false);
      }
    } else if (event.key === "Tab") {
      setOpen(false);
    } else if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
      const now = Date.now();
      search.current.text =
        now - search.current.at < 700 ? search.current.text + event.key : event.key;
      search.current.at = now;
      const index = options.findIndex(
        (option) =>
          !option.props.disabled &&
          String(option.props.children)
            .toLocaleLowerCase("pt-BR")
            .startsWith(search.current.text.toLocaleLowerCase("pt-BR")),
      );
      if (index >= 0) {
        if (!open) show();
        setActive(index);
      }
    }
  };

  return (
    <span className="filter-select" ref={root}>
      <button
        {...props}
        ref={trigger}
        type="button"
        role="combobox"
        className="filter-select-trigger"
        disabled={disabled}
        aria-required={required || undefined}
        aria-invalid={invalid || undefined}
        aria-describedby={invalid ? `${id}-error` : props["aria-describedby"]}
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={open ? id : undefined}
        aria-activedescendant={open ? `${id}-${active}` : undefined}
        onClick={() => (open ? setOpen(false) : show())}
        onKeyDown={keydown}
        onBlur={() => setOpen(false)}
      >
        <span>{options[selected]?.props.children || "Selecionar"}</span>
        <ChevronDown size={16} aria-hidden="true" />
      </button>
      {required && (
        <select
          className="filter-select-validation-control"
          value={value}
          onChange={onChange}
          required
          disabled={disabled}
          tabIndex={-1}
          aria-hidden="true"
          onInvalid={(event) => {
            event.preventDefault();
            setValidationRequested(true);
            trigger.current.focus();
          }}
        >
          {children}
        </select>
      )}
      {invalid && (
        <span id={`${id}-error`} className="filter-select-error">
          Selecione uma opção.
        </span>
      )}
      {open && (
        <span
          ref={list}
          id={id}
          role="listbox"
          aria-label={props["aria-label"] || "Opções"}
          className={"filter-select-options" + (above ? " opens-above" : "")}
          style={{ maxHeight: listHeight }}
        >
          {options.map((option, index) => (
            <span
              key={option.props.value}
              id={`${id}-${index}`}
              role="option"
              aria-selected={index === selected}
              aria-disabled={option.props.disabled || undefined}
              className={"filter-select-option" + (index === active ? " is-active" : "")}
              onPointerMove={() => !option.props.disabled && setActive(index)}
              onMouseDown={(event) => event.preventDefault()}
              onClick={(event) => {
                event.preventDefault();
                choose(index);
              }}
            >
              <span>{option.props.children}</span>
              {index === selected && <Check size={15} aria-hidden="true" />}
            </span>
          ))}
        </span>
      )}
    </span>
  );
}

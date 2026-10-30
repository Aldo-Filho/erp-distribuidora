import { useRef } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import { decimal } from "./productFormData";

export default function ProductNumberInput({
  value,
  onChange,
  label,
  places = 2,
  max = 9999999999.99,
  disabled = false,
  ...props
}) {
  const input = useRef(null);
  const adjust = (direction) => {
    if (disabled || input.current?.matches(":disabled")) return;
    try {
      decimal(value === "" ? "0" : value, label, { places, max });
      const [whole, fraction = ""] = String(value || "0")
        .trim()
        .replace(",", ".")
        .split(".");
      const scale = 10n ** BigInt(places);
      const current =
        BigInt(whole) * scale + BigInt(fraction.replace(/0+$/, "").padEnd(places, "0") || "0");
      const next = current + BigInt(direction) * scale;
      if (next < 0n) return;
      const text = places
        ? `${next / scale},${String(next % scale).padStart(places, "0")}`
        : String(next);
      decimal(text, label, { places, max });
      input.current.setCustomValidity("");
      onChange({ target: { value: text } });
    } catch (error) {
      input.current.setCustomValidity(error.message);
      input.current.reportValidity();
    }
  };
  return (
    <span className="product-number-input">
      <input
        {...props}
        ref={input}
        disabled={disabled}
        inputMode={places ? "decimal" : "numeric"}
        value={value}
        onChange={(event) => {
          event.target.setCustomValidity("");
          onChange(event);
        }}
        onKeyDown={(event) => {
          if (event.key === "ArrowUp" || event.key === "ArrowDown") {
            event.preventDefault();
            adjust(event.key === "ArrowUp" ? 1 : -1);
          }
        }}
      />
      <span className="product-number-steps">
        <button
          type="button"
          disabled={disabled}
          aria-label={`Aumentar ${label}`}
          onClick={() => adjust(1)}
        >
          <ChevronUp size={13} aria-hidden="true" />
        </button>
        <button
          type="button"
          disabled={disabled}
          aria-label={`Diminuir ${label}`}
          onClick={() => adjust(-1)}
        >
          <ChevronDown size={13} aria-hidden="true" />
        </button>
      </span>
    </span>
  );
}

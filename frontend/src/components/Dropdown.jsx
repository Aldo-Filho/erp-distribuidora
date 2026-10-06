import { useEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";

/**
 * Dropdown reutilizável, feito em React e Tailwind.
 *
 * Diferente de <select>, a lista aberta não é desenhada pelo sistema
 * operacional. Assim, bordas, espaçamentos e cores são iguais ao restante da tela.
 * `options` recebe itens no formato { value, label }; o primeiro item pode ter
 * value vazio para representar a opção "Todos".
 */
export function Dropdown({ value, onChange, options, ariaLabel }) {
  // isOpen controla somente a visibilidade da lista; o valor selecionado é
  // controlado pela página pai através das props value e onChange.
  const [isOpen, setIsOpen] = useState(false);
  // A referência aponta para botão e lista, permitindo reconhecer cliques externos.
  const rootRef = useRef(null);
  // Localiza o texto a exibir no botão a partir do valor que a página mantém.
  const selectedOption =
    options.find((option) => String(option.value) === String(value)) ||
    options[0];

  // Fecha a lista quando o usuário clica fora dela. Isto evita menus esquecidos abertos.
  useEffect(() => {
    function closeWhenClickingOutside(event) {
      if (!rootRef.current?.contains(event.target)) setIsOpen(false);
    }

    document.addEventListener("mousedown", closeWhenClickingOutside);
    return () =>
      document.removeEventListener("mousedown", closeWhenClickingOutside);
  }, []);

  function selectOption(option) {
    // Devolve só o value ao pai. O pai atualiza seu filtro e a página recalcula a tabela.
    onChange(String(option.value));
    setIsOpen(false);
  }

  return (
    <div ref={rootRef} className="relative">
      {/* O botão é o controle visível do dropdown; aria-* informa seu estado a leitores de tela. */}
      <button
        type="button"
        className="flex h-10 w-full items-center justify-between rounded-lg border border-slate-200 bg-white px-3 text-left text-sm text-slate-800 shadow-sm outline-none transition hover:border-slate-300 focus:border-blue-600 focus:ring-2 focus:ring-blue-100"
        onClick={() => setIsOpen((current) => !current)}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <span className="truncate">{selectedOption.label}</span>
        <span
          className={`text-slate-400 transition-transform ${isOpen ? "rotate-180" : ""}`}
        >
          <ChevronDown size={16} strokeWidth={1.8} aria-hidden="true" />
        </span>
      </button>
      {/* A lista existe no DOM apenas aberta, evitando que ela cubra a tabela fechada. */}
      {isOpen && (
        <ul
          className="absolute z-20 mt-1 max-h-60 w-full min-w-40 overflow-auto rounded-lg border border-slate-200 bg-white py-1 shadow-lg"
          role="listbox"
          aria-label={ariaLabel}
        >
          {options.map((option) => {
            const isSelected = String(option.value) === String(value);
            return (
              <li key={option.value}>
                <button
                  type="button"
                  className={`w-full px-3 py-2 text-left text-sm transition ${isSelected ? "bg-blue-50 font-semibold text-blue-700" : "text-slate-700 hover:bg-slate-50"}`}
                  onClick={() => selectOption(option)}
                  role="option"
                  aria-selected={isSelected}
                >
                  {option.label}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

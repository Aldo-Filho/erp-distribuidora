import { X } from "lucide-react";

export function Modal({ title, onClose, children }) {
  /**
   * O fundo chama onClose ao receber clique. O section abaixo interrompe o evento,
   * permitindo clicar e preencher o conteúdo interno sem fechar o modal.
   * `children` recebe qualquer formulário ou conteúdo do módulo chamador.
   */
  return (
    <div
      className="fixed inset-0 z-20 grid place-items-center bg-slate-950/45 p-3 sm:p-6"
      onMouseDown={onClose}
    >
      <section
        className="max-h-[calc(100dvh-1.5rem)] min-w-0 w-full max-w-2xl overflow-y-auto overscroll-contain rounded-2xl bg-white p-4 shadow-2xl sm:max-h-[calc(100dvh-3rem)] sm:p-6"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="mb-4 flex items-center justify-between gap-3 sm:mb-6">
          <h2 className="min-w-0 break-words text-lg font-semibold text-slate-900 sm:text-xl">
            {title}
          </h2>
          <button
            className="grid h-10 w-10 shrink-0 place-items-center rounded-md text-slate-500 hover:bg-slate-100"
            onClick={onClose}
            aria-label="Fechar"
          >
            <X size={18} strokeWidth={1.8} aria-hidden="true" />
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}

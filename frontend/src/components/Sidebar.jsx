import {
  Boxes,
  Box,
  Warehouse,
  LayoutGrid,
  ShoppingCart,
  Users,
  Truck,
} from "lucide-react";

// Um item aqui já cria automaticamente uma opção no menu.
// Para novos módulos, inclua o ícone e o rótulo nesta lista.
const menuItems = [
  [LayoutGrid, "Dashboard"],
  [ShoppingCart, "Pedidos"],
  [Box, "Produtos"],
  [Warehouse, "Armazéns", "armazens"],
  [Users, "Clientes"],
  [Truck, "Fornecedores"],
];

/**
 * Barra lateral compartilhada por todos os módulos.
 *
 * `collapsed` vem do App porque tanto a barra como o conteúdo principal
 * precisam saber a largura atual. `onToggle` altera esse estado no App.
 */
export function Sidebar({ collapsed, onToggle, activePage = "produtos" }) {
  // `w-16` equivale a 64 px. Só a partir de `lg` (1024 px) a barra pode
  // assumir w-48 (192 px). Isso torna a versão compacta automática em telas menores.
  const widthClass = collapsed ? "w-16" : "w-16 lg:w-48";
  // Os rótulos aparecem depois que a barra começa a abrir; os ícones ficam fixos.
  const labelClass = collapsed ? "opacity-0" : "opacity-100 delay-150";

  return (
    <aside
      className={`fixed inset-y-0 left-0 z-10 flex flex-col overflow-hidden border-r border-slate-750 bg-slate-900 text-slate-300 transition-[width] duration-300 ${widthClass}`}
    >
      {/* A coluna da marca mantém a mesma largura durante toda a animação. */}
      <div className="flex h-16 shrink-0 items-center border-b border-slate-800 text-white">
        <span className="grid w-16 shrink-0 place-items-center">
          <span className="grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-blue-600">
            <Boxes size={19} strokeWidth={1.8} aria-hidden="true" />
          </span>
        </span>
        <strong
          className={`hidden shrink-0 whitespace-nowrap lg:block lg:transition-opacity lg:duration-150 ${labelClass}`}
        >
          JamsHub
        </strong>
      </div>

      {/* A coluna fixa impede que o fechamento recentralize os ícones. */}
      <nav className="grid gap-1 p-3">
        {menuItems.map(([MenuIcon, label, route = label.toLowerCase()]) => {
          const isActive = route === activePage;
          const colors = isActive
            ? "bg-blue-600 text-white shadow-lg shadow-blue-950/30"
            : "hover:bg-slate-900 hover:text-white";

          return (
            <a
              key={label}
              href={`#${route}`}
              aria-current={isActive ? "page" : undefined}
              title={label}
              className={`flex min-w-0 items-center overflow-hidden rounded-lg py-3 text-sm font-semibold transition-colors ${colors}`}
            >
              <span className="grid w-10 shrink-0 place-items-center">
                <MenuIcon size={18} strokeWidth={1.8} aria-hidden="true" />
              </span>
              {/* O texto só aparece em lg e quando o usuário não recolheu o menu. */}
              <span
                className={`hidden shrink-0 whitespace-nowrap pl-2 lg:inline-block lg:transition-opacity lg:duration-150 ${labelClass}`}
              >
                {label}
              </span>
            </a>
          );
        })}
      </nav>

      {/* Em telas menores o menu já é sempre compacto; por isso não há botão. */}
      <button
        className="mt-auto hidden h-12 shrink-0 items-center border-t border-slate-800 text-2xl hover:bg-slate-900 lg:flex"
        onClick={onToggle}
        aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
      >
        <span className="grid w-16 shrink-0 place-items-center">
          {collapsed ? "›" : "‹"}
        </span>
      </button>
    </aside>
  );
}

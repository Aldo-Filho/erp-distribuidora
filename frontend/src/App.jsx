import { useEffect, useState } from "react";
import { Sidebar } from "./components/Sidebar";
import { ProductsPage } from "./pages/Products/ProductsPage";
import { WarehousesPage } from "./pages/Warehouses/WarehousesPage";

/**
 * Componente-raiz: concentra somente o layout que todos os módulos compartilham.
 * O fragmento da URL permite navegar e voltar entre os módulos disponíveis.
 */
function App() {
  // O estado é mantido aqui, e não no Sidebar, para ajustar a margem do <main>
  // exatamente na mesma hora em que a largura da barra muda.
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [page, setPage] = useState(() => window.location.hash === "#armazens" ? "armazens" : "produtos");

  useEffect(() => {
    function updatePage() {
      setPage(window.location.hash === "#armazens" ? "armazens" : "produtos");
    }
    window.addEventListener("hashchange", updatePage);
    return () => window.removeEventListener("hashchange", updatePage);
  }, []);

  // Em qualquer tela abaixo de lg a barra ocupa 64 px. Em lg, ela passa de
  // 64 para 192 px de acordo com o botão de recolher.
  const contentMargin = isSidebarCollapsed ? "ml-16" : "ml-16 lg:ml-48";

  return (
    <div className="min-h-screen bg-slate-50">
      <Sidebar
        activePage={page}
        collapsed={isSidebarCollapsed}
        onToggle={() => setIsSidebarCollapsed((current) => !current)}
      />
      <main
        className={`min-h-screen p-4 transition-[margin] duration-300 lg:p-8 ${contentMargin}`}
      >
        {page === "armazens" ? <WarehousesPage /> : <ProductsPage />}
      </main>
    </div>
  );
}

export default App;

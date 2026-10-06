// Compila JSX e ativa a atualização automática para componentes React.
import react from "@vitejs/plugin-react";
// Lê as classes Tailwind usadas nos JSX e gera o CSS correspondente.
import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  // Os dois plugins são necessários: um para React e outro para Tailwind.
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      "/api": {
        // O navegador chama /api/product, sem CORS; o Vite encaminha a chamada
        // para o Spring e remove o prefixo /api antes de enviá-la.
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});

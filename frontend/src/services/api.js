/**
 * Camada única de comunicação com a API.
 * As páginas devem usar api() e não fetch direto, para reutilizar a URL-base,
 * o cabeçalho JSON e o tratamento de erros nos próximos módulos.
 */
// No desenvolvimento, o proxy do Vite leva /api ao backend Spring na porta 8080.
// Em produção, VITE_API_URL permite trocar o endereço sem mudar este arquivo.
const API_URL = import.meta.env.VITE_API_URL || "/api";

/**
 * @param {string} path Rota da API, como '/product/search'.
 * @param {RequestInit} options Opções do fetch, como method, body e headers.
 * @returns {Promise<unknown|null>} JSON em sucesso, ou null para uma resposta 204.
 */
export async function api(path, options = {}) {
  // Mantém opções específicas de cada chamada e padroniza envio de JSON.
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  // O fetch não lança erro automaticamente para HTTP 4xx/5xx; esta verificação
  // converte a resposta de erro do Spring em Error para a página exibir.
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || "Não foi possível concluir a operação.");
  }

  // DELETE retorna 204 sem corpo. Os demais endpoints devolvem JSON.
  return response.status === 204 ? null : response.json();
}

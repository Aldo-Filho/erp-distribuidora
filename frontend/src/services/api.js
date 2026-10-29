let csrf;
let csrfRequest;
let csrfGeneration = 0;

export class ApiError extends Error {
  constructor(status, data) {
    const messages = {
      401: "Sua sessão terminou ou as credenciais são inválidas.",
      403: "Acesso negado ou proteção da requisição expirada. Atualize e tente novamente.",
      405: "Esta operação não está disponível.",
      409: "A operação conflita com o estado atual. Atualize os dados.",
      429: "Muitas tentativas. Aguarde antes de tentar novamente.",
    };
    super(messages[status] || data?.message || "Não foi possível concluir a solicitação.");
    this.status = status;
    this.data = data;
  }
}

export function clearCsrf() {
  csrfGeneration += 1;
  csrf = undefined;
  csrfRequest = undefined;
}

export async function loadCsrf() {
  if (!csrfRequest) {
    const generation = csrfGeneration;
    const request = fetch("/api/auth/csrf", { credentials: "same-origin", cache: "no-store" })
      .catch(() => {
        throw new Error("Não foi possível acessar o servidor. Confira a conexão.");
      })
      .then(async (response) => {
        const data = await response.json().catch(() => null);
        if (!response.ok) throw new ApiError(response.status, data);
        if (generation !== csrfGeneration) return loadCsrf();
        csrf = data;
        return data;
      })
      .finally(() => {
        if (csrfRequest === request) csrfRequest = undefined;
      });
    csrfRequest = request;
  }
  return csrfRequest;
}

export async function api(path, { method = "GET", body, publicRequest = false, signal } = {}) {
  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (!["GET", "HEAD"].includes(method)) {
    const token = csrf || (await loadCsrf());
    headers[token.headerName] = token.token;
  }
  let response;
  try {
    response = await fetch("/api" + path, {
      method,
      headers,
      credentials: "same-origin",
      cache: "no-store",
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error.name === "AbortError") throw error;
    throw new Error("Não foi possível acessar o servidor. Confira a conexão.");
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && !publicRequest) {
      window.dispatchEvent(new Event("erp:session-expired"));
    }
    if (response.status === 403) clearCsrf();
    throw new ApiError(response.status, data);
  }
  return data;
}

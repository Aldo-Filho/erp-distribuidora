export function validPostalCode(value) {
  return /^\d{5}-?\d{3}$/.test(value.trim());
}

export async function lookupPostalCode(zipCode, { signal } = {}) {
  if (!validPostalCode(zipCode))
    throw new Error("Informe um CEP com oito dígitos, como 01001-000.");
  const controller = new AbortController();
  const cancel = () => controller.abort();
  if (signal?.aborted) controller.abort();
  signal?.addEventListener("abort", cancel, { once: true });
  let timedOut = false;
  const timer = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, 8000);
  try {
    const response = await fetch(`https://viacep.com.br/ws/${zipCode.replace(/\D/g, "")}/json/`, {
      signal: controller.signal,
      credentials: "omit",
      referrerPolicy: "no-referrer",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) throw new Error("unavailable");
    const data = await response.json();
    if (data.erro) throw new Error("CEP não encontrado. Preencha o endereço manualmente.");
    if (!data.cep || !data.uf || !data.localidade) throw new Error("unavailable");
    return {
      state: data.uf,
      city: data.localidade,
      neighborhood: data.bairro || "",
      street: data.logradouro || "",
    };
  } catch (error) {
    if (signal?.aborted) throw new DOMException("Consulta cancelada", "AbortError");
    if (error.message.startsWith("CEP não encontrado")) throw error;
    throw new Error(
      timedOut
        ? "A consulta de CEP demorou demais. Tente novamente ou preencha manualmente."
        : "Consulta de CEP indisponível. Preencha o endereço manualmente.",
    );
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", cancel);
  }
}

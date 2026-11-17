export default function DocumentSteps({ purchase = false, status, reviewing, saving, dirty }) {
  const steps = purchase
    ? ["Montar compra", "Registrar envio", "Confirmar com fornecedor", "Receber"]
    : ["Montar pedido", "Revisar e reservar", "Despachar"];
  const current = purchase
    ? ({ DRAFT: reviewing ? 1 : 0, SENT: 2, CONFIRMED: 3, PARTIALLY_RECEIVED: 3, RECEIVED: 3 }[
        status
      ] ?? 0)
    : ({ PENDING: reviewing ? 1 : 0, CONFIRMED: 2, INVOICED: 2 }[status] ?? 0);
  const finished = status === "RECEIVED" || status === "INVOICED";
  return (
    <section className="panel document-progress" aria-label="Etapas do documento">
      <ol>
        {steps.map((step, index) => (
          <li
            key={step}
            aria-current={
              !finished && status !== "CANCELED" && index === current ? "step" : undefined
            }
            className={index < current || finished ? "is-complete" : ""}
          >
            {index + 1}. {step}
          </li>
        ))}
      </ol>
      <p role="status">
        {saving
          ? "Salvando…"
          : dirty
            ? "Há alterações ainda não salvas."
            : !status
              ? "Preencha os dados para iniciar."
              : status === "CANCELED"
                ? "Documento cancelado."
                : finished
                  ? "Operação concluída e salva."
                  : "Dados salvos. Continue pela etapa indicada."}
      </p>
    </section>
  );
}

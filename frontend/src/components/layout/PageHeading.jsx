export default function PageHeading({ title, total, singular, plural, loading = false }) {
  const description = loading
    ? "Carregando total…"
    : total == null
      ? "Total indisponível"
      : `${total.toLocaleString("pt-BR")} ${total === 1 ? singular : plural}`;

  return (
    <div className="page-heading">
      <h1>{title}</h1>
      <p className="page-total">{description}</p>
    </div>
  );
}

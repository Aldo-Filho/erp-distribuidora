// Reuses dialog contents in a page without adding a focus trap or scroll lock.
export default function EmbeddedContent({
  title,
  children,
  className = "",
  busy,
  hideTitle = false,
}) {
  return (
    <section className={`embedded-content ${className}`} aria-busy={busy || undefined}>
      {title && !hideTitle && <h2>{title}</h2>}
      {children}
    </section>
  );
}

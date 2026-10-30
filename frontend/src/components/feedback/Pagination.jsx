export default function Pagination({ page, totalPages, change }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button type="button" className="secondary" disabled={page === 0} onClick={() => change(page - 1)}>
        Anterior
      </button>
      <span>
        {page + 1} de {totalPages}
      </span>
      <button
        type="button"
        className="secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => change(page + 1)}
      >
        Próxima
      </button>
    </div>
  );
}

export default function ErrorAlert({ message, retry }) {
  if (!message) return null;
  return (
    <div className="error-alert" role="alert">
      <span>{message}</span>
      {retry && (
        <button type="button" className="text-button" onClick={retry}>
          Tentar novamente
        </button>
      )}
    </div>
  );
}

import { Eye, EyeOff } from "lucide-react";
import useDocumentVisibility from "./useDocumentVisibility";

export default function DocumentValue({ value, scope, label }) {
  const documents = useDocumentVisibility([scope, value]);
  const action = documents.visible ? "Ocultar" : "Mostrar";
  return (
    <span className="document-value">
      <span aria-label={documents.visible ? undefined : "Documento oculto"}>
        {documents.display(value)}
      </span>
      {documents.canToggle && value && (
        <button
          type="button"
          className="document-visibility-toggle"
          aria-label={`${action} ${label}`}
          aria-pressed={documents.visible}
          title={`${action} documento`}
          onClick={documents.toggle}
        >
          {documents.visible ? <EyeOff size={15} /> : <Eye size={15} />}
        </button>
      )}
    </span>
  );
}

import { useState } from "react";
import useAppearance from "./useAppearance";
import { themes } from "./appearanceData";
import FilterSelect from "../components/feedback/FilterSelect";
import ErrorAlert from "../components/feedback/ErrorAlert";

export default function ThemeSettings() {
  const { theme, saveTheme, busy } = useAppearance();
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  return (
    <section className="panel narrow">
      <h2>Tema da minha conta</h2>
      <p className="field-hint">
        Sua escolha acompanha sua conta. Conforme o sistema segue automaticamente a preferência
        deste dispositivo.
      </p>
      <label>
        Tema
        <FilterSelect
          value={theme || "system"}
          disabled={busy}
          onChange={async (event) => {
            setError("");
            setNotice("");
            try {
              await saveTheme(event.target.value === "system" ? null : event.target.value);
              setNotice("Tema salvo na sua conta.");
            } catch (issue) {
              setError(issue.message);
            }
          }}
        >
          {themes.map((entry) => (
            <option key={entry.value} value={entry.value}>
              {entry.name}
            </option>
          ))}
        </FilterSelect>
      </label>
      <ErrorAlert message={error} />
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
    </section>
  );
}

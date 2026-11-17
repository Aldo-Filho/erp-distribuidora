import { useEffect, useRef, useState } from "react";
import { ImagePlus } from "lucide-react";
import useAppearance from "../../appearance/useAppearance";
import { palettes } from "../../appearance/appearanceData";
import useAuth from "../../auth/useAuth";
import useConfirm from "../../components/feedback/useConfirm";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import useDirtyUnload from "../../hooks/useDirtyUnload";

export default function AppearancePage() {
  const { has } = useAuth();
  const value = useAppearance();
  const [notice, setNotice] = useState("");
  if (!has("company.appearance.manage")) return <p>Sem permissão para configurar a aparência.</p>;
  return (
    <>
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <AppearanceForm value={value} onSaved={setNotice} />
    </>
  );
}

function AppearanceForm({ value, onSaved }) {
  const { settings, busy } = value;
  const confirm = useConfirm();
  const [palette, setPalette] = useState(settings.palette);
  const [sourcePalette, setSourcePalette] = useState(settings.palette);
  if (sourcePalette !== settings.palette) {
    setSourcePalette(settings.palette);
    if (palette === sourcePalette) setPalette(settings.palette);
  }
  const fileInput = useRef(null);
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState("");
  useDirtyUnload(busy || Boolean(file) || palette !== settings.palette);
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview);
    },
    [preview],
  );
  const save = async (work, message, onSuccess) => {
    setError("");
    onSaved("");
    try {
      await work();
      onSuccess?.();
      onSaved(message);
    } catch (issue) {
      setError(issue.message);
    }
  };
  return (
    <div className="appearance-page">
      <header className="page-title">
        <div>
          <h1>Aparência da empresa</h1>
          <p className="field-hint">
            Logo e paleta se aplicam a todos. Cada pessoa escolhe seu tema em Minha conta.
          </p>
        </div>
        <button className="secondary" disabled={busy} onClick={value.reload}>
          Recarregar aparência
        </button>
      </header>
      <ErrorAlert message={error} />
      <section className="panel">
        <h2>Paleta global</h2>
        <p className="field-hint">
          Paleta atual: {palettes.find((entry) => entry.id === settings.palette)?.name}. A seleção
          só é aplicada ao salvar.
        </p>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            save(() => value.savePalette(palette), "Paleta salva para toda a empresa.");
          }}
        >
          <fieldset disabled={busy} className="appearance-palettes">
            <legend>Escolha uma paleta</legend>
            {palettes.map((entry) => (
              <label key={entry.id} className="appearance-palette" data-palette={entry.id}>
                <input
                  type="radio"
                  name="palette"
                  value={entry.id}
                  checked={palette === entry.id}
                  onChange={() => setPalette(entry.id)}
                />
                <span className="palette-swatch" aria-hidden="true" />
                <span>
                  <strong>{entry.name}</strong>
                  <small>{entry.description}</small>
                </span>
              </label>
            ))}
          </fieldset>
          <div className="actions">
            <button disabled={busy || palette === settings.palette}>
              {busy ? "Salvando…" : "Salvar paleta global"}
            </button>
          </div>
        </form>
      </section>
      <section className="panel">
        <h2>Logo</h2>
        <p className="field-hint">
          PNG ou JPEG, até 2 MB e 4096 pixels por lado. A imagem é validada e processada pelo
          servidor.
        </p>
        <div className="appearance-logo-preview">
          {file && preview ? (
            <img src={preview} alt="Prévia do novo logo" />
          ) : settings.logoUrl ? (
            <img src={`/api${settings.logoUrl}`} alt="Logo atual" />
          ) : (
            <span>
              <ImagePlus size={32} /> Sem logo personalizado
            </span>
          )}
        </div>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            if (file)
              save(
                () => value.uploadLogo(file),
                "Logo atualizado.",
                () => {
                  setFile(null);
                  setPreview(null);
                  if (fileInput.current) fileInput.current.value = "";
                },
              );
          }}
        >
          <label>
            Selecionar novo logo
            <input
              ref={fileInput}
              type="file"
              accept="image/png,image/jpeg"
              disabled={busy}
              onChange={(event) => {
                const selected = event.target.files?.[0];
                setError("");
                if (!selected) {
                  setFile(null);
                  setPreview(null);
                  return;
                }
                if (
                  !["image/png", "image/jpeg"].includes(selected.type) ||
                  selected.size > 2 * 1024 * 1024 ||
                  selected.size === 0
                ) {
                  setError("Selecione um PNG ou JPEG de até 2 MB.");
                  event.target.value = "";
                  setFile(null);
                  setPreview(null);
                  return;
                }
                setFile(selected);
                setPreview(URL.createObjectURL(selected));
              }}
            />
          </label>
          <div className="actions">
            {settings.logoUrl && (
              <button
                type="button"
                className="secondary"
                disabled={busy}
                onClick={async () => {
                  if (
                    await confirm("Remover o logo personalizado e usar a marca padrão?", {
                      title: "Remover logo",
                      confirmLabel: "Remover",
                    })
                  )
                    save(value.removeLogo, "Logo removido.");
                }}
              >
                Usar marca padrão
              </button>
            )}
            <button disabled={busy || !file}>{busy ? "Salvando…" : "Salvar logo"}</button>
          </div>
        </form>
      </section>
    </div>
  );
}

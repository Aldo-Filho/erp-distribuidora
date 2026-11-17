import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { appearance as service } from "../services/appearance";
import ErrorAlert from "../components/feedback/ErrorAlert";
import { AppearanceContext } from "./AppearanceContext";
import { cached, cache, defaultAppearance, validPalette, validTheme } from "./appearanceData";

export default function AppearanceProvider({ userId, children }) {
  const [settings, setSettings] = useState(() => ({
    ...defaultAppearance,
    palette: cached("jamshub:palette", "jade", validPalette),
  }));
  const [theme, setTheme] = useState(() =>
    userId ? cached(`jamshub:theme:${userId}`, null, validTheme) : null,
  );
  const [ready, setReady] = useState(!userId);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const request = useRef(null);
  const generation = useRef(0);
  const lock = useRef(false);
  const live = useRef(false);
  const channel = useRef(null);
  const applySettings = useCallback((value) => {
    if (
      !validPalette(value?.palette) ||
      !Number.isSafeInteger(value.version) ||
      value.version < 0 ||
      (value.logoUrl != null && !/^\/company\/appearance\/logo\?v=\d+$/.test(value.logoUrl))
    )
      throw new Error("A configuração de aparência recebida é inválida.");
    setSettings(value);
    cache("jamshub:palette", value.palette);
  }, []);

  const reload = useCallback(async () => {
    if (!userId || lock.current) return;
    const version = ++generation.current;
    request.current?.abort();
    const controller = new AbortController();
    request.current = controller;
    try {
      const [value, preference] = await Promise.all([
        service.find({ signal: controller.signal }),
        service.preference({ signal: controller.signal }),
      ]);
      if (!live.current || version !== generation.current) return;
      if (!validTheme(preference?.theme))
        throw new Error("A preferência de tema recebida é inválida.");
      applySettings(value);
      setTheme(preference.theme);
      cache(`jamshub:theme:${userId}`, preference.theme);
      setError("");
    } catch (issue) {
      if (live.current && version === generation.current && issue.name !== "AbortError")
        setError("Não foi possível atualizar a aparência. " + issue.message);
    } finally {
      if (live.current && version === generation.current) setReady(true);
    }
  }, [userId, applySettings]);

  useLayoutEffect(() => {
    document.documentElement.dataset.palette = settings.palette;
    document.documentElement.dataset.theme =
      theme === "DARK" ? "dark" : "light";
  }, [settings.palette, theme]);

  useEffect(() => {
    live.current = true;
    reload();
    window.addEventListener("focus", reload);
    if (userId && "BroadcastChannel" in window) {
      channel.current = new BroadcastChannel("jamshub-appearance");
      channel.current.onmessage = () => reload();
    }
    return () => {
      live.current = false;
      ++generation.current;
      request.current?.abort();
      window.removeEventListener("focus", reload);
      channel.current?.close();
    };
  }, [reload, userId]);

  const write = async (work, preference = false) => {
    if (!userId || lock.current) throw new Error("Aguarde a gravação em andamento.");
    lock.current = true;
    ++generation.current;
    request.current?.abort();
    setBusy(true);
    setError("");
    try {
      const value = await work();
      if (!live.current) return value;
      if (preference) {
        if (!validTheme(value?.theme)) throw new Error("Preferência de tema inválida.");
        setTheme(value.theme);
        cache(`jamshub:theme:${userId}`, value.theme);
      } else applySettings(value);
      channel.current?.postMessage("changed");
      return value;
    } catch (issue) {
      if (live.current)
        setError(
          issue.message + " Recarregue a aparência para conferir o resultado antes de repetir.",
        );
      throw issue;
    } finally {
      lock.current = false;
      if (live.current) setBusy(false);
    }
  };

  return (
    <AppearanceContext.Provider
      value={{
        settings,
        theme,
        busy,
        error,
        reload,
        savePalette: (palette) => write(() => service.palette(palette, settings.version)),
        uploadLogo: (file) => write(() => service.logo(file, settings.version)),
        removeLogo: () => write(() => service.removeLogo(settings.version)),
        saveTheme: (next) => write(() => service.theme(next), true),
      }}
    >
      {error && <ErrorAlert message={error} retry={reload} />}
      {ready ? (
        children
      ) : (
        <div className="screen-center" role="status">
          Carregando aparência…
        </div>
      )}
    </AppearanceContext.Provider>
  );
}

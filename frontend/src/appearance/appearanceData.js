export const palettes = [
  { id: "jade", name: "Jade", description: "Verde e azul, identidade original" },
  { id: "ocean", name: "Oceano", description: "Azul" },
  { id: "violet", name: "Violeta", description: "Roxo" },
  { id: "terracotta", name: "Terracota", description: "Tons de terra" },
  { id: "amber", name: "Âmbar", description: "Dourado" },
  { id: "graphite", name: "Grafite", description: "Neutro" },
];
export const defaultAppearance = { palette: "jade", logoUrl: null, version: 0 };
export const themes = [
  { value: "LIGHT", name: "Claro" },
  { value: "DARK", name: "Escuro" },
];

export function validPalette(value) {
  return palettes.some((palette) => palette.id === value);
}
export function validTheme(value) {
  return value === null || value === "LIGHT" || value === "DARK";
}
export function cached(key, fallback, validate) {
  try {
    const value = JSON.parse(localStorage.getItem(key));
    return validate(value) ? value : fallback;
  } catch {
    return fallback;
  }
}
export function cache(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* The API remains the source of truth. */
  }
}
export function initializeAppearance() {
  document.documentElement.dataset.palette = cached("jamshub:palette", "jade", validPalette);
  document.documentElement.dataset.theme = "light";
}

import { useContext } from "react";
import { AppearanceContext } from "./AppearanceContext";
export default function useAppearance() {
  return useContext(AppearanceContext);
}

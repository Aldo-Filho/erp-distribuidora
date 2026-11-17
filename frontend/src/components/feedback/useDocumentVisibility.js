import { useState } from "react";
import useAuth from "../../auth/useAuth";

export default function useDocumentVisibility(scope) {
  const { session, has } = useAuth();
  const administrator = [
    "access.users.manage",
    "access.roles.manage",
    "security.settings.manage",
  ].every(has);
  const key = JSON.stringify([session.userId, session.roleId, scope]);
  const [revelation, setRevelation] = useState({ key, visible: false });
  if (revelation.key !== key) setRevelation({ key, visible: false });
  const visible = administrator || (revelation.key === key && revelation.visible);

  return {
    visible,
    canToggle: !administrator,
    toggle: () => setRevelation((current) => ({ key, visible: !current.visible })),
    display: (value) => {
      if (value == null || String(value).trim() === "") return "—";
      if (visible) return value;
      return String(value).replace(/\D/g, "").length > 11 ? "••.•••.•••/••••-••" : "•••.•••.•••-••";
    },
  };
}

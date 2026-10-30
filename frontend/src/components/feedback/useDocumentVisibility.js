import { useEffect, useState } from "react";
import useAuth from "../../auth/useAuth";

export default function useDocumentVisibility(scope) {
  const { session, has } = useAuth();
  const administrator = [
    "access.users.manage",
    "access.roles.manage",
    "security.settings.manage",
  ].every(has);
  const key = JSON.stringify([session.userId, session.roleId, scope]);
  const [revealedFor, setRevealedFor] = useState(null);
  useEffect(() => setRevealedFor(null), [key]);
  const visible = administrator || revealedFor === key;

  return {
    visible,
    canToggle: !administrator,
    toggle: () => setRevealedFor((current) => (current === key ? null : key)),
    display: (value) => {
      if (value == null || String(value).trim() === "") return "—";
      if (visible) return value;
      return String(value).replace(/\D/g, "").length > 11 ? "••.•••.•••/••••-••" : "•••.•••.•••-••";
    },
  };
}

import { useEffect } from "react";
import { auth } from "../services/auth";

export default function SessionActivity({ refresh }) {
  useEffect(() => {
    let lastInteraction = 0;
    let lastSent = 0;
    let pending = false;
    const interact = () => {
      lastInteraction = Date.now();
    };
    const send = async () => {
      const now = Date.now();
      if (
        pending ||
        document.visibilityState !== "visible" ||
        now - lastInteraction > 60000 ||
        now - lastSent < 60000
      )
        return;
      pending = true;
      lastSent = now;
      try {
        await auth.activity();
        await refresh();
      } catch {
        // The API handles expiration. Failed writes are never replayed automatically.
      } finally {
        pending = false;
      }
    };
    const events = ["pointerdown", "keydown", "scroll"];
    events.forEach((name) => window.addEventListener(name, interact, { passive: true }));
    const interval = setInterval(send, 10000);
    return () => {
      clearInterval(interval);
      events.forEach((name) => window.removeEventListener(name, interact));
    };
  }, [refresh]);
  return null;
}

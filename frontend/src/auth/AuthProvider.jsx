import { createContext, useCallback, useEffect, useRef, useState } from "react";
import { auth } from "../services/auth";
import { clearCsrf } from "../services/api";
import { clearDrafts, setDraftOwner } from "./drafts";
import SessionActivity from "./SessionActivity";
import SessionExpiryDialog from "../components/feedback/SessionExpiryDialog";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [expired, setExpired] = useState(false);
  const channel = useRef(null);
  const previouslyAuthenticated = useRef(false);
  const requestVersion = useRef(0);

  const clear = useCallback((expiration = false) => {
    requestVersion.current += 1;
    setSession(null);
    setExpired(expiration);
    previouslyAuthenticated.current = false;
    setError("");
    setLoading(false);
  }, []);

  const refresh = useCallback(async () => {
    const version = ++requestVersion.current;
    try {
      const value = await auth.session();
      if (version !== requestVersion.current) return null;
      setDraftOwner(value.userId);
      setSession(value);
      previouslyAuthenticated.current = true;
      setError("");
      return value;
    } catch (failure) {
      if (version !== requestVersion.current) return null;
      if (failure.status === 401) clear(previouslyAuthenticated.current);
      else setError(failure.message);
      return null;
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }, [clear]);

  useEffect(() => {
    refresh();
    const onExpired = () => clear(true);
    window.addEventListener("erp:session-expired", onExpired);
    const interval = setInterval(refresh, 60000);
    const onFocus = () => refresh();
    window.addEventListener("focus", onFocus);
    if ("BroadcastChannel" in window) {
      channel.current = new BroadcastChannel("erp-session");
      channel.current.onmessage = (event) => {
        clearCsrf();
        if (event.data === "logout") clearDrafts();
        refresh();
      };
    }
    return () => {
      clearInterval(interval);
      window.removeEventListener("focus", onFocus);
      window.removeEventListener("erp:session-expired", onExpired);
      channel.current?.close();
    };
  }, [refresh, clear]);

  const signedIn = (value) => {
    requestVersion.current += 1;
    previouslyAuthenticated.current = true;
    setDraftOwner(value.userId);
    setSession(value);
    setExpired(false);
    setError("");
    setLoading(false);
    channel.current?.postMessage("changed");
  };
  const logout = async () => {
    await auth.logout();
    clearDrafts();
    clear(false);
    channel.current?.postMessage("logout");
  };
  const signedOut = () => {
    clearCsrf();
    clearDrafts();
    clear(false);
    channel.current?.postMessage("logout");
  };
  const has = (key) => session?.permissionKeys.includes(key) || false;

  return (
    <AuthContext.Provider
      value={{ session, loading, error, expired, has, refresh, signedIn, signedOut, logout }}
    >
      {children}
      {session && <SessionActivity refresh={refresh} />}
      {session && <SessionExpiryDialog session={session} refresh={refresh} logout={logout} />}
    </AuthContext.Provider>
  );
}

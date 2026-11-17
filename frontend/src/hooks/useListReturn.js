import { useLayoutEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import useAuth from "../auth/useAuth";
import { readDraft, saveDraft } from "../auth/drafts";

export default function useListReturn({ modulePath, listState, ready = false }) {
  const { session } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const key = `list-return:${session.userId}:${modulePath}`;
  const cached = readDraft(key);
  const [restored] = useState(() =>
    location.state?.listModule === modulePath || cached?.listKey === location.key
      ? cached
      : undefined,
  );
  const didRestore = useRef(false);
  useLayoutEffect(() => {
    if (!ready || didRestore.current || !restored) return;
    const frame = requestAnimationFrame(() => {
      didRestore.current = true;
      window.scrollTo(0, restored.scroll || 0);
    });
    return () => cancelAnimationFrame(frame);
  }, [ready, restored]);
  const entryState = () => {
    if (listState) saveDraft(key, { ...listState, scroll: window.scrollY, listKey: location.key });
    return { listModule: modulePath };
  };
  return {
    restored,
    entryState,
    open: (path) => navigate(path, { state: entryState() }),
    returnToList: () =>
      navigate(modulePath, {
        state:
          location.state?.listModule === modulePath
            ? {
                listModule: modulePath,
                highlightId: /^\/(?:purchases|orders)\/(\d+)/.exec(location.pathname)?.[1],
              }
            : null,
      }),
    navigationState: location.state,
  };
}

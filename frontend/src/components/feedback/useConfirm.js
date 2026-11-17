import { useCallback, useContext, useEffect, useRef } from "react";
import { ConfirmationContext } from "./ConfirmationContext.js";

export default function useConfirm() {
  const { request, cancel } = useContext(ConfirmationContext);
  const owner = useRef(Symbol());
  useEffect(() => {
    const identity = owner.current;
    return () => cancel(identity);
  }, [cancel]);
  return useCallback((message, options) => request(message, options, owner.current), [request]);
}

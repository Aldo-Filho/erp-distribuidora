import { useState } from "react";

const drafts = new Map();
let owner;

export function setDraftOwner(userId) {
  if (owner !== undefined && owner !== userId) drafts.clear();
  owner = userId;
}

export function clearDrafts() {
  drafts.clear();
  owner = null;
}

export function removeDraft(key) {
  drafts.delete(key);
}

export function readDraft(key) {
  return drafts.get(key);
}

export function saveDraft(key, value) {
  drafts.set(key, value);
}

// Only non-secret form fields. State survives a session redirect,
// stays in this tab's memory and is discarded on reload, logout or account switch.
export function useDraft(key, initial) {
  const [value, setValue] = useState(() => (drafts.has(key) ? drafts.get(key) : initial));
  const update = (next) => {
    setValue((previous) => {
      const result = typeof next === "function" ? next(previous) : next;
      drafts.set(key, result);
      return result;
    });
  };
  return [value, update];
}

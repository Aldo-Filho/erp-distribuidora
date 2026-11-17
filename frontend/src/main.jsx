import { initializeAppearance } from "./appearance/appearanceData";
import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import ConfirmationProvider from "./components/feedback/ConfirmationProvider";
import "./index.css";

initializeAppearance();

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <ConfirmationProvider>
        <App />
      </ConfirmationProvider>
    </BrowserRouter>
  </React.StrictMode>,
);

import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    // @testing-library/react needs globals: true to register automatic DOM cleanup between tests
    globals: true,
  },
});

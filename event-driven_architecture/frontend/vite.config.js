import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 8085,
    proxy: {
      '/api': {
        target: process.env.VITE_GATEWAY_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 8085,
  },
});

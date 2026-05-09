/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        cgv: {
          red: '#e50914',
          'red-dark': '#b20710',
          black: '#0a0a0a',
          panel: '#141414',
          muted: '#c4c4c4',
          gold: '#c9a227',
        },
      },
      fontFamily: {
        display: ['"Bebas Neue"', 'system-ui', 'sans-serif'],
        sans: ['"Source Sans 3"', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        screen: '0 0 40px rgba(229, 9, 20, 0.15)',
      },
    },
  },
  plugins: [],
};

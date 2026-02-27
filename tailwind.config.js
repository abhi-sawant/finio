/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./app/**/*.{tsx,ts}', './components/**/*.{tsx,ts}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        background: '#0f1117',
        surface: '#1a1d27',
        surfaceElevated: '#242736',
        primary: '#6C63FF',
        income: '#22c55e',
        expense: '#ef4444',
        transfer: '#3b82f6',
        'text-primary': '#f1f5f9',
        'text-muted': '#64748b',
        border: 'rgba(255,255,255,0.08)',
      },
      fontFamily: {
        sans: ['DMSans_400Regular'],
        'sans-medium': ['DMSans_500Medium'],
        'sans-bold': ['DMSans_700Bold'],
        display: ['Sora_700Bold'],
        'display-bold': ['Sora_800ExtraBold'],
      },
    },
  },
  plugins: [],
}


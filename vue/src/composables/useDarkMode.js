import {ref, watch} from 'vue'

const MODE_KEY = 'ldrbot-dark-mode-enabled'
const darkMode = ref(true)

// Init once per app session
const stored = localStorage.getItem(MODE_KEY)
if (stored !== null) {
    darkMode.value = stored === 'true'
} else {
    darkMode.value = window.matchMedia('(prefers-color-scheme: dark)').matches
}

watch(darkMode, (value) => {
    document.documentElement.classList.toggle('dark-mode', value)
    localStorage.setItem(MODE_KEY, value)
}, {immediate: true})

function toggleDarkMode() {
    darkMode.value = !darkMode.value
}

export function useDarkMode() {
    return {
        darkMode,
        toggleDarkMode
    }
}
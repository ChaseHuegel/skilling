import { createApp } from 'vue';
import { createPinia } from 'pinia';
import PrimeVue from 'primevue/config';
import Aura from '@primevue/themes/aura';
import router from './router';
import App from './App.vue';

// Apply dark mode class before PrimeVue initializes so its theme plugin
// reads the correct darkModeSelector state at setup time.
const prefersDark = localStorage.getItem('skilling_dark_mode') !== 'false';
if (prefersDark) {
    document.documentElement.classList.add('app-dark');
}

const app = createApp(App);
app.use(createPinia());
app.use(PrimeVue, {
    theme: {
        preset: Aura,
        options: {
            darkModeSelector: '.app-dark',
        },
    },
});
app.use(router);
app.mount('#app');

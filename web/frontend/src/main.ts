import { createApp } from 'vue';
import { createPinia } from 'pinia';
import PrimeVue from 'primevue/config';
import Aura from '@primevue/themes/aura';
import router from './router';
import App from './App.vue';
import { useAuthStore } from './stores/auth';

// Apply dark mode class before PrimeVue initializes so its theme plugin
// reads the correct darkModeSelector state at setup time.
const prefersDark = localStorage.getItem('skilling_dark_mode') !== 'false';
if (prefersDark) {
    document.documentElement.classList.add('app-dark');
}

const app = createApp(App);
app.use(createPinia());
// Restore the session before the router resolves its initial navigation so the
// route guard never races `checkSession()` (App.vue setup runs after the first
// route resolves). A saved storage state must be honored on a direct navigation
// to a guarded route instead of bouncing to the login page.
useAuthStore().checkSession();
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

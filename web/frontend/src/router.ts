import { createRouter, createWebHashHistory } from 'vue-router';
import { useAuthStore } from './stores/auth';

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: () => import('./views/LoginPage.vue'),
        },
        {
            path: '/',
            name: 'Dashboard',
            component: () => import('./views/DashboardPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/skills/new',
            name: 'SkillNew',
            component: () => import('./views/SkillEditorPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/skills/:id',
            name: 'SkillEdit',
            component: () => import('./views/SkillEditorPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/abilities',
            name: 'Abilities',
            component: () => import('./views/AbilitiesPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/tags',
            name: 'Tags',
            component: () => import('./views/TagsPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/layout',
            name: 'GuiLayout',
            component: () => import('./views/GuiLayoutPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/config',
            name: 'Config',
            component: () => import('./views/ConfigPage.vue'),
            meta: { requiresAuth: true },
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'NotFound',
            component: () => import('./views/NotFoundPage.vue'),
        },
    ],
});

router.beforeEach((to, _from) => {
    const auth = useAuthStore();
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
        return '/login';
    }
});

export default router;

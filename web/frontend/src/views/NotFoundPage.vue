<template>
    <div class="not-found">
        <h1>404</h1>
        <p>Page not found</p>
        <router-link to="/" class="btn btn-primary">Back to Skills</router-link>
    </div>
</template>

<style scoped>
.not-found {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 1.5rem;
    text-align: center;
    gap: 0.5rem;
}
.not-found h1 {
    font-size: 3rem;
    margin: 0;
    color: var(--p-text-muted-color);
}
.not-found p {
    color: var(--p-text-muted-color);
    margin: 0 0 1.25rem;
}
</style>

package io.github.chasehuegel.skilling.engine.registry;

/**
 * Internal helper shared by the registries for fail-fast class validation.
 */
final class RegistrySupport {

    private RegistrySupport() {}

    /**
     * Fails fast if the class has no public no-arg constructor, so a bad
     * registration is rejected at registration time instead of at first use.
     *
     * @param key   the registry key (used in the error message)
     * @param clazz the registered class
     * @throws IllegalArgumentException if the class lacks a public no-arg constructor
     */
    static void requirePublicNoArgConstructor(String key, Class<?> clazz) {
        try {
            clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Class for '" + key + "' must have a public no-arg constructor: " + clazz.getName(), e);
        }
    }
}

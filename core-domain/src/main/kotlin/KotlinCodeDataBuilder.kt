package ru.it_arch.kddd.domain

public interface KotlinCodeDataBuilder {
    public fun generateImplementationClass()
    public fun generateConstructor()
    public fun generateProperties()
    /**
     * ```
     * @Suppress("UNCHECKED_CAST")
     * override fun <T : Kddd, A : Kddd> fork(vararg args: A): T { <body> }
     * ```
     * */
    public fun generateFork()
}

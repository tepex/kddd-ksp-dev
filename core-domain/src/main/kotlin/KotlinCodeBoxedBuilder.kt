package ru.it_arch.clean_ddd.domain

public interface KotlinCodeBoxedBuilder {
    public fun generateImplementationClass()
    public fun generateProperty()
    public fun generateConstructor()
    public fun generateToString()
    public fun generateFork()
    public fun generateCompanion()
}

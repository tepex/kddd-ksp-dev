package ru.it_arch.kddd.domain

public interface KotlinCodeBoxedBuilder {
    public fun generateImplementationClass()
    public fun generateProperty()
    public fun generateConstructor()
    public fun generateToString()
    public fun generateFork()
    public fun generateCompanion()
}

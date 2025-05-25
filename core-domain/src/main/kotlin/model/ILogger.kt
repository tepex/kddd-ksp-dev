package ru.it_arch.kddd.domain.model

public interface ILogger {
    public fun log(text: String)
    public fun err(text: String)
}

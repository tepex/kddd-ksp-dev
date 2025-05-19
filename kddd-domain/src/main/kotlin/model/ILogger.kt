package ru.it_arch.clean_ddd.domain.model

public interface ILogger {
    public fun log(text: String)
    public fun err(text: String)
}

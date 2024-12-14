package ru.it_arch.clean_ddd.ksp.interop

internal interface KDLogger {
    fun log(text: String)
    fun err(text: String)
}

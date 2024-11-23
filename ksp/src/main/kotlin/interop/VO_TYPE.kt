package ru.it_arch.clean_ddd.ksp.interop

internal enum class VO_TYPE(val className: String) {
    VALUE_OBJECT("ru.it_arch.ddd.ValueObject"),
    VALUE_OBJECT_SINGLE("ru.it_arch.ddd.ValueObjectSingle")
}

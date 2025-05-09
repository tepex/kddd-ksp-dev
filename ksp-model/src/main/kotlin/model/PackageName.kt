package ru.it_arch.clean_ddd.ksp_model.model

import ru.it_arch.kddd.ValueObject

/** Тип для имени пакета. */
@JvmInline
public value class PackageName private constructor(override val boxed: String) : ValueObject.Boxed<String> {
    init {
        validate()
    }

    override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
        TODO("Not yet implemented")

    override fun validate() {}

    override fun toString(): String = boxed

    public operator fun plus(subpackage: KDOptions.Subpackage): PackageName =
        takeIf { subpackage.isEmpty() } ?: PackageName("$boxed.${subpackage.boxed}")

    public companion object {
        public operator fun invoke(value: String): PackageName = PackageName(value)
    }
}

package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.k3dm.ValueObject

public data class KDTypeContext(
    val options: KDOptions,
    val logger : KDLogger,
    val globalKDTypes: Map<TypeName, KDType>,
    val toBeGenerated: ClassName,
    val typeName: TypeName,
    val packageName: PackageName,
    val properties: List<KDProperty>
) {

    @JvmInline
    public value class PackageName private constructor(override val boxed: String) : ValueObject.Value<String> {
        init {
            validate()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Value<String>> apply(boxed: String): T =
            PackageName(boxed) as T

        override fun validate() {}

        override fun toString(): String = boxed

        public companion object {
            public operator fun invoke(value: String): PackageName = PackageName(value)
        }
    }
}

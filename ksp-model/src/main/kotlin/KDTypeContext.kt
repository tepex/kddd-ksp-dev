package ru.it_arch.clean_ddd.ksp.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import ru.it_arch.kddd.KDSerialName
import ru.it_arch.kddd.ValueObject

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
    public value class PackageName private constructor(override val boxed: String) : ValueObject.Boxed<String> {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> fork(boxed: String): T =
            PackageName(boxed) as T

        override fun validate() {}

        override fun toString(): String = boxed

        public companion object {
            public operator fun invoke(value: String): PackageName = PackageName(value)
        }
    }
}

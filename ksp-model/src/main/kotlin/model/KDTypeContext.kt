package ru.it_arch.clean_ddd.ksp.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import ru.it_arch.clean_ddd.ksp.interop.KDOptions
import ru.it_arch.kddd.ValueObject

public data class KDTypeContext private constructor(
    val options: KDOptions,
    val logger : KDLogger,
    val globalKDTypes: Map<TypeName, KDType>,
    val toBeGenerated: ClassName,
    val typeName: TypeName,
    val packageName: PackageName,
    val properties: List<Property>
) {

    @JvmInline
    public value class PackageName private constructor(override val boxed: String) : ValueObject.Boxed<String> {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ValueObject.Boxed<String>> copy(boxed: String): T =
            packageName(boxed) as T

        override fun validate() {}

        override fun toString(): String = boxed

        public companion object {
            public fun packageName(value: String): PackageName = PackageName(value)
        }
    }

    public data class Property(
        val name: MemberName,
        val typeName: TypeName
    )

    public companion object {
        public fun typeContext(
            options: KDOptions,
            logger: KDLogger,
            globalKDTypes: Map<TypeName, KDType>,
            toBeGenerated: ClassName,
            typeName: TypeName,
            declaration: KSClassDeclaration
        ): KDTypeContext = KDTypeContext(
            options,
            logger,
            globalKDTypes,
            toBeGenerated,
            typeName,
            PackageName.packageName(declaration.packageName.asString()),
            declaration.getAllProperties()
                .map { Property(toBeGenerated.member(it.simpleName.asString()), it.type.toTypeName()) }.toList()
        )
    }
}

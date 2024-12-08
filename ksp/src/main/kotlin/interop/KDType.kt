package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

@ConsistentCopyVisibility
internal data class KDType private constructor(
    val className: ClassName,
    val builder: TypeSpec.Builder,
    val parameters: List<KDParameter>,
    val valueObjectType: KDValueObjectType
) {

    val builderClassName = ClassName.bestGuess("${className.simpleName}.$BUILDER_CLASS_NAME")

    private val nestedTypes = mutableMapOf<TypeName, KDType>()

    fun addNestedType(key: TypeName, type: KDType) {
        nestedTypes[key.toNullable(false)] = type
        builder.addType(type.builder.build())
    }

    fun getNestedType(typeName: TypeName) =
        nestedTypes[typeName.toNullable(false)] ?: error("Can't find implementation for $typeName in $className")

    companion object {
        const val BUILDER_CLASS_NAME = "Builder"
        const val BUILDER_BUILD_METHOD_NAME = "build"

        fun create(
            className: ClassName,
            builder: TypeSpec.Builder,
            parameters: List<KDParameter>,
            valueObjectType: KDValueObjectType
        ) = KDType(className, builder, parameters, valueObjectType)
    }
}

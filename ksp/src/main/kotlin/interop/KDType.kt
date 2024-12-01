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

    val innerTypes = mutableMapOf<TypeName, KDType>()

    fun addInnerType(key: TypeName, type: KDType) {
        innerTypes[key.toNullable(false)] = type
        builder.addType(type.builder.build())
    }

    fun getInnerType(typeName: TypeName): KDType =
        innerTypes[typeName.toNullable(false)] ?: error("Can't find implementation for $typeName in $className")

    fun getBoxedTypeOrNull(typeName: TypeName): TypeName? =
        innerTypes[typeName.toNullable(false)]
            ?.let { if (it.valueObjectType is KDValueObjectType.KDValueObjectSingle) it.valueObjectType.boxedType else null }

    companion object {
        fun create(
            className: ClassName,
            builder: TypeSpec.Builder,
            parameters: List<KDParameter>,
            valueObjectType: KDValueObjectType
        ) = KDType(className, builder, parameters, valueObjectType)
    }
}

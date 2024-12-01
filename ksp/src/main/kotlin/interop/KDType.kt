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

    private val _innerTypes = mutableMapOf<TypeName, KDType>()
    val innerTypes: Map<TypeName, KDType>
        get() = _innerTypes.toMap()

    fun addInnerType(key: TypeName, type: KDType) {
        _innerTypes[key] = type
        builder.addType(type.builder.build())
    }

    fun getBoxedTypeOrNull(): TypeName? =
        if (valueObjectType is KDValueObjectType.KDValueObjectSingle) valueObjectType.boxedType else null

    companion object {
        fun create(
            className: ClassName,
            builder: TypeSpec.Builder,
            parameters: List<KDParameter>,
            valueObjectType: KDValueObjectType
        ) = KDType(className, builder, parameters, valueObjectType)
    }
}

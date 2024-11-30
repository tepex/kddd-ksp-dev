package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec

internal data class TypeSpecWrapper(
    val implName: ClassName,
    val builder: TypeSpec.Builder,
    val parameters: Set<KDParameter>,
    val valueObjectType: KDValueObjectType
)

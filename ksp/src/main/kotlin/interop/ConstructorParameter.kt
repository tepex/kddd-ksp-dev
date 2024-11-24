package ru.it_arch.clean_ddd.ksp.interop

import com.squareup.kotlinpoet.TypeName

internal data class ConstructorParameter(
    val name: String,
    val type: TypeName,
    val builderType: TypeName?,
    val isNullable: Boolean = false
)
